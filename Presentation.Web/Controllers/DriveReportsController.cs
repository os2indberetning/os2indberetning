using System;
using System.Data.Entity;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;
using System.Web.Http.Results;
using System.Web.OData;
using System.Web.OData.Query;
using Core.ApplicationServices;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using Ninject;
using System.Configuration;
using System.Collections.Generic;
using Newtonsoft.Json;
using OS2Indberetning.Models;

namespace OS2Indberetning.Controllers
{
    public class DriveReportsController : BaseController<DriveReport>
    {
        private readonly IDriveReportService _driveService;
        private readonly IGenericRepository<Employment> _employmentRepo;
        private readonly IGenericRepository<Person> _personRepo;
        private readonly ILogger _logger;
        private readonly IGenericRepository<OrgUnit> _orgrepo;
        private readonly IGenericRepository<BankAccount> _Bankrepo;




        public DriveReportsController(IGenericRepository<BankAccount> Bankrepo,IGenericRepository<OrgUnit> orgrepo, IGenericRepository<DriveReport> repo, IDriveReportService driveService, IGenericRepository<Person> personRepo, IGenericRepository<Employment> employmentRepo, ILogger logger)
            : base(repo, personRepo)
        {
            _driveService = driveService;
            _employmentRepo = employmentRepo;
            _logger = logger;
            _personRepo = personRepo;
            _orgrepo = orgrepo;
            _Bankrepo = Bankrepo;
        }

        // GET: odata/DriveReports
        /// <summary>
        /// ODATA GET API endpoint for drivereports.
        /// Converts string status to a ReportStatus enum and filters by it.
        /// Filters reports by leaderId and returns reports which that leader is responsible for approving.
        /// Does not return reports for which there is a substitute, unless getReportsWhereSubExists is true.
        /// </summary>
        /// <param name="queryOptions"></param>
        /// <param name="status"></param>
        /// <param name="leaderId"></param>
        /// <param name="getReportsWhereSubExists"></param>
        /// <returns>DriveReports</returns>
        [EnableQuery]
        public IHttpActionResult Get(ODataQueryOptions<DriveReport> queryOptions, string status = "", int leaderId = 0, bool getReportsWhereSubExists = false)
        {
            var queryable = GetQueryable(queryOptions);

            ReportStatus reportStatus;
            if (ReportStatus.TryParse(status, true, out reportStatus))
            {
                if (reportStatus == ReportStatus.Accepted)
                {
                    // If accepted reports are requested, then return accepted and invoiced. 
                    // Invoiced reports are accepted reports that have been processed for payment.
                    // So they are still accepted reports.
                    queryable =
                        queryable.Where(dr => dr.Status == ReportStatus.Accepted || dr.Status == ReportStatus.Invoiced);
                }
                else
                {
                    queryable = queryable.Where(dr => dr.Status == reportStatus);
                }

            }
            return Ok(queryable);
        }

        /// <summary>
        /// Returns the latest drivereport for a given user.
        /// Used for setting the option fields in DrivingView to the same as the latest report by the user.
        /// </summary>
        /// <param name="personId">Id of person to get report for.</param>
        /// <returns></returns>
        [EnableQuery]
        public IHttpActionResult GetLatestReportForUser(int personId)
        {
            var report = Repo.AsQueryable()
                .Where(x => x.PersonId.Equals(personId) && !x.IsFromApp)
                .OrderByDescending(x => x.CreatedDateTimestamp)
                .FirstOrDefault();

            if (report != null)
            {
                return Ok(report);
            }

            return StatusCode(HttpStatusCode.NoContent);
        }

        [HttpGet]
        public IHttpActionResult Eksport( string start, string end, string name, string orgUnit = null, string manr = null)
        {

            var convertedManr = 0;
            var convertedOrgUnit = 0;
            List<DriveReport> reports = new List<DriveReport>();
            
            var person = _personRepo.AsQueryable().Where(x => x.Initials == name).First();

            if (person == null)
            {
                return StatusCode(HttpStatusCode.NoContent);
            }


            if (!string.IsNullOrEmpty(manr) && manr != "undefined")
            {
                convertedManr = Convert.ToInt32(manr);
                reports.AddRange(Repo.AsQueryable().Where(r => r.Employment.EmploymentId == convertedManr && r.Employment.PersonId == person.Id));
            }
            else {
                reports.AddRange(Repo.AsQueryable().Where(x => x.Employment.PersonId == person.Id));
            }


            Core.DomainModel.EksportModel result = new Core.DomainModel.EksportModel();
            try
            {
                var adminName = User.Identity.Name.Split('\\');
                var actualAdminName = adminName[1];
                result.DateInterval = start + " - " + end;
                result.orgUnits = new HashSet<string>();
                result.name = person.FullName;
                result.adminName = _personRepo.AsQueryable().Where(x => x.Initials == actualAdminName).First().FullName;
                result.MaNumbers = new HashSet<int>();
            }
            catch (Exception e)
            {
                _logger.Log("RESULT IN FOR LOOP: " + result.adminName + "Eviroment username: " + Environment.UserName + "Persons repo: " + _personRepo.AsQueryable().Where(x => x != null), "web", 3);
            }


            if (!string.IsNullOrEmpty(orgUnit) && orgUnit != "undefined")
            {
                // convertedOrgUnit = Convert.ToInt32(orgUnit);
                reports = reports.Where(e => e.Employment.OrgUnit.ShortDescription.ToLower() == orgUnit.ToLower()).ToList();

            }

            System.DateTime dtDateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0, System.DateTimeKind.Utc);
            var convertedStart = Convert.ToDateTime(start);
            var convertedEnd = Convert.ToDateTime(end);
            List<Core.DomainModel.EksportDrivereport> drivereports = new List<Core.DomainModel.EksportDrivereport>();

            try
            {
                foreach (var repo in reports)
                {
                    if (repo.PersonId == person.Id)
                    {
                        var createdTime = dtDateTime.AddSeconds(repo.CreatedDateTimestamp).ToLocalTime();

                        if (createdTime > convertedStart && createdTime < convertedEnd)
                        {
                            if (repo.Employment != null)
                            {
                               if (repo.Employment.OrgUnit != null)

                                result.wholeAmount = result.wholeAmount + repo.AmountToReimburse;
                                result.wholeDistance = result.wholeDistance + repo.Distance;
                                result.orgUnits.Add(repo.Employment.OrgUnit.ShortDescription);
                                result.MaNumbers.Add(repo.Employment.EmploymentId);

                                var reportToBeAdded = new Core.DomainModel.EksportDrivereport
                                {
                                    DriveDateTimestamp = repo.DriveDateTimestamp,
                                    CreatedDateTimestamp = repo.CreatedDateTimestamp,
                                    OrgUnit = repo.Employment.OrgUnit.ShortDescription,
                                    Purpose = repo.Purpose,
                                    IsExtraDistance = repo.IsExtraDistance,
                                    FourKmRule = repo.FourKmRule,
                                    distanceFromHomeToBorder = person.DistanceFromHomeToBorder,
                                    AmountToReimburse = repo.AmountToReimburse,
                                    Route = ""
                                };
                                if (repo.AccountNumber != null)
                                {
                                    reportToBeAdded.kontering = repo.AccountNumber;
                                }
                                if (repo.ProcessedDateTimestamp != 0)
                                {
                                    reportToBeAdded.processedDate = dtDateTime.AddSeconds(repo.ProcessedDateTimestamp).ToLocalTime().ToString().Substring(0, 10);
                                }
                                else {
                                    reportToBeAdded.processedDate = "Ikke sendt endnu!";
                                }
                                if (repo.ClosedDateTimestamp != 0)
                                {
                                    reportToBeAdded.approvedDate = dtDateTime.AddSeconds(repo.ClosedDateTimestamp).ToLocalTime().ToString().Substring(0, 10);
                                }
                                else {
                                    reportToBeAdded.approvedDate = "Ikke accepteret endnu!";
                                }
                                if (repo.ApprovedBy != null) {
                                    reportToBeAdded.ApprovedBy = repo.ApprovedBy.FullName;
                                }
                                else
                                {
                                    reportToBeAdded.ApprovedBy = "not Approved yet.";

                                }
                                int counter = 0;
                                foreach (var p in repo.DriveReportPoints.AsQueryable().OrderBy(x=> x.Id)) {

                                    if (counter < 1)
                                    {
                                        reportToBeAdded.Route = reportToBeAdded.Route + p.StreetName + ", " + p.StreetNumber + ", " + p.ZipCode;
                                        counter++;
                                    }
                                    else {
                                        reportToBeAdded.Route =reportToBeAdded.Route + " - " + p.StreetName + ", " + p.StreetNumber +", " + p.ZipCode;

                                    }


                                }
                                drivereports.Add(reportToBeAdded);
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                _logger.Log("drivereports Error " + e.Message, "web", 3);
            }

            result.driveReports = drivereports.ToArray();
            result.municipality = reports.Select(x => x.Employment.OrgUnit.LongDescription).FirstOrDefault();


            foreach (var r in result.driveReports) {

                if (r.kontering != null) {
                    r.kontering = _Bankrepo.AsQueryable().Where(x=> x.Number == r.kontering).FirstOrDefault().Description;
                }
                else
                {
                    r.kontering = "ingen kontering";
                }
            }

            if (result.driveReports.Count() > 0)
            {
                return Json(result);
            }

            return StatusCode(HttpStatusCode.NoContent);
        }

        public string getRouteByreportId(int id){


            return "";
        }

        public string convertRoute(EksportDrivereport[] reports){
            string result = "Kan ikke finde ruten.";


            foreach (var r in reports) {

            }

            return null;
        }
        

        
        //GET: odata/DriveReports(5)
        /// <summary>
        /// ODATA API endpoint for a single drivereport.
        /// </summary>
        /// <param name="key"></param>
        /// <param name="queryOptions"></param>
        /// <returns>A single DriveReport</returns>
        public IHttpActionResult GetDriveReport([FromODataUri] int key, ODataQueryOptions<DriveReport> queryOptions)
        {
            return Ok(GetQueryable(key, queryOptions));
        }

        // PUT: odata/DriveReports(5)
        /// <summary>
        /// Not implemented.
        /// </summary>
        /// <param name="key"></param>
        /// <param name="delta"></param>
        /// <returns></returns>
        public new IHttpActionResult Put([FromODataUri] int key, Delta<DriveReport> delta)
        {
            return base.Put(key, delta);
        }

        // POST: odata/DriveReports
        /// <summary>
        /// ODATA POST api endpoint for drivereports.
        /// Returns forbidden if the user associated with the posted report is not the current user.
        /// </summary>
        /// <param name="driveReport"></param>
        /// <returns>The posted report.</returns>
        [EnableQuery]
        public new IHttpActionResult Post(DriveReport driveReport, string emailText)
        {
            if(CurrentUser.IsAdmin && emailText != null && driveReport.Status == ReportStatus.Accepted)
            {
                // An admin is trying to edit an already approved report.
                var adminEditResult = _driveService.Create(driveReport);
                // CurrentUser is restored after the calculation.
                _driveService.SendMailToUserAndApproverOfEditedReport(adminEditResult, emailText, CurrentUser, "redigeret");
                return Ok(adminEditResult);
            }

            if (!CurrentUser.Id.Equals(driveReport.PersonId))
            {
                return StatusCode(HttpStatusCode.Forbidden);
            }

            var result = _driveService.Create(driveReport);

            return Ok(result);
        }

        // PATCH: odata/DriveReports(5)
        /// <summary>
        /// PATCH API endpoint for drivereports.
        /// Returns forbidden if a user is trying to patch his/her own report or if the user is not the responsible leader for the report.
        /// Also returns forbidden if the report to be patched has a status other than pending.
        /// </summary>
        /// <param name="key"></param>
        /// <param name="delta"></param>
        /// <param name="emailText">The message to be sent to the owner of a report an admin has rejected or edited.</param>
        /// <returns></returns>
        [EnableQuery]
        [AcceptVerbs("PATCH", "MERGE")]
        public new IHttpActionResult Patch([FromODataUri] int key, Delta<DriveReport> delta, string emailText)
        {

            var report = Repo.AsQueryable().SingleOrDefault(x => x.Id == key);

            if (report == null)
            {
                return NotFound();
            }

            var leader = report.ResponsibleLeader;

            if (leader == null)
            {
                return StatusCode(HttpStatusCode.Forbidden);
            }

            if (CurrentUser.IsAdmin && emailText != null && report.Status == ReportStatus.Accepted)
            {
                // An admin is trying to reject an approved report.
                report.Status = ReportStatus.Rejected;
                report.Comment = emailText;
                report.ClosedDateTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
                try {
                    Repo.Save();
                    _driveService.SendMailToUserAndApproverOfEditedReport(report, emailText, CurrentUser, "afvist");
                    return Ok();
                } catch(Exception e) {
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

        // DELETE: odata/DriveReports(5)
        /// <summary>
        /// DELETE API endpoint for drivereports.
        /// Deletes the report identified by key if the current user is the owner of the report or is an admin.
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public new IHttpActionResult Delete([FromODataUri] int key)
        {
            if (CurrentUser.IsAdmin)
            {
                return base.Delete(key);
            }
            var report = Repo.AsQueryable().SingleOrDefault(x => x.Id.Equals(key));
            if (report == null)
            {
                return NotFound();
            }
            return report.PersonId.Equals(CurrentUser.Id) ? base.Delete(key) : Unauthorized();
        }
    }
}
