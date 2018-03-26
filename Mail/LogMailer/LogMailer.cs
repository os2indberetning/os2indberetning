using System;
using System.Collections.Generic;
using System.Configuration;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Core.ApplicationServices.MailerService.Interface;
using Core.ApplicationServices.Logger;
using Core.DomainServices.Interfaces;

namespace Mail.LogMailer
{
    public class LogMailer : ILogMailer
    {
        private readonly ILogParser _logParser;
        private readonly ILogReader _logReader;
        private readonly IMailService _mailService;
        private readonly ILogger _logger;
        private readonly ICustomSettings _customSettings;

        public LogMailer(ILogParser logParser, ILogReader logReader, IMailService mailService, ILogger logger, ICustomSettings customSettings)
        {
            _logParser = logParser;
            _logReader = logReader;
            _mailService = mailService;
            _logger = logger;
            _customSettings = customSettings;
        }

        public void Send()
        {

            var configvalue = _customSettings.DailyErrorLogMail;

            configvalue = Regex.Replace(configvalue, @"\s+", "");

            var receivers = configvalue.Split(',');

            var webLines = new List<string>();
            var dbupdaterLines = new List<string>();
            var dmzLines = new List<string>();
            var mailLines = new List<string>();

            try
            {
                webLines = _logReader.Read("C:\\logs\\os2eindberetning\\admin\\web.log");
                dbupdaterLines = _logReader.Read("C:\\logs\\os2eindberetning\\admin\\dbupdater.log");
                dmzLines = _logReader.Read("C:\\logs\\os2eindberetning\\admin\\dmz.log");
                mailLines = _logReader.Read("C:\\logs\\os2eindberetning\\admin\\mail.log");
            }
            catch (Exception ex)
            {
                _logger.Error($"{GetType().Name}, Send(), Error when trying to read from an admin log file", ex);
                throw ex;
            }

            var webMessage = String.Join(Environment.NewLine, _logParser.Messages(webLines, DateTime.Now.AddDays(-1)));
            var dbupdaterMessage = String.Join(Environment.NewLine, _logParser.Messages(dbupdaterLines, DateTime.Now.AddDays(-1)));
            var dmzMessage = String.Join(Environment.NewLine, _logParser.Messages(dmzLines, DateTime.Now.AddDays(-1)));
            var mailMessage = String.Join(Environment.NewLine, _logParser.Messages(mailLines, DateTime.Now.AddDays(-1)));

            var newLine = System.Environment.NewLine;

            var result = "";

            // Only add each header if there are log messages in that category.
            if (webMessage.Any())
            {
                result += "Web:" + newLine + newLine + webMessage + newLine + newLine;
            }
            if (dbupdaterMessage.Any())
            {
                result += "DBUpdater:" + newLine + newLine + webMessage + newLine + newLine;
            }
            if (dmzMessage.Any())
            {
                result += "DMZ: " + newLine + newLine + dmzMessage + newLine + newLine;
            }
            if (mailMessage.Any())
            {
                result += "Mail: " + newLine + newLine + mailMessage;
            }

            if (result == "")
            {
                result = "Ingen fejl registreret";
            }

            foreach (var receiver in receivers)
            {
                _mailService.SendMail(receiver, "Log", result);
            }
        }
    }
}
