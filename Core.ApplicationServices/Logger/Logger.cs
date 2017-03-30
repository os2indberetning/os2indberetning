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

        public Logger()
        {
            // Filename for each log is configured in the Log4Net.config file in each project.
            _devLog = LogManager.GetLogger("Logger");
            _adminLog = LogManager.GetLogger("adminLog");
        }

        //public void Log(string msg, string fileName)
        //{
        //    _devLog.Info(msg);
        //}

        //public void Log(string msg, string fileName, Exception ex)
        //{
        //    _devLog.Error(msg, ex);
        //}

        //public void Log(string msg, string fileName, Exception ex, int level)
        //{
        //    var message = "[Niveau " + level + "] - " + msg;
        //    switch (level)
        //    {
        //        case 1: _devLog.Error(message, ex); break;
        //        case 2: _devLog.Warn(message, ex); break;
        //        default:
        //            _devLog.Info(message, ex); break;
        //    }
        //}

        //public void Log(string msg, string fileName, int level)
        //{
        //    var message = "[Niveau " + level + "] - " + msg;
        //    switch (level)
        //    {
        //        case 1: _devLog.Error(message); break;
        //        case 2: _devLog.Warn(message); break;
        //        default:
        //            _devLog.Info(message); break;
        //    }
        //}

        public void Debug(string message)
        {
            _devLog.Debug(message);
        }

        public void Error(string message, Exception exception = null)
        {
            _devLog.Error(message, exception);
        }

        public void InfoAdmin(string msg)
        {
            _adminLog.Info(msg);
        }

        public void ErrorAdmin(string msg)
        {
            _adminLog.Error(msg);
        }
    }
}