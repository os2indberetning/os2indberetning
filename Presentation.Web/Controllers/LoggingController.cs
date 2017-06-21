using Core.ApplicationServices.Logger;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace OS2Indberetning.Controllers
{
    public class LoggingController : ApiController
    {
        private readonly ILogger _logger;

        public LoggingController()
        {
            _logger = new Logger();
        }

        [Route("api/logging/audit")]
        public IHttpActionResult Audit([FromBody] Dictionary<string, string> message)
        {
            try
            {
                if (message != null)
                {
                    string user, location, controller, action, parameters = null;
                    message.TryGetValue("user", out user);
                    message.TryGetValue("location", out location);
                    message.TryGetValue("controller", out controller);
                    message.TryGetValue("action", out action);
                    message.TryGetValue("parameters", out parameters);

                    _logger.AuditLogDMZ(user, location, controller, action, parameters);
                    return Ok();
                }
                else
                {
                    return BadRequest();
                }
            }
            catch (Exception e)
            {
                _logger.Error("Error when trying to auditlog from API", e);
                return InternalServerError();
            }
        }

        [Route("api/logging/testcon")]
        public IHttpActionResult TestCon([FromBody] string message)
        {
            return Ok("test");
        }

        public IHttpActionResult Debug(string message)
        {
            throw new NotImplementedException();
        }

        public IHttpActionResult Error(string message)
        {
            throw new NotImplementedException();
        }

        public IHttpActionResult ForAdmin(string message)
        {
            throw new NotImplementedException();
        }

    }
}
