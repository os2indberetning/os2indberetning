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
        private readonly IGenericRepository<LicensePlate> _LicensePlateRepo;




        public DriveReportsController(IGenericRepository<BankAccount> Bankrepo, IGenericRepository<OrgUnit> orgrepo, IGenericRepository<DriveReport> repo, IDriveReportService driveService, IGenericRepository<Person> personRepo, IGenericRepository<Employment> employmentRepo, IGenericRepository<LicensePlate> licensePlateRepo, ILogger logger)
            : base(repo, personRepo)
        {
            _driveService = driveService;
            _employmentRepo = employmentRepo;
            _logger = logger;
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
        public IHttpActionResult Get(ODataQueryOptions<DriveReport> queryOptions, string status = "", int leaderId = 0, bool getReportsWhereSubExists = false)
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

                }
            }
            catch (Exception ex)
            {
                _logger.Error($"{GetType().Name}, Get(), queryOption={queryOptions}, status={status}, leaderId={leaderId}, getReportsWhereSubExists={getReportsWhereSubExists}", ex);
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
            bool parseSucces = bool.TryParse(ConfigurationManager.AppSettings["AlternativeCalculationMethod"], out isAltCalc);
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

        /// <summary>
        /// Used for generating reports for the tax authorities.
        /// </summary>
        /// <param name="start"></param>
        /// <param name="end"></param>
        /// <param name="name"></param>
        /// <param name="orgUnit"></param>
        /// <returns></returns>
        [HttpGet]
        public IHttpActionResult Eksport(string start, string end, string personId, string orgunitId = null)
        {
            _logger.Debug($"{GetType().Name}, Eksport(), start={start}, end={end}, person={personId}, orgUnit={orgunitId}");

            // Validate parameters
            long parsedStartDateUnix;
            long parsedEndDateUnix;
            int parsedPersonId;
            int parsedOrgunitId = -1;

            if (string.IsNullOrEmpty(personId) || string.IsNullOrEmpty(start) || string.IsNullOrEmpty(end))
            {
                return StatusCode(HttpStatusCode.NoContent);
                _logger.Error($"{GetType().Name}, Eksport(), start={start}, end={end}, person={personId}, orgUnit={orgunitId}, statusCode=204 No Content");
            }
            else
            {
                try
                {
                    parsedStartDateUnix = (long)DateTime.Parse(start).Subtract(new DateTime(1970, 1, 1)).TotalSeconds;
                    parsedEndDateUnix = (long)DateTime.Parse(end).Subtract(new DateTime(1970, 1, 1)).TotalSeconds;
                    parsedPersonId = int.Parse(personId);
                }
                catch (Exception)
                {
                    return StatusCode(HttpStatusCode.NoContent);
                    _logger.Error($"{GetType().Name}, Eksport(), start={start}, end={end}, person={personId}, orgUnit={orgunitId}, statusCode=204 No Content");
                }
            }

            // Get person for which report has been requested
            var person = _personRepo.AsQueryable().Where(x => x.Id == parsedPersonId).FirstOrDefault();
            if (person == null)
            {
                return StatusCode(HttpStatusCode.NoContent);
                _logger.Error($"{GetType().Name}, Eksport(), start={start}, end={end}, person={personId}, orgUnit={orgunitId}, statusCode=204 No Content");
            }

            // Get all the persons drivereports that has been invoiced in the requested timespan, and only for the supplied orgunit if orgunit is supplied.
            List<DriveReport> reportsForRequestedTimespan = new List<DriveReport>();
            if (orgunitId == null || orgunitId.Equals("undefined"))
            {
                reportsForRequestedTimespan.AddRange(Repo.AsQueryable().Where(r => r.PersonId == person.Id && r.Status == ReportStatus.Invoiced && r.ProcessedDateTimestamp >= parsedStartDateUnix && r.ProcessedDateTimestamp <= parsedEndDateUnix));
            }
            else
            {
                parsedOrgunitId = int.Parse(orgunitId);
                reportsForRequestedTimespan.AddRange(Repo.AsQueryable().Where(r => r.PersonId == person.Id && r.Employment.OrgUnit.Id.Equals(parsedOrgunitId) && r.Status == ReportStatus.Invoiced && r.ProcessedDateTimestamp >= parsedStartDateUnix && r.ProcessedDateTimestamp <= parsedEndDateUnix));
            }

            // Initialize EksportModel
            ExportModel result = new ExportModel();
            try
            {
                var adminInitials = User.Identity.Name.Split('\\')[1];
                result.DateInterval = $"{start} - {end}";
                result.OrgUnit = (parsedOrgunitId < 0) ? "Ikke angivet" : _orgrepo.AsQueryable().Where(x => x.Id == parsedOrgunitId).FirstOrDefault().LongDescription;
                result.Name = person.FullName;
                result.AdminName = _personRepo.AsQueryable().Where(x => x.Initials == adminInitials).First().FullName;
                result.Municipality = ConfigurationManager.AppSettings["PROTECTED_muniplicity"] ?? "Ikke angivet";
                result.LicensePlates = string.Join(", ", _LicensePlateRepo.AsQueryable().Where(x => x.PersonId == person.Id).Select(y => y.Plate).ToArray()); // Combine all the users license plates into comma seperated string.

                // Get alternative home adress if user has one, otherwise get home address
                var HomeAddress = person.PersonalAddresses.Where(x => x.Type == PersonalAddressType.AlternativeHome).FirstOrDefault() ?? person.PersonalAddresses.Where(x => x.Type == PersonalAddressType.Home).FirstOrDefault();
                result.HomeAddressStreetAndNumber = $"{HomeAddress.StreetName} {HomeAddress.StreetNumber}";
                result.HomeAddressZipCodeAndTown = $"{HomeAddress.ZipCode} {HomeAddress.Town}";
            }
            catch (Exception e)
            {
                _logger.Error($"{GetType().Name}, Eksport(), start={start}, end={end}, person={personId}, orgUnit={orgunitId}, Error when initializing export model", e);
            }

            DateTime unixDateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
            List<ExportDriveReport> drivereports = new List<ExportDriveReport>();

            try
            {
                foreach (var currentReport in reportsForRequestedTimespan)
                {
                    result.TotalAmount = result.TotalAmount + currentReport.AmountToReimburse;
                    result.TotalDistance = result.TotalDistance + currentReport.Distance;

                    var driveDate = unixDateTime.AddSeconds(currentReport.DriveDateTimestamp).ToLocalTime();
                    var createdDate = unixDateTime.AddSeconds(currentReport.CreatedDateTimestamp).ToLocalTime();

                    var reportToBeAdded = new ExportDriveReport
                    {
                        DriveDateTimestamp = driveDate.ToString().Substring(0, 10),
                        CreatedDateTimestamp = createdDate.ToString().Substring(0, 10),
                        OrgUnit = currentReport.Employment.OrgUnit.ShortDescription,
                        Purpose = currentReport.Purpose,
                        IsExtraDistance = currentReport.IsExtraDistance,
                        FourKmRule = currentReport.FourKmRule,
                        FourKmRuleDeducted = currentReport.FourKmRuleDeducted,
                        SixtyDaysRule = currentReport.SixtyDaysRule,
                        DistanceFromHomeToBorder = currentReport.FourKmRule ? (currentReport.IsRoundTrip.HasValue && currentReport.IsRoundTrip.Value ? person.DistanceFromHomeToBorder * 2 : person.DistanceFromHomeToBorder) : 0,
                        AmountToReimburse = currentReport.AmountToReimburse,
                        ApprovedDate = unixDateTime.AddSeconds(currentReport.ClosedDateTimestamp).ToLocalTime().ToString().Substring(0, 10), // currentReport will always be accepted, since it has been invoiced
                        ProcessedDate = unixDateTime.AddSeconds(currentReport.ProcessedDateTimestamp).ToLocalTime().ToString().Substring(0, 10),
                        ApprovedBy = currentReport.ApprovedBy.FullName,
                        Route = "",
                        Distance = currentReport.Distance,
                        IsRoundTrip = currentReport.IsRoundTrip,
                        LicensePlate = currentReport.LicensePlate,
                        Rate = currentReport.KmRate,
                        HomeAddress = currentReport.Person.PersonalAddresses.Where(x => x.Type == PersonalAddressType.Home).First().Description,
                        UserComment = ""
                    };

                    bool firstPoint = true;
                    if (currentReport.KilometerAllowance == KilometerAllowance.Read)
                    {
                        reportToBeAdded.Route = "Aflæst";
                        reportToBeAdded.UserComment = currentReport.UserComment;
                    }
                    else
                    {
                        foreach (var point in currentReport.DriveReportPoints.AsQueryable().OrderBy(x => x.Id))
                        {
                            if (firstPoint)
                            {
                                reportToBeAdded.Route = reportToBeAdded.Route + point.StreetName + ", " + point.StreetNumber + ", " + point.ZipCode;
                                firstPoint = false;
                            }
                            else
                            {
                                reportToBeAdded.Route = reportToBeAdded.Route + " - " + point.StreetName + ", " + point.StreetNumber + ", " + point.ZipCode;
                            }
                        }
                    }

                    drivereports.Add(reportToBeAdded);
                }
            }
            catch (Exception e)
            {
                _logger.Error($"{GetType().Name}, Eksport(), start={start}, end={end}, person={personId}, orgUnit={orgunitId}, DriveReports error", e);
            }

            result.DriveReports = drivereports.ToArray();

            return Json(result);
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
            if (!CurrentUser.Id.Equals(leader.Id))
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
