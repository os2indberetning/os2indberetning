using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.ApplicationServices.Logger
{
    public interface ILogger
    {
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
        /// Info logging for Administrators. This will be included in the daily log e-mail.
        /// </summary>
        /// <param name="message"></param>
        void LogForAdmin(string message);

        void AuditLog(string user, string userLocation, string controller, string action, string parameters);

        void AuditLogDMZ(string user, string userLocation, string controller, string action, string parameters);

    }
}
