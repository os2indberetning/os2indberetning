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
using OS2Indberetning.Filters;
using System.Globalization;

namespace OS2Indberetning.Controllers
{
    public class DriveReportsController : BaseController<DriveReport>
    {
        private readonly IDriveReportService _driveService;
        private readonly IGenericRepository<Employment> _employmentRepo;
        private readonly IGenericRepository<Person> _personRepo;
        private readonly IGenericRepository<OrgUnit> _orgrepo;
        private readonly IGenericRepository<BankAccount> _Bankrepo;
        private readonly IGenericRepository<LicensePlate> _LicensePlateRepo;


        public DriveReportsController(IGenericRepository<BankAccount> Bankrepo, IGenericRepository<OrgUnit> orgrepo, IGenericRepository<DriveReport> repo, IDriveReportService driveService, IGenericRepository<Person> personRepo, IGenericRepository<Employment> employmentRepo, IGenericRepository<LicensePlate> licensePlateRepo)
            : base(repo, personRepo)
        {
            _driveService = driveService;
            _employmentRepo = employmentRepo;
            _personRepo = personRepo;
            _orgrepo = orgrepo;
            _Bankrepo = Bankrepo;
            _LicensePlateRepo = licensePlateRepo;
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
        public IHttpActionResult Get(ODataQueryOptions<DriveReport> queryOptions, string from = "", string status = "", int leaderId = 0, bool getReportsWhereSubExists = false)
        {
            IQueryable<DriveReport> queryable = null;
            try
            {
                queryable = GetQueryable(queryOptions);

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

                    switch (from)
                    {
                        case "approve":
                                queryable = queryable.Where(dr => dr.ResponsibleLeaders.Any(p => p.Id == CurrentUser.Id));
                            break;
                        default:
                            break;
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.Error($"{GetType().Name}, Get(), queryOption={queryOptions}, status={status}, leaderId={leaderId}, getReportsWhereSubExists={getReportsWhereSubExists}", ex);
            }
            return Ok(queryable);
        }

        [EnableQuery]
        public IHttpActionResult Get(ODataQueryOptions<DriveReport> queryOptions, string queryType)
        {
            IQueryable<DriveReport> queryable = null;
            switch (queryType)
            {
                case "admin":
                    {
                        if (CurrentUser.IsAdmin)
                        {
                            queryable = GetQueryable(queryOptions);
                        }
                        else
                        {
                            return Unauthorized();
                        }
                    }
                    break;
                case "godkender":
                    {
                        if(CurrentUser.Employments.Any(em => em.IsLeader) || CurrentUser.SubstituteLeaders.Count > 0)
                        {
                            queryable = GetQueryable(queryOptions);
                        }
                        else
                        {
                            return Unauthorized();
                        }
                    }
                    break;
                case "mine":
                    {
                        queryable = GetQueryable(queryOptions).Where(dr => dr.PersonId == CurrentUser.Id);                        
                    }
                    break;
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
            try
            {
                var currentTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
                var report = Repo.AsQueryable()
                    .Where(
                        x => x.PersonId.Equals(personId)
                        && x.Employment.StartDateTimestamp < currentTimestamp
                        && (x.Employment.EndDateTimestamp > currentTimestamp || x.Employment.EndDateTimestamp == 0)
                        && !x.IsFromApp)
                    .OrderByDescending(x => x.CreatedDateTimestamp)
                    .FirstOrDefault();

                if (report != null)
                {
                    return Ok(report);
                }


            }
            catch (Exception ex)
            {
                _logger.Error($"{GetType().Name}, GetLatestReportForUser(), personId={personId}", ex);
            }
            _logger.Debug($"{GetType().Name}, GetLatestReportForUser(), personId={personId}, statusCode=204 No Content");
            return StatusCode(HttpStatusCode.NoContent);
        }

        /// <summary>
        /// Returns a bool indicating if the special Norddjurs calculation method is configured to be used.
        /// Used for setting the available options in the kilometerallowance menu.
        /// </summary>
        /// <returns></returns>
        [EnableQuery]
        public IHttpActionResult GetCalculationMethod()
        {
            bool isAltCalc;
            bool parseSucces = bool.TryParse(_customSettings.AlternativeCalculationMethod, out isAltCalc);
            _logger.Debug($"{GetType().Name}, GetCalculationMethod(), isAltCalc={isAltCalc}");

            if (parseSucces)
            {
                return Ok(isAltCalc);
            }
            else
            {
                return Ok(false);
            }

        }
        
        private string GetStatusString(ReportStatus status)
        {
            string toReturn = "";
            switch(status)
            {
                case ReportStatus.Accepted:
                    toReturn = "Godkendt";
                    break;
                case ReportStatus.Invoiced:
                    toReturn = "Overført til løn";
                    break;
                case ReportStatus.Pending:
                    toReturn = "Afventer";
                    break;
                case ReportStatus.Rejected:
                    toReturn = "Afvist";
                    break;
            }
            return toReturn;
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
            if (CurrentUser.IsAdmin && emailText != null && driveReport.Status == ReportStatus.Accepted)
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

            if (report.ResponsibleLeaders.Count == 0)
            {
                return StatusCode(HttpStatusCode.Forbidden);
            }

            if (CurrentUser.IsAdmin && emailText != null && report.Status == ReportStatus.Accepted)
            {
                // An admin is trying to reject an approved report.
                report.Status = ReportStatus.Rejected;
                report.Comment = emailText;
                report.ClosedDateTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
                report.ApprovedBy = CurrentUser;
                try
                {
                    Repo.Save();
                    if (report.FourKmRule)
                    {
                        _driveService.CalculateFourKmRuleForOtherReports(report); 
                    }
                    _driveService.SendMailToUserAndApproverOfEditedReport(report, emailText, CurrentUser, "afvist");
                    return Ok();
                }
                catch (Exception e)
                {
                    _logger.LogForAdmin($"Fejl under forsøg på at afvise en allerede godkendt indberetning fra {report.Person.FullName}. Rapportens status er ikke ændret.");
                }
            }


            // Cannot approve own reports.
            if (report.PersonId == CurrentUser.Id)
            {
                return StatusCode(HttpStatusCode.Forbidden);
            }

            // Cannot approve reports where you are not responsible leader
            if (!report.IsPersonResponsible(CurrentUser.Id))
            {
                return StatusCode(HttpStatusCode.Forbidden);
            }

            // Return Unauthorized if the status is not pending when trying to patch.
            // User should not be allowed to change a Report which has been accepted or rejected.
            if (report.Status != ReportStatus.Pending)
            {
                _logger.LogForAdmin("Forsøg på at redigere indberetning med anden status end afventende. Rapportens status er ikke ændret.");
                return StatusCode(HttpStatusCode.Forbidden);
            }

            var status = new object();
            if (delta.TryGetPropertyValue("Status", out status))
            {
                if (status.ToString().Equals("Rejected"))
                {
                    bool sendEmailResult = true;
                    try
                    {
                        base.Patch(key, delta);
                        _driveService.CalculateFourKmRuleForOtherReports(report);
                    }
                    catch (Exception ex)
                    {
                        _logger.Error($"{GetType().Name}, Patch(), Error when trying to update report status for user {report.Person.FullName}", ex);
                        return InternalServerError();
                    }

                    try
                    {
                        _driveService.SendMailForRejectedReport(key, delta);
                    }
                    catch
                    {
                        _logger.LogForAdmin($"{report.Person.FullName} har fået en indberetning afvist af sin leder, men er ikke blevet notificeret via email");
                        sendEmailResult = false;
                    }
                    return Ok(sendEmailResult);
                }
            }

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
            var report = Repo.AsQueryable().SingleOrDefault(x => x.Id.Equals(key));
            if (report == null)
            {
                return NotFound();
            }
            if (report.PersonId.Equals(CurrentUser.Id) || CurrentUser.IsAdmin)
            {
                var deleteResult = base.Delete(key);
                if (report.FourKmRule)
                {
                    _driveService.CalculateFourKmRuleForOtherReports(report); 
                }
                return deleteResult;
            }
            else
            {
                return Unauthorized();
            }
        }
    }
}
