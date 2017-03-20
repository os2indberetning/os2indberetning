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

        public IHttpActionResult NDKWorkRouteCalculation(int employmentId, DriveReportTransportType transportType, [FromBody] Address[] addresses)
        {
            var result = _driveReportService.GetNDKWorkRouteCalculation(employmentId, transportType, addresses);
            int i = 1;
            return Ok();
        }
    }
}
