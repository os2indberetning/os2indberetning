using System;
using System.Collections.Generic;
using System.Configuration;
using System.Globalization;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using Core.ApplicationServices;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.MailerService.Impl;
using Core.ApplicationServices.MailerService.Interface;
using Core.ApplicationServices.Interfaces;

using Core.DomainModel;
using Core.DomainServices;
using Mail.LogMailer;
using Ninject;
using Core.DomainServices.Interfaces;

namespace Mail
{
    public class ConsoleMailerService
    {
        private IMailService _mailService;
        private ISubstituteService _substituteService;

        private IGenericRepository<MailNotificationSchedule> _repo;
        private ILogger _logger;
        private ICustomSettings _customSettings;

        public ConsoleMailerService(IMailService mailService, ISubstituteService subService, IGenericRepository<MailNotificationSchedule> repo, ILogger logger, ICustomSettings customSettings)
        {
            _mailService = mailService;
            _repo = repo;
            _logger = logger;
            _customSettings = customSettings;
            _substituteService = subService;
        }

        /// <summary>
        /// Checks if there are any mail notifications scheduled for now. If there is mails will be sent. Otherwise nothing happens.
        /// </summary>
        public void RunMailService()
        {
            // Update the responsible leaders for pending reports by removing the subs that are expired.
            _substituteService.UpdateResponsibleLeadersDaily();

            // Send notifications
            var logMailer = new LogMailer.LogMailer(new LogParserRegex(), new LogReader(), _mailService, _logger, _customSettings);
            try
            {
                logMailer.Send();
                _logger.Debug($"{this.GetType().Name}, RunMailerService(): logmails send succesfully to admin");
            }
            catch (Exception e)
            {
                Console.WriteLine("Kunne ikke sende daglig aktivitet i fejlloggen!");
                _logger.LogForAdmin("Fejl under afsendelse af daglig log aktivitet. Daglig aktivitet ikke udsendt.");
                _logger.Error($"{GetType().Name}, RunMailService(), Error when trying to send daily log mail to admin", e);
            }

            var startOfDay = Utilities.ToUnixTime(new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, 00, 00, 00));
            var endOfDay = Utilities.ToUnixTime(new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, 23, 59, 59));
        
            var notifications = _repo.AsQueryable().Where(r => r.DateTimestamp >= startOfDay && r.DateTimestamp <= endOfDay);

            if (notifications.Any())
            {
                Console.WriteLine("Forsøger at sende emails.");
                foreach (var notification in notifications.ToList())
                {
                    AttemptSendMails(_mailService, Utilities.FromUnixTime(notification.FileGenerationSchedule.DateTimestamp), notification.CustomText, 2);
                }
                
                _logger.Debug($"{this.GetType().Name}, RunMailerService(), Notification mails for leaders sending finished");
            }
            else
            {
                _logger.Debug($"{this.GetType().Name}, RunMailerService(): No notifications for leaders found for today");
                Console.WriteLine("Ingen email-adviseringer fundet! Programmet lukker om 3 sekunder.");
                Console.WriteLine(Environment.CurrentDirectory);
                Thread.Sleep(3000);
            }
        }



        /// <summary>
        /// Attempts to send mails to leaders with pending reports to be approved.
        /// </summary>
        /// <param name="service">IMailService to use for sending mails.</param>
        /// <param name="timesToAttempt">Number of times to attempt to send emails.</param>
        public void AttemptSendMails(IMailService service, DateTime payRoleDateTime, String customText, int timesToAttempt)
        {
            if (timesToAttempt > 0)
            {
                try
                {
                    service.SendMails(payRoleDateTime, customText);
                }
                catch (System.Net.Mail.SmtpException e)
                {
                    Console.WriteLine("Kunne ikke oprette forbindelse til SMTP-Serveren. Forsøger igen...");
                    _logger.LogForAdmin("Kunne ikke forbinde til SMTP-server. Mails kan ikke sendes.");
                    _logger.Error($"{GetType().Name}, AttemptSendMails(), Could not connect to SMTP server, mails could not be send", e);
                    AttemptSendMails(service, payRoleDateTime, customText, timesToAttempt - 1);
                }
            }
            else
            {
                Console.WriteLine("Alle forsøg fejlede. Programmet lukker om 3 sekunder.");
                _logger.LogForAdmin("Alle forsøg på at sende mailadviseringer til ledere fejlede");
                _logger.Error($"{GetType().Name}, AttemptSendMails(), All attempts failed");
                Thread.Sleep(3000);

            }
        }
    }
}
