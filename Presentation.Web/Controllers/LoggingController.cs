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
                    string resultString = "";
                    foreach(var entry in message)
                    {
                        resultString += entry.Key + "---" + entry.Value + " | ";
                    }
                    _logger.AuditLog(resultString);
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
            try
            {
                if (message != null)
                {
                    string resultString = "";
                    foreach (var entry in message)
                    {
                        //resultString += entry.Key + "---" + entry.Value + " | ";
                    }
                    _logger.AuditLog(resultString);
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
