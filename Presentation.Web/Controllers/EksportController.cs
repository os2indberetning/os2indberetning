using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using OS2Indberetning.Controllers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Web;
using System.Web.Mvc;
using System.Web.OData;
using System.Web.OData.Query;

namespace OS2Indberetning.Controllers
{
    public class EksportController : BaseController<DriveReport>
    {
        private readonly IDriveReportService _driveService;
        private readonly IGenericRepository<Employment> _employmentRepo;

        private readonly ILogger _logger;

        public EksportController(IGenericRepository<DriveReport> repo, IDriveReportService driveService, IGenericRepository<Person> personRepo, IGenericRepository<Employment> employmentRepo, ILogger logger): base(repo, personRepo)
        {
            _driveService = driveService;
            _employmentRepo = employmentRepo;
            _logger = logger;
        }


        [EnableQuery]
        public System.Web.Http.IHttpActionResult Get(ODataQueryOptions<DriveReport> queryOptions, string Employee,string Manr, string from, string to)
        {
            var queryable = GetQueryable(queryOptions);

           // ReportStatus reportStatus;
           
               
                    // If accepted reports are requested, then return accepted and invoiced. 
                    // Invoiced reports are accepted reports that have been processed for payment.
                    // So they are still accepted reports.
                   // queryable =
                   //     queryable.Where(dr => dr.Status == ReportStatus.Accepted || dr.Status == ReportStatus.Invoiced);
               

            
            return Ok(queryable);
        }

        [EnableQuery]
        [AcceptVerbs("PATCH", "MERGE")]
        public new System.Web.Http.IHttpActionResult Patch([FromODataUri] int key, Delta<DriveReport> delta, string emailText)
        {

            var report = Repo.AsQueryable().SingleOrDefault(x => x.Id == key);

            if (report == null)
            {
                return NotFound();
            }

            var leader = report.ResponsibleLeader;

            if (leader == null)
            {
                return StatusCode(System.Net.HttpStatusCode.Forbidden);
            }

            if (CurrentUser.IsAdmin && emailText != null && report.Status == ReportStatus.Accepted)
            {
                // An admin is trying to reject an approved report.
                report.Status = ReportStatus.Rejected;
                report.Comment = emailText;
                report.ClosedDateTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
                try
                {
                    Repo.Save();
                    _driveService.SendMailToUserAndApproverOfEditedReport(report, emailText, CurrentUser, "afvist");
                    return Ok();
                }
                catch (Exception e)
                {
                    _logger.Log("Fejl under forsøg på at afvise en allerede godkendt indberetning. Rapportens status er ikke ændret.", "web", e, 3);
                }
            }


            // Cannot approve own reports.
            if (report.PersonId == CurrentUser.Id)
            {
                return StatusCode(HttpStatusCode.Forbidden);
            }

            // Cannot approve reports where you are not responsible leader
            if (!CurrentUser.Id.Equals(leader.Id))
            {
                return StatusCode(HttpStatusCode.Forbidden);
            }


            // Return Unauthorized if the status is not pending when trying to patch.
            // User should not be allowed to change a Report which has been accepted or rejected.
            if (report.Status != ReportStatus.Pending)
            {
                _logger.Log("Forsøg på at redigere indberetning med anden status end afventende. Rapportens status er ikke ændret.", "web", 3);
                return StatusCode(HttpStatusCode.Forbidden);
            }


            _driveService.SendMailIfRejectedReport(key, delta);
            return base.Patch(key, delta);
        }


    }
}