using System;
using System.Linq;
using ConsoleApplications.Test.Mailer;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Mail;
using NSubstitute;
using NUnit.Framework;
using Core.DomainServices;
using Core.DomainServices.Interfaces;
using System.Collections.Generic;

namespace ConsoleApplications.Test
{
    [TestFixture]
    public class MailerTests
    {
        private MailNotificationsRepoMock repoMock;
        private ILogger _logger;
        private ICustomSettings _customSettings;

        [SetUp]
        public void SetUp()
        {
            _logger = NSubstitute.Substitute.For<ILogger>();
            repoMock = new MailNotificationsRepoMock();
            _customSettings = new CustomSettings();
        }

        [Test]
        public void RunMailService_UnnotifiedNotifications_ShouldCall_SendMails_ButNotAddNewNotification()
        {
            var preLength = repoMock.AsQueryable().ToList().Count;
            var customText1 = "customText1";
            var customText2 = "";
            var datetimeNoMilisec = new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, DateTime.Now.Hour, DateTime.Now.Minute, DateTime.Now.Second);
            var mailSub = NSubstitute.Substitute.For<IMailService>();
            var notification1 = repoMock.noti1 = new MailNotificationSchedule()
            {
                Id = 1,
                DateTimestamp = Utilities.ToUnixTime(datetimeNoMilisec),
                FileGenerationSchedule = new FileGenerationSchedule { DateTimestamp = Utilities.ToUnixTime(datetimeNoMilisec) },
                CustomText = customText1
            };
            repoMock.noti2 = new MailNotificationSchedule()
            {
                Id = 2,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(1)),
                FileGenerationSchedule = new FileGenerationSchedule { DateTimestamp = Utilities.ToUnixTime(datetimeNoMilisec) },
                CustomText = customText2
            };
            repoMock.ReSeed();
            var uut = new ConsoleMailerService(mailSub, repoMock,_logger, _customSettings);
            uut.RunMailService();
            mailSub.Received().SendMails(datetimeNoMilisec, customText1);
            mailSub.DidNotReceive().SendMails(datetimeNoMilisec, customText2);
            Assert.AreEqual(preLength,repoMock.AsQueryable().ToList().Count);
        }

        [Test]
        public void RunMailService_2NotificationsWithRepeat_ShouldCall_SendMails()
        {
            var preLength = repoMock.AsQueryable().ToList().Count;
            var customText1 = "customText1";
            var customText2 = "";
            var datetimeNoMilisec = new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, DateTime.Now.Hour, DateTime.Now.Minute, DateTime.Now.Second);
            var mailSub = NSubstitute.Substitute.For<IMailService>();

            repoMock.noti1 = new MailNotificationSchedule()
            {
                Id = 1,
                DateTimestamp = Utilities.ToUnixTime(datetimeNoMilisec),
                FileGenerationSchedule = new FileGenerationSchedule { DateTimestamp = Utilities.ToUnixTime(datetimeNoMilisec) },
                CustomText = customText1
            };
            repoMock.noti2 = new MailNotificationSchedule()
            {
                Id = 2,
                DateTimestamp = Utilities.ToUnixTime(datetimeNoMilisec),
                FileGenerationSchedule = new FileGenerationSchedule { DateTimestamp = Utilities.ToUnixTime(datetimeNoMilisec) },
                CustomText = customText2
            };
            repoMock.ReSeed();
            var uut = new ConsoleMailerService(mailSub, repoMock, _logger, _customSettings);
            uut.RunMailService();
            mailSub.Received().SendMails(datetimeNoMilisec, customText1);
            mailSub.Received().SendMails(datetimeNoMilisec, customText2);
        }

        [Test]
        public void RunMailService_NoNotificationsForToday_ShouldNotCall_SendMails()
        {
            var mailSub = NSubstitute.Substitute.For<IMailService>();
            repoMock.noti1 = new MailNotificationSchedule()
            {
                Id = 1,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(1)),
                FileGenerationSchedule = new FileGenerationSchedule { DateTimestamp = Utilities.ToUnixTime(DateTime.Now) }
            };
            repoMock.noti2 = new MailNotificationSchedule()
            {
                Id = 2,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(2)),
                FileGenerationSchedule = new FileGenerationSchedule { DateTimestamp = Utilities.ToUnixTime(DateTime.Now) }
            };
            repoMock.ReSeed();
            var uut = new ConsoleMailerService(mailSub, repoMock, _logger, _customSettings);
            uut.RunMailService();
            mailSub.DidNotReceive().SendMails(new DateTime(), "");
        }
    }
}
