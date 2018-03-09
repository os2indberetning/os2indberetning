using System;
using System.Collections.Generic;
using Core.DomainModel;
using Presentation.Web.Test.Controllers;
using Core.DomainServices;

namespace ConsoleApplications.Test.Mailer
{
    public class MailNotificationsRepoMock : GenericRepositoryMock<MailNotificationSchedule>
    {
        public MailNotificationSchedule noti1 = new MailNotificationSchedule()
        {
            Id = 1,
            DateTimestamp = Utilities.ToUnixTime(DateTime.Now)
        };

        public MailNotificationSchedule noti2 = new MailNotificationSchedule()
        {
            Id = 2,
            DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(1))
        };

        protected override List<MailNotificationSchedule> Seed()
        {

            return new List<MailNotificationSchedule>
            {
                noti1,
                noti2,
            };
        }
    }
}
