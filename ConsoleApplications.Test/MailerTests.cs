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
            var mailSub = NSubstitute.Substitute.For<IMailService>();
            repoMock.noti1 = new MailNotificationSchedule()
            {
                Id = 1,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Notified = false,
                Repeat = false
            };
            repoMock.noti2 = new MailNotificationSchedule()
            {
                Id = 2,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(1)),
                Notified = false,
                Repeat = false
            };
            repoMock.ReSeed();
            var uut = new ConsoleMailerService(mailSub, repoMock,_logger, _customSettings);
            uut.RunMailService();
            mailSub.ReceivedWithAnyArgs().SendMails(new DateTime());
            Assert.AreEqual(preLength,repoMock.AsQueryable().ToList().Count);
        }

        [Test]
        public void RunMailService_2NotificationsWithRepeat_ShouldCall_SendMails_AndAdd2NewNotifications()
        {
            var preLength = repoMock.AsQueryable().ToList().Count;

            var mailSub = NSubstitute.Substitute.For<IMailService>();
            repoMock.noti1 = new MailNotificationSchedule()
            {
                Id = 1,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Notified = false,
                Repeat = true
            };
            repoMock.noti2 = new MailNotificationSchedule()
            {
                Id = 2,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Notified = false,
                Repeat = true
            };
            repoMock.ReSeed();
            var uut = new ConsoleMailerService(mailSub, repoMock, _logger, _customSettings);
            uut.RunMailService();
            mailSub.ReceivedWithAnyArgs().SendMails(new DateTime());
            Assert.AreEqual(preLength+2,repoMock.AsQueryable().ToList().Count);
        }



        [Test]
        public void RunMailService_NotificationWithRepeat_ShouldCall_SendMails_AndAddNewNotification()
        {
            var preLength = repoMock.AsQueryable().ToList().Count;

            var mailSub = NSubstitute.Substitute.For<IMailService>();
            repoMock.noti1 = new MailNotificationSchedule()
            {
                Id = 1,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Notified = false,
                Repeat = false
            };
            repoMock.noti2 = new MailNotificationSchedule()
            {
                Id = 2,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Notified = false,
                Repeat = true
            };
            repoMock.ReSeed();
            var uut = new ConsoleMailerService(mailSub, repoMock, _logger, _customSettings);
            uut.RunMailService();
            mailSub.ReceivedWithAnyArgs().SendMails(new DateTime());
            Assert.AreEqual(preLength + 1, repoMock.AsQueryable().ToList().Count);
        }

        [Test]
        public void RunMailService_NoNotificationsForToday_ShouldNotCall_SendMails()
        {
   
            var mailSub = NSubstitute.Substitute.For<IMailService>();
            repoMock.noti1 = new MailNotificationSchedule()
            {
                Id = 1,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(1)),
                Notified = false,
                Repeat = true
            };
            repoMock.noti2 = new MailNotificationSchedule()
            {
                Id = 2,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(2)),
                Notified = false,
                Repeat = true
            };
            repoMock.ReSeed();
            var uut = new ConsoleMailerService(mailSub, repoMock, _logger, _customSettings);
            uut.RunMailService();
            mailSub.DidNotReceive().SendMails(new DateTime());
        }

        [Test]
        public void RunMailService_NoUnnotifiedNotifications_ShouldNotCall_SendMails()
        {
            var mailSub = NSubstitute.Substitute.For<IMailService>();
            repoMock.noti1 = new MailNotificationSchedule()
            {
                Id = 1,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Notified = true,
                Repeat = true
            };
            repoMock.noti2 = new MailNotificationSchedule()
            {
                Id = 2,
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Notified = true,
                Repeat = true
            };
            repoMock.ReSeed();
            var uut = new ConsoleMailerService(mailSub, repoMock, _logger, _customSettings);
            uut.RunMailService();
            mailSub.DidNotReceive().SendMails(new DateTime());
        }
    }
}
