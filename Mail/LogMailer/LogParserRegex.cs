using Core.ApplicationServices;
using Core.ApplicationServices.Logger;
using Ninject;
using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Text.RegularExpressions;

namespace Mail.LogMailer
{
    public class LogParserRegex : ILogParser
    {
        private ILogger _logger = NinjectWebKernel.CreateKernel().Get<ILogger>();
        public List<string> Messages(List<string> log, DateTime fromDate)
        {
            var messages = new List<string>();

            foreach (var line in log)
            {
                try
                {
                    if (!string.IsNullOrWhiteSpace(line))
                    {
                        // Pattern will match the following: "2017 - 03 - 30 13:26:29,477 INFO: Any message goes here." where INFO can be any level of severity, like DEBUG or WARN as well.
                        string pattern = @"(\d{4}\-\d{2}\-\d{2}\s\d{2}\:\d{2}\:\d{2}),\d{3}\s(\w+)\s\:\s(.*)";
                        Match match = Regex.Match(line, pattern, RegexOptions.IgnoreCase);

                        if (match.Success)
                        {
                            // Groups[0] is the entire match. First actual group is index 1.
                            var date = DateTime.ParseExact(match.Groups[1].Value, "yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture);
                            var severity = match.Groups[2].Value;
                            var message = match.Groups[3].Value;

                            if (date < fromDate) break;
                            messages.Add(severity + ": " + message);
                        }
                    }
                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().Name}, Messages(), Error when parsing log entry. Line in log= {line} ", e);
                    Console.WriteLine(e.Message);
                }
            }
            return messages;
        }
    }
}
