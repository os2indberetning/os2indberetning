﻿using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Runtime.InteropServices;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Core.ApplicationServices.Logger;

namespace Core.ApplicationServices.MailerService.Impl
{
    public class MailService : IMailService
    {
        private readonly IGenericRepository<Report> _reportRepo;
        private readonly IMailSender _mailSender;
        private readonly ILogger _logger;

        public MailService(IGenericRepository<Report> reportRepo, IMailSender mailSender,  ILogger logger)
        {
            _reportRepo = reportRepo;
            _mailSender = mailSender;
            _logger = logger;
        }

        /// <summary>
        /// Sends an email to all leaders with pending reports to be approved.
        /// </summary>
        public void SendMails(DateTime payRoleDateTime)
        {
            var reports = GetLeadersWithPendingReportsMails();

            var mailBody = ConfigurationManager.AppSettings["PROTECTED_MAIL_BODY"];
            if (string.IsNullOrEmpty(mailBody))
            {
                _logger.Log($"{this.GetType().Name}, SendMails(): Mail body is null or empty, check value in CustomSettings.config", "mail", 3);
            }
            var driveBody = ConfigurationManager.AppSettings["PROTECTED_MAIL_BODY_DRIVE"];
            driveBody = driveBody.Replace("####", payRoleDateTime.ToString("dd-MM-yyyy"));

            var mailSubjectDrive = ConfigurationManager.AppSettings["PROTECTED_MAIL_SUBJECT_DRIVE"];
            var mailSubjectVacation = ConfigurationManager.AppSettings["PROTECTED_MAIL_SUBJECT_VACATION"];
            if (string.IsNullOrEmpty(mailSubjectDrive))
            {
                _logger.Log($"{this.GetType().Name}, SendMails(): Mail subject for drive is null or empty, check value in CustomSettings.config", "mail", 3);
            }
            if (string.IsNullOrEmpty(mailSubjectVacation))
            {
                _logger.Log($"{this.GetType().Name}, SendMails(): Mail subject for vacation is null or empty, check value in CustomSettings.config", "mail", 3);
            }

            foreach (var mailAddress in mailAddresses)
            {
                switch (report.ReportType)
                {
                    case ReportType.Drive:
                        _mailSender.SendMail(report.ResponsibleLeader.Mail, mailSubjectDrive, driveBody);
                        break;
                    case ReportType.Vacation:
                        _mailSender.SendMail(report.ResponsibleLeader.Mail, , ConfigurationManager.AppSettings["PROTECTED_MAIL_BODY_VACATION"]);
                        break;
                    default:
                        _logger.Log("Kunne ikke finde typen af rapport: " + report.Id, "web");
                        break;
                }

            }
        }

        /// <summary>
        /// Gets the email address of all leaders that have pending reports to be approved.
        /// </summary>
        /// <returns>List of email addresses.</returns>
        public IEnumerable<Report> GetLeadersWithPendingReportsMails()
        {
            var reports = _reportRepo.AsQueryable().Where(r => r.Status == ReportStatus.Pending).ToList();

            var reportsWithNoLeader = reports.Where(report => report.ResponsibleLeader == null);

            foreach (var report in reportsWithNoLeader)
            {
                _logger.Log($"{this.GetType().Name}, GetLeadersWithPendingReportsMails(): {report.Person.FullName}s indberetning har ingen leder. Indberetningen kan derfor ikke godkendes.", "web", 2);
            }

            return reports.Where(report => report.ResponsibleLeaderId != null && !string.IsNullOrEmpty(report.ResponsibleLeader.Mail) && report.ResponsibleLeader.RecieveMail);
        }
    }
}
