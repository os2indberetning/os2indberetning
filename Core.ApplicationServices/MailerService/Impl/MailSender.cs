using System;
using System.Configuration;
using System.Net;
using System.Net.Mail;
using Core.ApplicationServices.MailerService.Interface;
using Ninject;
using Core.ApplicationServices.Logger;
using Core.DomainServices.Interfaces;

namespace Core.ApplicationServices.MailerService.Impl
{
    public class MailSender : IMailSender
    {
        private readonly SmtpClient _smtpClient;
        private readonly ILogger _logger;
        private readonly ICustomSettings _customSettings;

        public MailSender(ILogger logger, ICustomSettings customSettings)
        {
            _logger = logger;
            _customSettings = customSettings;
          
            try
            {
                int port;
                bool hasPortValue = int.TryParse(_customSettings.SMTPHostPort, out port);

                _smtpClient = new SmtpClient()
                {
                    Host = ConfigurationManager.AppSettings["PROTECTED_SMTP_HOST"],
                    
                    EnableSsl = false,
                    Credentials = new NetworkCredential()
                    {
                        UserName = _customSettings.SMTPUser,
                        Password = _customSettings.SMTPPassword
                    }
                };
               
                if (hasPortValue)
                {
                    _logger.Debug($"{this.GetType().Name}, tryParse on PROTECTED_SMTP_HOST_PORT. port = {port}");
                    _smtpClient.Port = port;
                }
            }
            catch (Exception e)
            {
                _logger.Error($"{this.GetType().Name}, Smtp client initialization falied, check values in CustomSettings.config", e);
                throw e;
            }
        }

        /// <summary>
        /// Sends an email
        /// </summary>
        /// <param name="to">Email address of recipient.</param>
        /// <param name="subject">Subject of the email.</param>
        /// <param name="body">Body of the email.</param>
        public void SendMail(string to, string subject, string body)
        {
            if (String.IsNullOrWhiteSpace(to))
            {
                return;
            }
            var msg = new MailMessage();
            msg.To.Add(to);
            msg.From = new MailAddress(_customSettings.MailFromAddress);
            msg.Body = body;
            msg.Subject = subject;
            try
            {
                _smtpClient.Send(msg);
            }
            catch (Exception e )
            {
                _logger.LogForAdmin($"Fejl under afsendelse af mail til {to}, med emnet: \"{subject}\". Mail er ikke afsendt.");
                _logger.Error($"{GetType().Name}, SendMail(), Error when sending mail to {to}, with subject: \"{subject}\". Mail has not been sent", e);
            }
        }
    }
}
