﻿using System;
using System.Collections.Generic;
using System.Data;
using System.Globalization;
using System.Linq;
using System.Web.OData;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using Core.ApplicationServices.Logger;
using Core.DomainServices.Interfaces;

namespace Core.ApplicationServices
{
    public class DriveReportService : IDriveReportService
    {
        private readonly IRoute<RouteInformation> _route;
        private readonly IGenericRepository<RateType> _rateTypeRepo;
        private readonly IAddressCoordinates _coordinates;
        private readonly IGenericRepository<DriveReport> _driveReportRepository;
        private readonly IReimbursementCalculator _calculator;
        private readonly IGenericRepository<OrgUnit> _orgUnitRepository;
        private readonly IGenericRepository<Employment> _employmentRepository;
        private readonly IGenericRepository<Substitute> _substituteRepository;
        private readonly IGenericRepository<Person> _personRepository;
        private readonly IMailService _mailService;

        private readonly ILogger _logger;
        private readonly ICustomSettings _customSettings;

        public DriveReportService(IMailService mailService, IGenericRepository<DriveReport> driveReportRepository, IReimbursementCalculator calculator, IGenericRepository<OrgUnit> orgUnitRepository, IGenericRepository<Employment> employmentRepository, IGenericRepository<Substitute> substituteRepository, IAddressCoordinates coordinates, IRoute<RouteInformation> route, IGenericRepository<RateType> rateTypeRepo, IGenericRepository<Person> personRepo, ILogger logger, ICustomSettings customSettings)
        {
            _route = route;
            _rateTypeRepo = rateTypeRepo;
            _coordinates = coordinates;
            _calculator = calculator;
            _orgUnitRepository = orgUnitRepository;
            _employmentRepository = employmentRepository;
            _substituteRepository = substituteRepository;
            _mailService = mailService;
            _driveReportRepository = driveReportRepository;
            _personRepository = personRepo;
            _logger = logger;
            _customSettings = customSettings;
        }

        /// <summary>
        /// Validates report and creates it in the database if it validates.
        /// </summary>
        /// <param name="report">Report to be created.</param>
        /// <returns>Created report.</returns>
        public DriveReport Create(DriveReport report)
        {
            if (report.PersonId == 0)
            {
                throw new Exception("No person provided");
            }
            if (!Validate(report))
            {
                throw new Exception("DriveReport has some invalid parameters");
            }

            if (report.IsFromApp)
            {
                report = _calculator.Calculate(null, report);
            }
            else
            {
                if (report.KilometerAllowance != KilometerAllowance.Read)
                {
                    var pointsWithCoordinates = new List<DriveReportPoint>();
                    foreach (var driveReportPoint in report.DriveReportPoints)
                    {
                        if (string.IsNullOrEmpty(driveReportPoint.Latitude) || driveReportPoint.Latitude == "0" ||
                            string.IsNullOrEmpty(driveReportPoint.Longitude) || driveReportPoint.Longitude == "0")
                        {
                            pointsWithCoordinates.Add(
                                (DriveReportPoint)_coordinates.GetAddressCoordinates(driveReportPoint));
                        }
                        else
                        {
                            pointsWithCoordinates.Add(driveReportPoint);
                        }
                    }

                    report.DriveReportPoints = pointsWithCoordinates;

                    var isBike = _rateTypeRepo.AsQueryable().First(x => x.TFCode.Equals(report.TFCode)).IsBike;


                    // Set transportType to Bike if isBike is true. Otherwise set it to Car.
                    var drivenRoute = _route.GetRoute(
                        isBike ? DriveReportTransportType.Bike : DriveReportTransportType.Car, report.DriveReportPoints);


                    report.Distance = drivenRoute.Length / 1000;

                    if (report.Distance < 0)
                    {
                        report.Distance = 0;
                    }

                    report = _calculator.Calculate(drivenRoute, report);
                }
                else
                {
                    report = _calculator.Calculate(null, report);
                }
            }

            // Round off Distance and AmountToReimburse to two decimals.
            report.Distance = Convert.ToDouble(report.Distance.ToString("0.##", CultureInfo.InvariantCulture), CultureInfo.InvariantCulture);
            report.AmountToReimburse = Convert.ToDouble(report.AmountToReimburse.ToString("0.##", CultureInfo.InvariantCulture), CultureInfo.InvariantCulture);

            var createdReport = _driveReportRepository.Insert(report);
            createdReport.UpdateResponsibleLeaders(GetResponsibleLeadersForReport(report));
            var actualLeader = GetActualLeaderForReport(report);
            if (actualLeader != null)
            {
                createdReport.ActualLeaderId = actualLeader.Id;
            }

            if (report.Status == ReportStatus.Rejected)
            {
                // User is editing a rejected report to try and get it approved.
                report.Status = ReportStatus.Pending;
            }

            _driveReportRepository.Save();

            // If the report is calculated or from an app, then we would like to store the points.
            if (report.KilometerAllowance != KilometerAllowance.Read || report.IsFromApp)
            {
                // Reports from app with manual distance have no drivereportpoints.
                if (report.DriveReportPoints.Count > 1)
                {
                    for (var i = 0; i < createdReport.DriveReportPoints.Count; i++)
                    {
                        var currentPoint = createdReport.DriveReportPoints.ElementAt(i);

                        if (i == report.DriveReportPoints.Count - 1)
                        {
                            // last element   
                            currentPoint.PreviousPointId = createdReport.DriveReportPoints.ElementAt(i - 1).Id;
                        }
                        else if (i == 0)
                        {
                            // first element
                            currentPoint.NextPointId = createdReport.DriveReportPoints.ElementAt(i + 1).Id;
                        }
                        else
                        {
                            // between first and last
                            currentPoint.NextPointId = createdReport.DriveReportPoints.ElementAt(i + 1).Id;
                            currentPoint.PreviousPointId = createdReport.DriveReportPoints.ElementAt(i - 1).Id;
                        }
                    }
                    _driveReportRepository.Save();
                }
            }

            //AddFullName(report);

            SixtyDayRuleCheck(report);

            return report;
        }

        /// <summary>
        /// Validates report.
        /// </summary>
        /// <param name="report">Report to be validated.</param>
        /// <returns>True or false</returns>
        public bool Validate(DriveReport report)
        {
            // Report does not validate if it is read and distance is less than zero.
            if (report.KilometerAllowance == KilometerAllowance.Read && report.Distance < 0)
            {
                return false;
            }
            // Report does not validate if it is calculated and has less than two points.
            if (report.KilometerAllowance != KilometerAllowance.Read && report.DriveReportPoints.Count < 2)
            {
                return false;
            }
            // Report does not validate if it has no purpose given.
            if (string.IsNullOrEmpty(report.Purpose))
            {
                return false;
            }
            return true;
        }

        /// <summary>
        /// Is called from DriveReport Patch.
        /// Sends email to the user associated with the report identified by key, with notification about rejected report.
        /// </summary>
        /// <param name="key"></param>
        /// <param name="delta"></param>
        public void SendMailForRejectedReport(int key, Delta<DriveReport> delta)
        {
            var report = _driveReportRepository.AsQueryable().FirstOrDefault(r => r.Id == key);
            var recipient = "";
            if (report != null && !string.IsNullOrEmpty(report.Person.Mail))
            {
                recipient = report.Person.Mail;
            }
            else
            {
                _logger.LogForAdmin("Forsøg på at sende mail om afvist indberetning til " + report.Person.FullName + ", men der findes ingen emailadresse. " + report.Person.FullName + " har derfor ikke modtaget en mailadvisering");
                throw new Exception("Forsøg på at sende mail til person uden emailaddresse");
            }
            var comment = new object();
            if (delta.TryGetPropertyValue("Comment", out comment))
            {
                _mailService.SendMail(recipient, "Afvist indberetning",
                    $"Din indberetning, oprettet den {Utilities.FromUnixTime(report.CreatedDateTimestamp)}, er blevet afvist med kommentaren: \n \n" + comment + "\n \n Du har mulighed for at redigere den afviste indberetning i OS2indberetning under Mine indberetninger / Afviste, hvorefter den vil lægge sig under Afventer godkendelse - fanen igen.");
            }
        }

        /// <summary>
        /// Recalculates the deduction of 4 km from the persons reports driven on the date of the given unix time stamp. Does not recalculate rejected or invoiced reports.
        /// </summary>
        /// <param name="DriveDateTimestamp"></param>
        /// <param name="PersonId"></param>
        public void CalculateFourKmRuleForOtherReports(DriveReport report)
        {
            if (report.Status.Equals(ReportStatus.Rejected))
            {
                report.Distance += report.FourKmRuleDeducted;
                report.FourKmRuleDeducted = 0;
            }

            var reportsFromSameDayWithFourKmRule = _driveReportRepository.AsQueryable().Where(x => x.PersonId == report.PersonId
                    && x.Status != ReportStatus.Rejected
                    && x.Status != ReportStatus.Invoiced
                    && x.FourKmRule)
                    .OrderBy(x => x.DriveDateTimestamp).ToList();

            foreach (var r in reportsFromSameDayWithFourKmRule)
            {
                if (_calculator.AreReportsDrivenOnSameDay(report.DriveDateTimestamp, r.DriveDateTimestamp))
                {
                    _calculator.CalculateFourKmRuleForReport(r);
                }
            }

            _driveReportRepository.Save();
        }

        private void SixtyDayRuleCheck(DriveReport report)
        {
            if (report.SixtyDaysRule)
            {
                _logger.LogForAdmin($"Brugeren {report.Person.FullName} har angivet at være omfattet af 60-dages reglen");

                SendSixtyDaysRuleNotifications(report);
            }
        }

        private void SendSixtyDaysRuleNotifications(DriveReport report)
        {
            // Send mails to admins
            _mailService.SendMailToAdmins($"{report.Person.FullName} har angivet brug af 60-dages reglen", $"Brugeren {report.Person.FirstName} {report.Person.LastName} med medarbejdernummer {report.Employment.EmploymentId} har angivet at være omfattet af 60-dages reglen");

            // Send mail to leader.
            foreach (var leader in report.ResponsibleLeaders)
            {
                if (leader.RecieveMail && !string.IsNullOrEmpty(leader.Mail))
                {
                    _mailService.SendMail(leader.Mail, $"{report.Person.FullName} har angivet brug af 60-dages reglen", $"Brugeren {report.Person.FirstName} {report.Person.LastName} med medarbejdernummer {report.Employment.EmploymentId} har angivet at være omfattet af 60-dages reglen");
                }
            }
        }

        /// <summary>
        /// Gets the ResponsibleLeader for driveReport
        /// </summary>
        /// <param name="driveReport"></param>
        /// <returns>DriveReport with ResponsibleLeader attached</returns>
        public List<Person> GetResponsibleLeadersForReport(DriveReport driveReport)
        {
            var responsibleLeaders = new List<Person>();
            var currentDateTimestamp = Utilities.ToUnixTime(DateTime.Now);

            // Entity Framework doesnt always return child objects
            // https://stackoverflow.com/questions/29272581/why-ef-navigation-property-return-null

            // Fix for bug that sometimes happens when drivereport is from app, where personid is set, but person is not.
            var personId = _employmentRepository.AsQueryable().First(x => x.PersonId == driveReport.PersonId).PersonId;

            // Fix for bug that sometimes happens when drivereport is from app, where personid is set, but person is not.
            var empl = _employmentRepository.AsQueryable().First(x => x.Id == driveReport.EmploymentId);

            //Fetch personal approver for the person (Person and Leader of the substitute is the same)
            var personalApprovers =
                _substituteRepository.AsQueryable()
                    .Where(
                        s =>
                            s.PersonId != s.LeaderId && s.PersonId == personId &&
                            s.StartDateTimestamp < currentDateTimestamp && s.EndDateTimestamp > currentDateTimestamp).ToList();
            if (personalApprovers != null)
            {
                foreach (var substitute in personalApprovers)
                {
                    // There must be only one responsible leaders
                    // Add the personal approver to the list and return the collection
                    responsibleLeaders.Add(substitute.Sub);
                    return responsibleLeaders;
                }
            }

            //Find an org unit where the person is not the leader, and then find the leader of that org unit to attach to the drive report
            var orgUnit = _orgUnitRepository.AsQueryable().SingleOrDefault(o => o.Id == empl.OrgUnitId);
            if (orgUnit == null)
            {
                return responsibleLeaders;
            }

            var leaderOfOrgUnit = _employmentRepository.AsQueryable().FirstOrDefault(e => e.OrgUnit.Id == orgUnit.Id && e.IsLeader && e.StartDateTimestamp < currentDateTimestamp && (e.EndDateTimestamp > currentDateTimestamp || e.EndDateTimestamp == 0));

            var currentTimestamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

            // If the municipality uses SD/IDM instead of KMD/SOFD, the level property is not used, and we need to look at the parent instead og level.
            if (_customSettings.SdIsEnabled)
            {
                while ((leaderOfOrgUnit == null && orgUnit != null && orgUnit.Parent != null) ||
                    (leaderOfOrgUnit != null && leaderOfOrgUnit.PersonId == personId))
                {
                    leaderOfOrgUnit = _employmentRepository
                        .AsQueryable()
                        .FirstOrDefault(e =>
                            e.OrgUnit.Id == orgUnit.ParentId &&
                            e.IsLeader &&
                            e.StartDateTimestamp < currentDateTimestamp &&
                            (e.EndDateTimestamp == 0 || e.EndDateTimestamp > currentDateTimestamp));

                    orgUnit = orgUnit.Parent;
                }
            }
            else
            {
                while ((leaderOfOrgUnit == null && orgUnit != null && orgUnit.Level > 0) ||
                    (leaderOfOrgUnit != null && leaderOfOrgUnit.PersonId == personId))
                {
                    leaderOfOrgUnit = _employmentRepository
                        .AsQueryable()
                        .FirstOrDefault(e =>
                            e.OrgUnit.Id == orgUnit.ParentId &&
                            e.IsLeader &&
                            e.StartDateTimestamp < currentDateTimestamp &&
                            (e.EndDateTimestamp == 0 || e.EndDateTimestamp > currentDateTimestamp));

                    orgUnit = orgUnit.Parent;
                }
            }

            if (orgUnit == null || leaderOfOrgUnit == null)
            {
                return responsibleLeaders;
            }

            var leader = leaderOfOrgUnit.Person;
            var orgToCheck = empl.OrgUnit;

            var leaderOverrulingSubstitutes = _substituteRepository
                .AsQueryable()
                .Where(
                    s =>
                        s.PersonId == s.LeaderId &&
                        s.PersonId == leader.Id &&
                        s.StartDateTimestamp <= currentDateTimestamp && s.EndDateTimestamp >= currentDateTimestamp &&
                        s.TakesOverOriginalLeaderReports &&
                        s.PersonId != personId
                )
                .ToList();

            if (!leaderOverrulingSubstitutes.Any() && !responsibleLeaders.Contains(leader))
            {
                responsibleLeaders.Add(leaderOfOrgUnit.Person);
            }

            // Recursively look for substitutes in child orgs, up to the org of the actual leader.
            // Say the actual leader is leader of orgunit 1 with children 2 and 3. Child 2 has another child 4.
            // A report comes in for orgUnit 4. Check if leader has a substitute for that org.
            // If not then check if leader has a substitute for org 2.
            // If not then return the actual leader.
            List<Substitute> subs = null;
            var loopHasFinished = false;
            while (!loopHasFinished)
            {
                subs = _substituteRepository.AsQueryable().Where(s => s.OrgUnitId == orgToCheck.Id && s.PersonId == leader.Id && s.StartDateTimestamp <= currentDateTimestamp && s.EndDateTimestamp >= currentDateTimestamp && s.PersonId.Equals(s.LeaderId)).ToList();
                if (subs.Count > 0)
                {
                    foreach (var sub in subs)
                    {
                        if (sub.Sub == null)
                        {
                            // This is a hack fix for a weird bug that happens, where sometimes the Sub navigation property on a Substitute is null, even though the SubId is not.
                            sub.Sub = _employmentRepository.AsQueryable().FirstOrDefault(x => x.PersonId == sub.SubId).Person;
                        }
                        if (!responsibleLeaders.Contains(sub.Sub))
                            responsibleLeaders.Add(sub.Sub);
                    }
                    loopHasFinished = true;
                }
                else
                {
                    orgToCheck = orgToCheck.Parent;
                    if (orgToCheck == null || orgToCheck.Id == orgUnit.Parent?.Id)
                    {
                        loopHasFinished = true;
                    }
                }
            }

            return responsibleLeaders; // sub != null ? sub.Sub : leaderOfOrgUnit.Person;
        }

        public Person GetActualLeaderForReport(DriveReport driveReport)
        {
            var currentDateTimestamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

            // Fix for bug that sometimes happens when drivereport is from app, where personid is set, but person is not.
            var person = _employmentRepository.AsQueryable().First(x => x.PersonId == driveReport.PersonId).Person;

            // Fix for bug that sometimes happens when drivereport is from app, where personid is set, but person is not.
            var empl = _employmentRepository.AsQueryable().First(x => x.Id == driveReport.EmploymentId);

            //Find an org unit where the person is not the leader, and then find the leader of that org unit to attach to the drive report
            var orgUnit = _orgUnitRepository.AsQueryable().SingleOrDefault(o => o.Id == empl.OrgUnitId);

            if (orgUnit == null)
            {
                _logger.Error($"{this.GetType().Name}, GetActualLeaderForReport(), Orgunit with id: {empl.OrgUnitId} not found. Report ID: {driveReport.Id}");
                return null;
            }

            var leaderOfOrgUnit =
                _employmentRepository.AsQueryable().FirstOrDefault(e => e.OrgUnit.Id == orgUnit.Id && e.IsLeader && e.StartDateTimestamp < currentDateTimestamp && (e.EndDateTimestamp > currentDateTimestamp || e.EndDateTimestamp == 0));

            var currentTimestamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

            if (orgUnit.Parent == null)
            {
                if (leaderOfOrgUnit == null)
                {
                    return null;
                }
                return leaderOfOrgUnit.Person;
            }

            // If the municipality uses SD/IDM instead of KMD/SOFD, the level property is not used, and we need to look at the parent instead og level.
            if (_customSettings.SdIsEnabled)
            {
                while ((leaderOfOrgUnit == null && orgUnit.Parent != null) || (leaderOfOrgUnit != null && leaderOfOrgUnit.PersonId == person.Id))
                {
                    leaderOfOrgUnit = _employmentRepository.AsQueryable().FirstOrDefault(e => e.OrgUnit.Id == orgUnit.ParentId && e.IsLeader &&
                                                                                                e.StartDateTimestamp < currentTimestamp &&
                                                                                                (e.EndDateTimestamp == 0 || e.EndDateTimestamp > currentTimestamp));
                    orgUnit = orgUnit.Parent;
                }
            }
            else
            {
                while ((leaderOfOrgUnit == null && orgUnit.Level > 0) || (leaderOfOrgUnit != null && leaderOfOrgUnit.PersonId == person.Id))
                {
                    leaderOfOrgUnit = _employmentRepository.AsQueryable().FirstOrDefault(e => e.OrgUnit.Id == orgUnit.ParentId && e.IsLeader &&
                                                                                                e.StartDateTimestamp < currentTimestamp &&
                                                                                                (e.EndDateTimestamp == 0 || e.EndDateTimestamp > currentTimestamp));
                    orgUnit = orgUnit.Parent;
                }
            }


            if (orgUnit == null)
            {
                _logger.Error($"{this.GetType().Name}, GetActualLeaderForReport(), Orgunit with id: {empl.OrgUnitId} not found. Report ID: {driveReport.Id}");
                return null;
            }
            if (leaderOfOrgUnit == null)
            {
                // This statement will be hit when all orgunits up to (not including) level 0 have been checked for a leader. 
                // If no actual leader has been found then return the reponsibleleader.
                // This will happen when members of orgunit 0 try to create a report, as orgunit 0 has no leaders and they are all handled by a substitute.
                var leaders = GetResponsibleLeadersForReport(driveReport);
                if (leaders != null && leaders.Count > 0)
                    return GetResponsibleLeadersForReport(driveReport).FirstOrDefault();
                else
                    _logger.Error($"{this.GetType().Name}, GetActualLeaderForReport(), No leaders found. Report ID: {driveReport.Id}");
                return null;
            }

            return leaderOfOrgUnit.Person;
        }

        /// <summary>
        /// Sends an email to the owner of and person responsible for a report that has been edited or rejected by an admin.
        /// </summary>
        /// <param name="report">The edited report</param>
        /// <param name="emailText">The message to be sent to the owner and responsible leader</param>
        /// <param name="admin">The admin rejecting or editing</param>
        /// <param name="action">A string included in the email. Should be "afvist" or "redigeret"</param>
        public void SendMailToUserAndApproverOfEditedReport(DriveReport report, string emailText, Person admin, string action)
        {
            var mailContent = "Hej," + Environment.NewLine + Environment.NewLine +
            "Jeg, " + admin.FullName + ", har pr. dags dato " + action + " den følgende godkendte kørselsindberetning:" + Environment.NewLine + Environment.NewLine;

            mailContent += "Formål: " + report.Purpose + Environment.NewLine;

            if (report.KilometerAllowance != KilometerAllowance.Read)
            {
                mailContent += "Startadresse: " + report.DriveReportPoints.ElementAt(0).ToString() + Environment.NewLine
                + "Slutadresse: " + report.DriveReportPoints.Last().ToString() + Environment.NewLine;
            }

            mailContent += "Afstand: " + report.Distance.ToString().Replace(".", ",") + Environment.NewLine
            + "Kørselsdato: " + Utilities.FromUnixTime(report.DriveDateTimestamp) + Environment.NewLine + Environment.NewLine
            + "Hvis du mener at dette er en fejl, så kontakt mig da venligst på " + admin.Mail + Environment.NewLine
            + "Med venlig hilsen " + admin.FullName + Environment.NewLine + Environment.NewLine
            + "Besked fra administrator: " + Environment.NewLine + emailText;

            _mailService.SendMail(report.Person.Mail, "En administrator har ændret i din indberetning.", mailContent);

            _mailService.SendMail(report.ApprovedBy.Mail, "En administrator har ændret i en indberetning du har godkendt.", mailContent);
        }
    }
}
