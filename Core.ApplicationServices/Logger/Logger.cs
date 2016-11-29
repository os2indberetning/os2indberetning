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
        private ILog log;
        private ILog sdLog;

        public Logger()
        {
            log = LogManager.GetLogger("RollingFileAppender");
            sdLog = LogManager.GetLogger("SDLogger");
        }

        public void Log(string msg, string fileName)
        {
            if (fileName.Equals("SD"))
            {
                sdLog.Error(msg);
            }
            else
            {
                log.Error(msg);
            }
        }

        public void Log(string msg, string fileName, Exception ex)
        {
            if (fileName.Equals("SD"))
            {
                sdLog.Error(msg, ex);
            }
            else
            {
                log.Error(msg, ex); 
            }
        }

        public void Log(string msg, string fileName, Exception ex, int level)
        {
            var message = "[Niveau " + level + "] - " + msg;
            switch (level)
            {
                case 1: log.Error(message, ex); break;
                case 2: log.Warn(message, ex); break;
                default:
                    log.Info(message, ex); break;
            }
        }

        public void Log(string msg, string fileName, int level)
        {
            var message = "[Niveau " + level + "] - " + msg;
            switch (level)
            {
                case 1: log.Error(message); break;
                case 2: log.Warn(message); break;
                default:
                    log.Info(message); break;
            }
        }
    }

}