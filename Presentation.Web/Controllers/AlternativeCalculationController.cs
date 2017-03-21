using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace OS2Indberetning.Controllers
{
    public class AlternativeCalculationController : ApiController
    {
        private readonly IDriveReportService _driveReportService;

        public AlternativeCalculationController(IDriveReportService driveReportService)
        {
            _driveReportService = driveReportService;
        }

        public IHttpActionResult NDKWorkRouteCalculation(int employmentId, DriveReportTransportType transportType, bool startsHome, bool endsHome, [FromBody] Address[] addresses)
        {
            double result = 0.0;
            try
            {
                result = _driveReportService.GetNDKWorkRouteCalculation(employmentId, transportType, startsHome, endsHome, addresses);
            }
            catch (Exception e)
            {
                return InternalServerError(e);
            }
            
            if(result < 0)
            {
                return BadRequest();
            }

            return Ok(result);
        }
    }
}
