using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.DomainServices
{
    public static class Utilities
    {

        /// <summary>
        /// Converts DateTime to unix timestamp
        /// </summary>
        /// <param name="date">DateTime to convert</param>
        /// <returns>long timestamp</returns>
        public static long ToUnixTime(DateTime date)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            return Convert.ToInt64((date - epoch).TotalSeconds);
        }

        /// <summary>
        /// Converts unix timestamp to datetime
        /// </summary>
        /// <param name="unixTime">Timestamp to convert</param>
        /// <returns>DateTime</returns>
        public static DateTime FromUnixTime(long unixTime)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            return epoch.AddSeconds(unixTime);
        }
    }
}
