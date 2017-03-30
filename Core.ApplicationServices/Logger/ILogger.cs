using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.ApplicationServices.Logger
{
    public interface ILogger
    {
        //void Log(string msg, string fileName);
        //void Log(string msg, string fileName, Exception ex);

        //void Log(string msg, string fileName, Exception ex, int level);
        //void Log(string msg, string fileName, int level);

        /// <summary>
        /// Debug logging for developers.
        /// </summary>
        /// <param name="msg"></param>
        void Debug(string msg);

        /// <summary>
        /// Error logging for developers.
        /// </summary>
        /// <param name="message"></param>
        /// <param name="exception"></param>
        void Error(string message, Exception exception = null);

        /// <summary>
        /// Info logging for Administrators. This will be included in the daily log mail.
        /// </summary>
        /// <param name="message"></param>
        void InfoAdmin(string message);

        /// <summary>
        /// Error logging for Administrators. This will be included in the daily log mail.
        /// </summary>
        /// <param name="message"></param>
        /// <param name="exception"></param>
        void ErrorAdmin(string message);
    }
}
