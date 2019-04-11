using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Linq.Expressions;
using System.Net;
using System.Net.Http;
using System.Reflection;
using System.Web.Http;
using System.Web.Http.Controllers;
using System.Web.OData;
using System.Web.OData.Query;
using Core.ApplicationServices;
using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.Interfaces;
using dk.nita.saml20.identity;
using Ninject;
using OS2Indberetning.Filters;
using Expression = System.Linq.Expressions.Expression;

namespace OS2Indberetning.Controllers
{
    [AuditlogFilter]
    public class BaseController<T> : ODataController where T : class
    {
        protected ODataValidationSettings ValidationSettings = new ODataValidationSettings();
        protected IGenericRepository<T> Repo;
        private readonly IGenericRepository<Person> _personRepo;
        private readonly PropertyInfo _primaryKeyProp;

        protected readonly ILogger _logger;
        protected readonly ICustomSettings _customSettings;

        protected Person CurrentUser;

        protected override void Initialize(HttpControllerContext controllerContext)
        {
            base.Initialize(controllerContext);

#if DEBUG
            var userCookie = controllerContext.Request.Headers.GetCookies()?.FirstOrDefault()?["debug_user"];
            var userInitials = userCookie?.Value?.ToLower();

            if (userInitials != null)
            {
                CurrentUser = _personRepo.AsQueryable().FirstOrDefault(p => p.Initials.ToLower().Equals(userInitials) && p.IsActive);
            }
#else
            if (ConfigurationManager.AppSettings["AUTHENTICATION"].Equals("SAML"))
            {
                LoginUserSAML();
            }
            else
            {
                LoginUserWindowsIntegratedAuthentication();
            }
#endif

            _logger.Debug($"{GetType()}, Initialize(), User logged in: {CurrentUser.FullName}");
        }

        private void LoginUserSAML()
        {
            if (Saml20Identity.Current != null)
            {
                string cpr;
                try
                {
                    var claim = ConfigurationManager.AppSettings["AUTHENTICATION_ATTRIBUTE"];
                    cpr = Saml20Identity.Current[claim].First().AttributeValue.First();
                }
                catch (Exception e)
                {
                    _logger.Error($"{GetType().Name}, Valid attribute not available on SAML token", e);
                    throw new UnauthorizedAccessException("Valid SAML attribute not available");
                }
                ValidateUser(cpr, cpr: true);
            }
            else
            {
                throw new UnauthorizedAccessException("SAML token not available");
            }
        }

        private void LoginUserWindowsIntegratedAuthentication()
        {
            string[] httpUser = User.Identity.Name.Split('\\');

            if (httpUser.Length == 2 && String.Equals(httpUser[0], _customSettings.AdDomain, StringComparison.CurrentCultureIgnoreCase))
            {
                ValidateUser(httpUser[1]);
            }
            else
            {
                _logger.LogForAdmin("Gyldig domænebruger ikke fundet (" + User.Identity.Name + "). " + User.Identity.Name + " har derfor ikke kunnet logge på.");
                _logger.Debug($"{GetType().Name}, Initialize(), Gyldig domænebruger ikke fundet (" + User.Identity.Name + "). " + User.Identity.Name + " har derfor ikke kunnet logge på.");
                throw new UnauthorizedAccessException("Gyldig domænebruger ikke fundet.");
            }
        }

        private void ValidateUser(string initials, bool cpr = false)
        {
            if (cpr)
            {
                CurrentUser = _personRepo.AsQueryable().FirstOrDefault(p => p.CprNumber.Equals(initials) && p.IsActive);
            }
            else
            {
                CurrentUser = _personRepo.AsQueryable().FirstOrDefault(p => p.Initials.ToLower().Equals(initials) && p.IsActive);
            }

            if (CurrentUser == null)
            {
                if (cpr)
                {   
                    _logger.LogForAdmin("Bruger ikke fundet i databasen. CPR: " + initials);
                    _logger.Debug($"{GetType().Name}, ValidateUser(), Bruger ikke fundet i databasen. CPR: " + initials);
                    throw new UnauthorizedAccessException("Bruger ikke fundet via SAML login.");
                }
                else
                {
                    _logger.LogForAdmin("AD-bruger ikke fundet i databasen (" + User.Identity.Name + "). " + User.Identity.Name + " har derfor ikke kunnet logge på.");
                    _logger.Debug($"{GetType().Name}, Initialize(), AD-bruger ikke fundet i databasen (" + User.Identity.Name + "). " + User.Identity.Name + " har derfor ikke kunnet logge på.");
                    throw new UnauthorizedAccessException("Bruger ikke fundet i databasen.");
                }
            }

            if (!CurrentUser.IsActive)
            {
                _logger.LogForAdmin("Inaktiv bruger forsøgte at logge ind (" + User.Identity.Name + "). " + User.Identity.Name + " har derfor ikke kunnet logge på.");
                _logger.Debug($"{GetType().Name}, Initialize(), Inaktiv bruger forsøgte at logge ind (" + User.Identity.Name + "). " + User.Identity.Name + " har derfor ikke kunnet logge på.");
                throw new UnauthorizedAccessException("Inaktiv bruger forsøgte at logge ind.");
            }
        }

        public BaseController(IGenericRepository<T> repository, IGenericRepository<Person> personRepo)
        {
            _personRepo = personRepo;
            ValidationSettings.AllowedQueryOptions = AllowedQueryOptions.All;
            ValidationSettings.MaxExpansionDepth = 4;
            Repo = repository;
            _primaryKeyProp = Repo.GetPrimaryKeyProperty();

            _logger = NinjectWebKernel.GetKernel(isWebProject: true).Get<ILogger>();
            _customSettings = NinjectWebKernel.GetKernel(isWebProject: true).Get<ICustomSettings>();
        }

        protected IQueryable<T> GetQueryable(ODataQueryOptions<T> queryOptions)
        {
            if (queryOptions.Filter != null) { 
                return (IQueryable<T>)queryOptions.Filter.ApplyTo(Repo.AsQueryable(), new ODataQuerySettings());
            }
            return Repo.AsQueryable();        
        }

        protected IQueryable<T> GetQueryable(int key, ODataQueryOptions<T> queryOptions)
        {
            var result = new List<T> { };
            var entity = Repo.AsQueryable().FirstOrDefault(PrimaryKeyEquals(_primaryKeyProp, key));
            if (entity != null)
            {
                result.Add(entity);
            }
            if (queryOptions.Filter != null)
            {
                return (IQueryable<T>) queryOptions.Filter.ApplyTo(result.AsQueryable(), new ODataQuerySettings());
            }
            return result.AsQueryable();        
        }

        protected IHttpActionResult Put(int key, Delta<T> delta)
        {
            return StatusCode(HttpStatusCode.MethodNotAllowed);
        }

        protected IHttpActionResult Post(T entity)
        {
            Validate(entity);

            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            try
            {
                entity = Repo.Insert(entity);
                Repo.Save();
                return Created(entity);
            }
            catch (Exception e)
            {
                return InternalServerError(e);
            }
        }

        protected IHttpActionResult Patch(int key, Delta<T> delta)
        {
            //Validate(delta.GetEntity());

            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var entity = Repo.AsQueryable().FirstOrDefault(PrimaryKeyEquals(_primaryKeyProp, key));
            if (entity == null)
            {
                return BadRequest("Unable to find entity with id " + key);
            }

            try
            {
                delta.Patch(entity);
                Repo.Save();
            }
            catch (Exception e)
            {
                return InternalServerError(e);
            }

            return Updated(entity);
        }

        protected IHttpActionResult Delete(int key)
        {
            var entity = Repo.AsQueryable().FirstOrDefault(PrimaryKeyEquals(_primaryKeyProp, key));
            if (entity == null)
            {
                return BadRequest("Unable to find entity with id " + key);
            }
            try
            {
                Repo.Delete(entity);
                Repo.Save();
            }
            catch (Exception e)
            {
                return InternalServerError(e);
            }
            return Ok();
        }

        private static Expression<Func<T, bool>> PrimaryKeyEquals(PropertyInfo property, int value)
        {
            var param = Expression.Parameter(typeof(T));
            var body = Expression.Equal(Expression.Property(param, property), Expression.Constant(value));
            return Expression.Lambda<Func<T, bool>>(body, param);
        }
    }
}