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
using Core.DomainModel;
using Core.DomainServices;
using Mail.LogMailer;
using Ninject;

namespace Mail
{
    public class ConsoleMailerService
    {
        private IMailService _mailService;
        private IGenericRepository<MailNotificationSchedule> _repo;
        private ILogger _logger;

        public ConsoleMailerService(IMailService mailService, IGenericRepository<MailNotificationSchedule> repo, ILogger logger)
        {
            _mailService = mailService;
            _repo = repo;
            _logger = logger;
        }

        /// <summary>
        /// Checks if there are any mail notifications scheduled for now. If there is mails will be sent. Otherwise nothing happens.
        /// </summary>
        public void RunMailService()
        {

            var logMailer = new LogMailer.LogMailer(new LogParserRegex(), new LogReader(), _mailService, _logger);
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

            var startOfDay = ToUnixTime(new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, 00, 00, 00));
            var endOfDay = ToUnixTime(new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, 23, 59, 59));
        
            var notifications = _repo.AsQueryable().Where(r => r.DateTimestamp >= startOfDay && r.DateTimestamp <= endOfDay && !r.Notified);

            if (notifications.Any())
            {
                Console.WriteLine("Forsøger at sende emails.");
                foreach (var notification in notifications.ToList())
                {
                    if (notification.Repeat)
                    {
                        var newDateTime = ToUnixTime(FromUnixTime(notification.DateTimestamp).AddMonths(1));
                        _repo.Insert(new MailNotificationSchedule()
                        {
                            DateTimestamp = newDateTime,
                            Notified = false,
                            Repeat = true
                        });
                    }
                    notification.Notified = true;

                    AttemptSendMails(_mailService,FromUnixTime(notification.PayRoleTimestamp), 2);
                }

                _repo.Save();
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
        public void AttemptSendMails(IMailService service, DateTime payRoleDateTime, int timesToAttempt)
        {
            if (timesToAttempt > 0)
            {
                try
                {
                    service.SendMails(payRoleDateTime);
                }
                catch (System.Net.Mail.SmtpException e)
                {
                    Console.WriteLine("Kunne ikke oprette forbindelse til SMTP-Serveren. Forsøger igen...");
                    _logger.LogForAdmin("Kunne ikke forbinde til SMTP-server. Mails kan ikke sendes.");
                    _logger.Error($"{GetType().Name}, AttemptSendMails(), Could not connect to SMTP server, mails could not be send", e);
                    AttemptSendMails(service, payRoleDateTime, timesToAttempt - 1);
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

        /// <summary>
        /// Converts DateTime to timestamp
        /// </summary>
        /// <param name="date">DateTime to convert</param>
        /// <returns>long timestamp</returns>
        public long ToUnixTime(DateTime date)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            return Convert.ToInt64((date - epoch).TotalSeconds);
        }

        /// <summary>
        /// Converts timestamp to datetime
        /// </summary>
        /// <param name="unixTime">Timestamp to convert</param>
        /// <returns>DateTime</returns>
        public DateTime FromUnixTime(long unixTime)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            return epoch.AddSeconds(unixTime);
        }
    }
}
