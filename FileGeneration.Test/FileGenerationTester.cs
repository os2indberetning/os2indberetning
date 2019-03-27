using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.Interfaces;
using NUnit.Framework;
using System;
using FileGenerationScheduler;
using NSubstitute;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace FileGeneration.Test
{
    [TestFixture]
    public class FileGenerationTester
    {
        private IGenericRepository<Core.DomainModel.FileGenerationSchedule> fileRepoMock;
        private IGenericRepository<Core.DomainModel.MailNotificationSchedule> mailRepoMock;
        private ILogger _logger;
        private ICustomSettings _customSettings;
        private List<FileGenerationSchedule> fileRepoList;
        private List<MailNotificationSchedule> mailRepoList;


        [SetUp]
        public void SetUp()
        {
            var fileIdCounter = 0;
            var mailIdCounter = 0;

            fileRepoList = new List<FileGenerationSchedule>();
            mailRepoList = new List<MailNotificationSchedule>();

            _logger = NSubstitute.Substitute.For<ILogger>();
            fileRepoMock = NSubstitute.Substitute.For<IGenericRepository<FileGenerationSchedule>>();
            fileRepoMock.Insert(new FileGenerationSchedule()).ReturnsForAnyArgs(x => x.Arg<FileGenerationSchedule>()).AndDoes(x => fileRepoList.Add(x.Arg<FileGenerationSchedule>())).AndDoes(x => x.Arg<FileGenerationSchedule>().Id = fileIdCounter).AndDoes(x => fileIdCounter++);
            fileRepoMock.AsQueryable().ReturnsForAnyArgs(fileRepoList.AsQueryable());

            mailRepoMock = NSubstitute.Substitute.For<IGenericRepository<MailNotificationSchedule>>();
            mailRepoMock.Insert(new MailNotificationSchedule()).ReturnsForAnyArgs(x => x.Arg<MailNotificationSchedule>()).AndDoes(x => mailRepoList.Add(x.Arg<MailNotificationSchedule>())).AndDoes(x => x.Arg<MailNotificationSchedule>().Id = mailIdCounter).AndDoes(x => mailIdCounter++);
            mailRepoMock.AsQueryable().ReturnsForAnyArgs(mailRepoList.AsQueryable());

            _customSettings = new CustomSettings();
        }

        [Test]
        public void RunFileGenService_ShouldCall_TransferReportsToPayrollAndMailToAdmins_NoRepeat()
        {
            var mailServiceSub = NSubstitute.Substitute.For<IMailService>();
            var transferToPayrollServiceSub = NSubstitute.Substitute.For<ITransferToPayrollService>();

            // Insert initial data
            var file1 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Repeat = false
            });
            var file2 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Repeat = false
            });
            var fileRepoEntriesCount = fileRepoList.Count;

            // Run service call
            var service = new FileGenerationService(mailServiceSub, transferToPayrollServiceSub, mailRepoMock, fileRepoMock, _logger);
            service.RunFileGenerationService();

            // Asserts
            // 1. TransferReportsToPayroll called
            // 2. SendMailToAdmins(subject, message) called
            // 3. Count of the entries in the repo not increased (repeat = false)

            transferToPayrollServiceSub.Received().TransferReportsToPayroll();
            mailServiceSub.ReceivedWithAnyArgs().SendMailToAdmins("", "");
            var fileRepoEntriesAfterCall = fileRepoList.Count;
            Assert.AreEqual(fileRepoEntriesCount, fileRepoEntriesAfterCall);
        }

        [Test]
        public void RunFileGenService_ShouldNOTCall_TransferReportsToPayrollAndMailToAdmins()
        {
            var mailServiceSub = NSubstitute.Substitute.For<IMailService>();
            var transferToPayrollServiceSub = NSubstitute.Substitute.For<ITransferToPayrollService>();

            // Insert initial data
            var file1 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(1)),
                Repeat = false
            });
            var file2 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now.AddDays(1)),
                Repeat = false
            });
            var fileRepoEntriesCount = fileRepoList.Count;

            // Run service call
            var service = new FileGenerationService(mailServiceSub, transferToPayrollServiceSub, mailRepoMock, fileRepoMock, _logger);
            service.RunFileGenerationService();

            // Asserts
            // 1. TransferReportsToPayroll NOT called
            // 2. SendMailToAdmins(subject, message) NOT called
            // 3. Count of the entries in the repo not increased

            transferToPayrollServiceSub.DidNotReceive().TransferReportsToPayroll();
            mailServiceSub.DidNotReceiveWithAnyArgs().SendMailToAdmins("", "");
            var fileRepoEntriesAfterCall = fileRepoList.Count;
            Assert.AreEqual(fileRepoEntriesCount, fileRepoEntriesAfterCall);
        }

        [Test]
        public void RunFileGenService_RescheduleDatesWhenRepeatTrue()
        {

            var mailServiceSub = NSubstitute.Substitute.For<IMailService>();
            var transferToPayrollServiceSub = NSubstitute.Substitute.For<ITransferToPayrollService>();
            var datetimeNow = DateTime.Now;

            // Insert initial data
            var file1 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                Repeat = true
            });
            var file2 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                Repeat = true
            });

            var mail1 = mailRepoMock.Insert(new MailNotificationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                FileGenerationScheduleId = file1.Id,
                CustomText = "Custom test1",
            });

            var mail2 = mailRepoMock.Insert(new MailNotificationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                FileGenerationScheduleId = file1.Id,
                CustomText = "Custom test2",
            });

            var mail3 = mailRepoMock.Insert(new MailNotificationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                FileGenerationScheduleId = file2.Id,
                CustomText = "Custom test3",
            });

            var mail4 = mailRepoMock.Insert(new MailNotificationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                FileGenerationScheduleId = file2.Id,
                CustomText = "Custom test4",
            });

            file1.MailNotificationSchedules = new List<MailNotificationSchedule> { mail1, mail2 };
            file2.MailNotificationSchedules = new List<MailNotificationSchedule> { mail3, mail4 };

            var fileRepoEntriesCount = fileRepoList.Count;
            var mailRepoEntriesCount = mailRepoList.Count;

            // Run service call
            var service = new FileGenerationService(mailServiceSub, transferToPayrollServiceSub, mailRepoMock, fileRepoMock, _logger);
            service.RunFileGenerationService();

            // Asserts
            // 1. TransferReportsToPayroll called
            // 2. SendMailToAdmins(subject, message) called
            // 3. There are 2 more fileGenSchedule entries in the fileRepoMock
            // 4. The dates of the new 2 entries are set to a month later compared to the originals
            // 5. There are 4 more entries in the mailRepoMock
            // 6. The new mailNotification objects have the correct FileGenerationScheduleId foreign keys - pointing to the correct fileGen created from the service
            // 7. Custom text should be copied over to the new instance of the mail notifications

            transferToPayrollServiceSub.Received().TransferReportsToPayroll();
            mailServiceSub.ReceivedWithAnyArgs().SendMailToAdmins("", "");
            var fileRepoEntriesAfterCall = fileRepoList.Count;
            var mailRepoEntriesAfterCall = mailRepoList.Count;
            Assert.AreEqual(fileRepoEntriesCount + 2, fileRepoEntriesAfterCall);
            Assert.AreEqual(fileRepoList[2].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(fileRepoList[3].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(mailRepoEntriesCount + 4, mailRepoEntriesAfterCall);
            Assert.AreEqual(mailRepoList[4].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(mailRepoList[5].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(mailRepoList[6].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(mailRepoList[7].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(mailRepoList[4].FileGenerationScheduleId, fileRepoList[2].Id);
            Assert.AreEqual(mailRepoList[5].FileGenerationScheduleId, fileRepoList[2].Id);
            Assert.AreEqual(mailRepoList[6].FileGenerationScheduleId, fileRepoList[3].Id);
            Assert.AreEqual(mailRepoList[7].FileGenerationScheduleId, fileRepoList[3].Id);
            Assert.AreEqual(mail1.CustomText, mailRepoList[4].CustomText);
            Assert.AreEqual(mail2.CustomText, mailRepoList[5].CustomText);
            Assert.AreEqual(mail3.CustomText, mailRepoList[6].CustomText);
            Assert.AreEqual(mail4.CustomText, mailRepoList[7].CustomText);
        }

        [Test]
        public void RunFileGenService_RescheduleDatesWhenRepeatTrue_OnlyOneFile()
        {
            var mailServiceSub = NSubstitute.Substitute.For<IMailService>();
            var transferToPayrollServiceSub = NSubstitute.Substitute.For<ITransferToPayrollService>();
            var datetimeNow = DateTime.Now;

            // Insert initial data
            var file1 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                Repeat = true
            });
            var file2 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                Repeat = false
            });

            var mail1 = mailRepoMock.Insert(new MailNotificationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                FileGenerationScheduleId = file1.Id,
                CustomText = "Custom test1",
            });

            var mail2 = mailRepoMock.Insert(new MailNotificationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                FileGenerationScheduleId = file1.Id,
                CustomText = "Custom test2",
            });

            var mail3 = mailRepoMock.Insert(new MailNotificationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                FileGenerationScheduleId = file2.Id,
                CustomText = "Custom test3",
            });

            var mail4 = mailRepoMock.Insert(new MailNotificationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(datetimeNow),
                FileGenerationScheduleId = file2.Id,
                CustomText = "Custom test4",
            });

            file1.MailNotificationSchedules = new List<MailNotificationSchedule> { mail1, mail2 };
            file2.MailNotificationSchedules = new List<MailNotificationSchedule> { mail3, mail4 };

            var fileRepoEntriesCount = fileRepoList.Count;
            var mailRepoEntriesCount = mailRepoList.Count;

            // Run service call
            var service = new FileGenerationService(mailServiceSub, transferToPayrollServiceSub, mailRepoMock, fileRepoMock, _logger);
            service.RunFileGenerationService();

            // Asserts
            // 1. TransferReportsToPayroll called
            // 2. SendMailToAdmins(subject, message) called
            // 3. There are 1 more fileGenSchedule entries in the fileRepoMock
            // 4. The dates of the new entry are set to a month later compared to the file1
            // 5. There are 2 more entries in the mailRepoMock
            // 6. The new mailNotification objects have the correct FileGenerationScheduleId foreign keys - pointing to the correct fileGen created from the service (fileRepoList[2].Id)
            // 7. Custom text should be copied over to the new instance of the mail1 and mail2

            transferToPayrollServiceSub.Received().TransferReportsToPayroll();
            mailServiceSub.ReceivedWithAnyArgs().SendMailToAdmins("", "");
            var fileRepoEntriesAfterCall = fileRepoList.Count;
            var mailRepoEntriesAfterCall = mailRepoList.Count;
            Assert.AreEqual(fileRepoEntriesCount + 1, fileRepoEntriesAfterCall);
            Assert.AreEqual(fileRepoList[2].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(mailRepoEntriesCount + 2, mailRepoEntriesAfterCall);
            Assert.AreEqual(mailRepoList[4].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(mailRepoList[5].DateTimestamp, Utilities.ToUnixTime(datetimeNow.AddMonths(1)));
            Assert.AreEqual(mailRepoList[4].FileGenerationScheduleId, fileRepoList[2].Id);
            Assert.AreEqual(mailRepoList[5].FileGenerationScheduleId, fileRepoList[2].Id);
            Assert.AreEqual(mail1.CustomText, mailRepoList[4].CustomText);
            Assert.AreEqual(mail2.CustomText, mailRepoList[5].CustomText);
        }

        [Test]
        public void RunFileGenService_IsAllFilesGenerated()
        {
            var mailServiceSub = NSubstitute.Substitute.For<IMailService>();
            var transferToPayrollServiceSub = NSubstitute.Substitute.For<ITransferToPayrollService>();

            // Insert initial data
            var file1 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Completed = false,
                Repeat = false
            });
            var file2 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Completed = false,
                Repeat = false
            });
            var file3 = fileRepoMock.Insert(new FileGenerationSchedule
            {
                DateTimestamp = Utilities.ToUnixTime(DateTime.Now),
                Completed = false,
                Repeat = true
            });
            var fileRepoEntriesCount = fileRepoList.Count;

            // Run service call
            var service = new FileGenerationService(mailServiceSub, transferToPayrollServiceSub, mailRepoMock, fileRepoMock, _logger);
            service.RunFileGenerationService();

            // Asserts
            // 1. 3 files where to schedule, one with Repeat set to true
            // 2. TransferReportsToPayroll called
            // 3. SendMailToAdmins(subject, message) called
            // 4. Files count should be one higher (4)
            // 5. First 3 should have IsFileGenerated set to true
            // 6. Last should not yet been generated

            transferToPayrollServiceSub.Received().TransferReportsToPayroll();
            mailServiceSub.ReceivedWithAnyArgs().SendMailToAdmins("", "");
            var fileRepoEntriesAfterCall = fileRepoList.Count;
            Assert.AreEqual(fileRepoEntriesAfterCall, fileRepoEntriesCount + 1);
            Assert.AreEqual(fileRepoList[0].Completed, true);
            Assert.AreEqual(fileRepoList[1].Completed, true);
            Assert.AreEqual(fileRepoList[2].Completed, true);
            Assert.AreEqual(fileRepoList[3].Completed, false);
        }
    }
}
