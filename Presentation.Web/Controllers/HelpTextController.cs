using Core.ApplicationServices;
using Core.ApplicationServices.Interfaces;
using Core.DomainServices.Interfaces;
using Ninject;
using OS2Indberetning.Filters;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;
using System.Web.Http.Controllers;

namespace OS2Indberetning.Controllers
{
    [AuditlogFilter]
    public class HelpTextController : ApiController
    {
        private ICustomSettings _customSettings;

        protected override void Initialize(HttpControllerContext controllerContext)
        {
            _customSettings = NinjectWebKernel.GetKernel().Get<ICustomSettings>();

            base.Initialize(controllerContext);
        }
        // GET api/<controller>/5
        /// <summary>
        /// API Endpoint for getting help texts to be displayed in the frontend.
        /// HelpTexts are read from CustomSettings.config
        /// </summary>
        /// <param name="id">Returns the helptext identified by id</param>
        /// <returns>Help text</returns>
        public IHttpActionResult Get(string id)
        {
            try
            {
                // Do not allow returning of keys that start with PROTECTED.
                if (id.IndexOf("PROTECTED", StringComparison.Ordinal) > -1)
                {
                    // If the key contains PROTECTED, then return forbidden.
                    return StatusCode(HttpStatusCode.Forbidden);
                }
                // If the key doesnt contain protected, then return the result.
                var res = ConfigurationManager.AppSettings[id];
                return Ok(res);
            }
            catch (Exception e)
            {
                return InternalServerError();
            }
        }

        /// <summary>
        /// Returns all help texts to cut down number of requests.
        /// </summary>
        /// <param name="id"></param>
        /// <returns></returns>
        public IHttpActionResult GetAll()
        {
            var res = new List<KeyValuePair<string, string>>();
            try
            {
                res.AddRange(from key in ConfigurationManager.AppSettings.AllKeys 
                             where !key.Contains("PROTECTED") select new KeyValuePair<string, string>(key, ConfigurationManager.AppSettings[key]));

                return Ok(res);
            }
            catch (Exception e)
            {
                return InternalServerError();
            }
        }
    }
}