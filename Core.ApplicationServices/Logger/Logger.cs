using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using log4net;

namespace Core.ApplicationServices.Logger
{
    public class Logger : ILogger
    {
        private ILog _devLog;
        private ILog _adminLog;
        private ILog _auditLog;

        public Logger()
        {
            // Filename for each log is configured in the Log4Net.config in each project.
            _devLog = LogManager.GetLogger("Logger");
            _adminLog = LogManager.GetLogger("adminLog");
            try
            {
                _auditLog = LogManager.GetLogger("auditLog");
            }
            catch { }
        }

        public void Debug(string message)
        {
            _devLog.Debug(message);
        }

        public void Error(string message, Exception exception = null)
        {
            _devLog.Error(message, exception);
        }

        public void LogForAdmin(string msg)
        {
            _adminLog.Info(msg);
        }

        public void AuditLog(string msg)
        {
            _auditLog.Info(msg);
        }
    }
}