﻿using System;
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
        private ILog _auditLogDmz;
        private ILog _sdLog;

        public Logger()
        {
            // Filename for each log is configured in the Log4Net.config in each project.
            _devLog = LogManager.GetLogger("DefaultLog");
            _adminLog = LogManager.GetLogger("adminLog");
            try
            {
                _auditLog = LogManager.GetLogger("auditLog");
            }
            catch { }
            _auditLogDmz = LogManager.GetLogger("auditLogDMZ");
            _sdLog = LogManager.GetLogger("SdLog");
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
        
        public void AuditLog(string user, string userLocation, string controller, string action, string parameters)
        {
            _auditLog.Info(FormatAuditlog(DateTime.Now.ToString(), user, userLocation, controller, action, parameters));
        }

        public void AuditLogDMZ(string date, string user, string userLocation, string controller, string action, string parameters)
        {
            _auditLogDmz.Info(FormatAuditlog(date, user, userLocation, controller, action, parameters));
        }

        public void DebugSd(string message)
        {
            _sdLog.Info(message);
        }

        public void ErrorSd(string message, Exception exception = null)
        {
            _sdLog.Error(message, exception);
        }

        private string FormatAuditlog(string date, string user, string userLocation, string controller, string action, string parameters)
        {
            return $"Timestamp: {date} - User: {user ?? "not available"} - Location: {userLocation ?? "not available"} - Controller: {controller ?? "not available"} - Action: {action ?? "not available"} - Parameters: {parameters ?? "not available"}";
        }
    }
}