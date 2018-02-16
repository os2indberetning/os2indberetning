using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data.Entity;
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
        private readonly IGenericRepository<DriveReport> _driveRepo;
        private readonly IGenericRepository<Substitute> _subRepo;
        private readonly IGenericRepository<Person> _personRepo;
        private readonly IMailSender _mailSender;
        private readonly ILogger _logger;

        public MailService(IGenericRepository<DriveReport> driveRepo, IGenericRepository<Substitute> subRepo, IGenericRepository<Person> personRepo, IMailSender mailSender, ILogger logger)
        {
            _driveRepo = driveRepo;
            _subRepo = subRepo;
            _personRepo = personRepo;
            _mailSender = mailSender;
            _logger = logger;
        }

        /// <summary>
        /// Sends an email to all leaders with pending reports to be approved.
        /// </summary>
        public void SendMails(DateTime payRoleDateTime)
        {
            var mailAddresses = GetLeadersWithPendingReportsMails();

            var mailBody = ConfigurationManager.AppSettings["PROTECTED_MAIL_BODY"];
            if (string.IsNullOrEmpty(mailBody))
            {
                _logger.Debug($"{this.GetType().Name}, SendMails(): Mail body is null or empty, check value in CustomSettings.config");
            }
            mailBody = mailBody.Replace("####", payRoleDateTime.ToString("dd-MM-yyyy"));

            var mailSubject = ConfigurationManager.AppSettings["PROTECTED_MAIL_SUBJECT"];
            if (string.IsNullOrEmpty(mailSubject))
            {
                _logger.Debug($"{this.GetType().Name}, SendMails(): Mail subject is null or empty, check value in CustomSettings.config");
            }

            foreach (var mailAddress in mailAddresses)
            {
                _mailSender.SendMail(mailAddress, ConfigurationManager.AppSettings["PROTECTED_MAIL_SUBJECT"], mailBody);
            }
        }

        /// <summary>
        /// Gets the email address of all leaders that have pending reports to be approved.
        /// </summary>
        /// <returns>List of email addresses.</returns>
        public IEnumerable<string> GetLeadersWithPendingReportsMails()
        {
            var approverEmails = new List<String>();

            var reports = _driveRepo.AsQueryable().Where(r => r.Status == ReportStatus.Pending).ToList();

            var reportsWithNoLeader = reports.Where(driveReport => driveReport.ResponsibleLeaders.Count == 0);

            foreach (var report in reportsWithNoLeader)
            {
                _logger.LogForAdmin($"{report.Person.FullName}s indberetning har ingen leder. Indberetningen kan derfor ikke godkendes.");
                _logger.Error($"{this.GetType().Name}, GetLeadersWithPendingReportsMails(): {report.Person.FullName}s indberetning har ingen leder. Indberetningen kan derfor ikke godkendes.");
            }

            foreach (var driveReport in reports)
            {
                approverEmails.AddRange(driveReport.ResponsibleLeaders.Where(p => !string.IsNullOrEmpty(p.Mail) && p.RecieveMail).Select(p => p.Mail));
            }

            return approverEmails;
        }

        public void SendMailToAdmins(string subject, string text)
        {
            var adminEmailAdresses = _personRepo.AsQueryable().Where(p => p.IsActive && p.IsAdmin && p.AdminRecieveMail && !string.IsNullOrEmpty(p.Mail)).Select(p => p.Mail);

            foreach (var emailAddress in adminEmailAdresses)
            {
                _mailSender.SendMail(emailAddress, subject, text);
            } 
        }

        public void SendMail(string toAddress, string subject, string text)
        {
            if (!string.IsNullOrEmpty(toAddress))
            {
                _mailSender.SendMail(toAddress, subject, text); 
            }
            else
            {
                throw new ArgumentException("Receiving emailaddress can not be null or empty", "toAddress");
            }
        }
    }
}
