using Core.DomainModel;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Web;
using System.Web.Http;
using System.Web.Mvc;

namespace OS2Indberetning.Controllers
{
    public class ConfigurationsController : ApiController
    {
        public ConfigurationsController() {
        }

        // GET: Configurations
        public IHttpActionResult Get(string Id)
        {
            CustomConfiguration config = new CustomConfiguration() { value = ConfigurationManager.AppSettings[Id], key = Id };
            return Json(config);
        }
    }
}