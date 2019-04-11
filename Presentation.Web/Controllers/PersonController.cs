﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Web.Http;
using System.Web.Mvc;
using System.Web.OData;
using System.Web.OData.Query;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using Infrastructure.DataAccess;
using Core.ApplicationServices.Logger;

namespace OS2Indberetning.Controllers
{
    public class PersonController : BaseController<Person>
    {
        private IPersonService _person;
        private readonly IGenericRepository<Employment> _employmentRepo;
        private readonly IGenericRepository<LicensePlate> _licensePlateRepo;
        private readonly IGenericRepository<Substitute> _substituteRepo;
        private readonly IGenericRepository<AppLogin> _appLoginRepo;
        private readonly IGenericRepository<OrgUnit> _orgUnitsRepo;
        private readonly ILogger _logger;


        public PersonController(IGenericRepository<Person> repo, IPersonService personService, IGenericRepository<Employment> employmentRepo, IGenericRepository<LicensePlate> licensePlateRepo, IGenericRepository<Substitute> substituteRepo, IGenericRepository<AppLogin> appLoginRepo, IGenericRepository<OrgUnit> orgUnitRepo, ILogger log)
            : base(repo, repo)
        {
            _person = personService;
            _employmentRepo = employmentRepo;
            _licensePlateRepo = licensePlateRepo;
            _substituteRepo = substituteRepo;
            _appLoginRepo = appLoginRepo;
            _orgUnitsRepo = orgUnitRepo;
            _logger = log;
        }

        // GET: odata/Person
        /// <summary>
        /// GET API endpoint for Person
        /// </summary>
        /// <param name="queryOptions"></param>
        /// <returns>People</returns>
        [EnableQuery]
        public IHttpActionResult GetPerson(ODataQueryOptions<Person> queryOptions)
        {
            var res = GetQueryable(queryOptions);
            _person.ScrubCprFromPersons(res);
            var currentTimestamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
            //We used to loop through all persons and remove employments that had expired
            //This proved to be to slow, and we did not have a use for current employments 
            //for a list of users, each time we needed someones employment we made queries
            //to that person anyway, so the return of this function has all employments
            //including the expired ones.
            return Ok(res);
        }


        /// <summary>
        /// GET API endpoint for CurrentUser.
        /// Sets HomeWorkDistance on each of the users employments.
        /// Strips CPR-number off.
        /// </summary>
        /// <returns>The user currently logged in.</returns>
        [EnableQuery(MaxExpansionDepth = 4)]
        // Disable caching.
        [OutputCache(NoStore = true, Duration = 0, VaryByParam = "None")]
        public Person GetCurrentUser()
        {
            try
            {
                var currentDateTimestamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
                var employments = _employmentRepo.AsQueryable().Where(x => x.PersonId == CurrentUser.Id && (x.EndDateTimestamp == 0 || x.EndDateTimestamp > currentDateTimestamp));
                var employmentList = employments.ToList();

                CurrentUser.Employments.Clear();

                foreach (var employment in employmentList)
                {
                    CurrentUser.Employments.Add(employment);
                }

                _person.AddHomeWorkDistanceToEmployments(CurrentUser);
                CurrentUser.CprNumber = "";
                CurrentUser.HasAppPassword = _appLoginRepo.AsQueryable().Any(x => x.PersonId == CurrentUser.Id);
                CurrentUser.IsSubstitute = _substituteRepo.AsQueryable().Any(x => x.SubId.Equals(CurrentUser.Id) && x.StartDateTimestamp < currentDateTimestamp && x.EndDateTimestamp > currentDateTimestamp);
            }
            catch (Exception ex)
            {
                _logger.Error($"{GetType().Name}, GetCurrentUser(), Error", ex);
            }

            return CurrentUser;
        }

        /// <summary>
        /// GET API endpoint for user as CurrentUser.
        /// Sets HomeWorkDistance on each of the users employments.
        /// Strips CPR-number off.
        /// </summary>
        /// <returns>A user with with properties like CurrentUser. Is used when retrieving a user as CurrentUser when an admin tries to edit an approved report.</returns>
        [EnableQuery(MaxExpansionDepth = 4)]
        // Disable caching.
        [OutputCache(NoStore = true, Duration = 0, VaryByParam = "None")]
        public Person GetUserAsCurrentUser(int id)
        {
            var result = Repo.AsQueryable().First(x => x.Id == id);

            var currentDateTimestamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
            var employments = _employmentRepo.AsQueryable().Where(x => x.PersonId == id && (x.EndDateTimestamp == 0 || x.EndDateTimestamp > currentDateTimestamp));
            var employmentList = employments.ToList();

            result.Employments.Clear();

            foreach (var employment in employmentList)
            {
                result.Employments.Add(employment);
            }

            _person.AddHomeWorkDistanceToEmployments(result);
            result.CprNumber = "";
            result.HasAppPassword = _appLoginRepo.AsQueryable().Any(x => x.PersonId == result.Id);
            result.IsSubstitute = _substituteRepo.AsQueryable().Any(x => x.SubId.Equals(result.Id) && x.StartDateTimestamp < currentDateTimestamp && x.EndDateTimestamp > currentDateTimestamp);

            return result;
        }

        //GET: odata/Person(5)
        /// <summary>
        /// GET API endpoint for a single person
        /// Strips CPR-number off.
        /// </summary>
        /// <param name="key">Returns the person identified by key</param>
        /// <param name="queryOptions"></param>
        /// <returns>A single Person</returns>
        public IQueryable<Person> GetPerson([FromODataUri] int key, ODataQueryOptions<Person> queryOptions)
        {
            try
            {
                var cprScrubbed = _person.ScrubCprFromPersons(GetQueryable(key, queryOptions));
                var res = cprScrubbed.ToList();
                var currentTimestamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

                foreach (var person in res.ToList())
                {
                    // Remove employments that have expired.
                    person.Employments = person.Employments.Where(x => x.EndDateTimestamp == 0 || x.EndDateTimestamp > currentTimestamp).ToList();
                }

                return res.AsQueryable();
            }
            catch (RouteInformationException e)
            {
                throw new Exception("Kunne ikke beregne rute mellem hjemme- og arbejdsadresse.", e);
            }
        }

        // PUT: odata/Person(5)
        /// <summary>
        /// Not implemented.
        /// </summary>
        /// <param name="key"></param>
        /// <param name="delta"></param>
        /// <returns></returns>
        public new IHttpActionResult Put([FromODataUri] int key, Delta<Person> delta)
        {
            return base.Put(key, delta);
        }

        // POST: odata/Person
        /// <summary>
        /// Not implemented.
        /// </summary>
        /// <param name="person"></param>
        /// <returns></returns>
        [EnableQuery]
        public new IHttpActionResult Post(Person person)
        {
            return StatusCode(HttpStatusCode.MethodNotAllowed);
        }

        // PATCH: odata/Person(5)
        /// <summary>
        /// PATCH API endpoint for person.
        /// Returns forbidden if the person identified by key is not the current user or if the current user is not an admin.
        /// </summary>
        /// <param name="key">Patches the Person identified by key</param>
        /// <param name="delta"></param>
        /// <returns></returns>
        [EnableQuery]
        [System.Web.Http.AcceptVerbs("PATCH", "MERGE")]
        public new IHttpActionResult Patch([FromODataUri] int key, Delta<Person> delta)
        {
            var person = Repo.AsQueryable().Single(x => x.Id == key);
            return CurrentUser.IsAdmin || CurrentUser.Id == person.Id ? base.Patch(key, delta) : StatusCode(HttpStatusCode.Forbidden);
        }

        // DELETE: odata/Person(5)
        /// <summary>
        /// Not implemented.
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public new IHttpActionResult Delete([FromODataUri] int key)
        {
            return StatusCode(HttpStatusCode.MethodNotAllowed);
        }

        // GET odata/Person(5)/Employments
        /// <summary>
        /// Returns employments belonging to the user identified by key.
        /// </summary>
        /// <param name="key"></param>
        /// <param name="queryOptions"></param>
        /// <returns>Employments</returns>
        [EnableQuery]
        public IHttpActionResult GetEmployments([FromODataUri] int key, ODataQueryOptions<Person> queryOptions)
        {
            var person = Repo.AsQueryable().FirstOrDefault(x => x.Id.Equals(key));

            if (person == null)
            {
                return BadRequest("Der findes ingen person med id " + key);
            }

            person = _person.AddHomeWorkDistanceToEmployments(person);

            var currentTimestamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

            // Remove employments that have expired.
            var res = person.Employments.Where(x => x.EndDateTimestamp == 0 || x.EndDateTimestamp > currentTimestamp);

            return Ok(res);
        }

        // GET: odata/Person(5)/Service.HasLicensePlate
        /// <summary>
        /// Returns whether or not the user identified by key has a license plate.
        /// </summary>
        /// <param name="key"></param>
        /// <param name="parameters"></param>
        /// <returns></returns>
        [EnableQuery]
        [System.Web.Http.HttpGet]
        public IHttpActionResult HasLicensePlate([FromODataUri] int key, ODataActionParameters parameters)
        {
            return Ok(_licensePlateRepo.AsQueryable().Any(x => x.PersonId == key));
        }

        /// <summary>
        /// Gets all persons that for the logged in leader
        /// </summary>
        /// <returns></returns>
        [System.Web.Http.HttpGet]
        public IHttpActionResult GetEmployeesOfLeader()
        {
            List<Person> employees = new List<Person>();

            if (CurrentUser.Employments.Where(e => e.IsLeader).Any() || CurrentUser.SubstituteLeaders.Count > 0)
            {
                employees = _person.GetEmployeesOfLeader(CurrentUser);
            }
            else
            {
                return Unauthorized();
            }

            return Ok(employees);
        }
    }
}
