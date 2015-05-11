﻿
using System;
using System.Collections.Generic;
using System.Linq;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;

namespace Core.ApplicationServices
{
    public class SubstituteService : ISubstituteService
    {
        public void ScrubCprFromPersons(IQueryable<Substitute> subs)
        {
            foreach (var sub in subs)
            {
                sub.Sub.CprNumber = "";
                sub.Leader.CprNumber = "";


                sub.Person.CprNumber = "";

            }
        }

        public void AddFullName(IQueryable<Substitute> substitutes)
        {
            var subs = substitutes.ToList();

            foreach (var sub in subs)
            {
                sub.Sub.FullName = sub.Sub.FirstName;

                if (!string.IsNullOrEmpty(sub.Sub.MiddleName))
                {
                    sub.Sub.FullName += " " + sub.Sub.MiddleName;
                }

                sub.Sub.FullName += " " + sub.Sub.LastName;

                sub.Sub.FullName += " [" + sub.Sub.Initials + "]";


                sub.Leader.FullName = sub.Leader.FirstName;

                if (!string.IsNullOrEmpty(sub.Leader.MiddleName))
                {
                    sub.Leader.FullName += " " + sub.Leader.MiddleName;
                }

                sub.Leader.FullName += " " + sub.Leader.LastName;
                sub.Leader.FullName += " [" + sub.Leader.Initials + "]";


                sub.Person.FullName = sub.Person.FirstName;

                if (!string.IsNullOrEmpty(sub.Person.MiddleName))
                {
                    sub.Person.FullName += " " + sub.Person.MiddleName;
                }

                sub.Person.FullName += " " + sub.Person.LastName;
                sub.Person.FullName += " [" + sub.Person.Initials + "]";

            }

            substitutes = subs.AsQueryable();
        }

        public long GetStartOfDayTimestamp(long timestamp)
        {
            var dateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0);
            dateTime = dateTime.AddSeconds(timestamp).ToLocalTime();
            dateTime = new DateTime(dateTime.Year,dateTime.Month,dateTime.Day,0,0,0,0).ToUniversalTime();
            var unixTimestamp = (Int32)(dateTime.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
            return unixTimestamp;
        }

        public long GetEndOfDayTimestamp(long timestamp)
        {
            var dateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0);
            dateTime = dateTime.AddSeconds(timestamp).Date.AddDays(1).AddTicks(-1);
            dateTime = new DateTime(dateTime.Year, dateTime.Month, dateTime.Day, 23, 59, 59, 999).ToUniversalTime();
            var unixTimestamp = (Int32)(dateTime.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
            return unixTimestamp;
        }
    }
}
