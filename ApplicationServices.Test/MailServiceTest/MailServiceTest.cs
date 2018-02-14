using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.MailerService.Impl;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using NSubstitute;
using NUnit.Framework;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ApplicationServices.Test.MailServiceTest
{
    [TestFixture]
    public class MailServiceTest
    {
        private IMailService _mailService;
        private IGenericRepository<DriveReport> _reportRepoMock;
        private IGenericRepository<Core.DomainModel.Substitute> _substitueRepoMock;
        private IGenericRepository<Person> _personRepoMock;
        private List<Person> _personList;
        private IMailSender _mailSenderMock;
        private IDriveReportService _driveReportServiceMock;
        private ILogger _loggerMock;

        [SetUp]
        public void Setup()
        {
            var idCounter = 0;
            _reportRepoMock = NSubstitute.Substitute.For<IGenericRepository<DriveReport>>();
            _substitueRepoMock = NSubstitute.Substitute.For<IGenericRepository<Core.DomainModel.Substitute>>();

            _personRepoMock = NSubstitute.Substitute.For<IGenericRepository<Person>>();
            _personList = new List<Person>();
            _personRepoMock.Insert(new Person()).ReturnsForAnyArgs(x => x.Arg<Person>()).AndDoes(x => _personList.Add(x.Arg<Person>())).AndDoes(x => x.Arg<Person>().Id = idCounter).AndDoes(x => idCounter++);
            _personRepoMock.AsQueryable().ReturnsForAnyArgs(_personList.AsQueryable());

            _mailSenderMock = NSubstitute.Substitute.For<IMailSender>();
            _driveReportServiceMock = NSubstitute.Substitute.For<IDriveReportService>();
            _loggerMock = NSubstitute.Substitute.For<ILogger>();

            _mailService = new MailService(_reportRepoMock, _substitueRepoMock, _personRepoMock, _mailSenderMock, _driveReportServiceMock, _loggerMock);
        }

        [Test]
        public void TestSendMail()
        {
            // Arrange, Act
            _mailService.SendMail("toAddress", "subject", "text");

            // Assert
            _mailSenderMock.Received().SendMail("toAddress", "subject", "text");
        }

        [Test]
        public void TestSendMail_ShouldThrowException()
        {
            
            // Arrange, Act
            TestDelegate callWithNull = () => _mailService.SendMail(null, "subject", "text");
            TestDelegate callWithEmptyString = () => _mailService.SendMail("", "subject", "text");

            // Assert
            Assert.Throws<ArgumentException>(callWithNull);
            Assert.Throws<ArgumentException>(callWithEmptyString);
        }

        [Test]
        public void TestSendMailsToAdmins()
        {
            // Arrange
            var person = _personRepoMock.Insert(new Person
            {
                Mail = "person@test.test",
                RecieveMail = false,
                IsAdmin = false,
                IsActive = true
            });
            var admin1 = _personRepoMock.Insert(new Person
            {
                Mail = "admin1@test.test",
                IsAdmin = true,
                RecieveMail = true,
                IsActive = true
            });
            var admin2 = _personRepoMock.Insert(new Person
            {
                Mail = "admin2@test.test",
                IsAdmin = true,
                AdminRecieveMail = false,
                IsActive = true
            });
            var admin3 = _personRepoMock.Insert(new Person
            {
                Mail = "admin3@test.test",
                IsAdmin = true,
                RecieveMail = true,
                IsActive = false
            });

            // Act
            _mailService.SendMailToAdmins("Subject", "Text");

            // Assert
            _mailSenderMock.DidNotReceive().SendMail("person@test.test", "Subject", "Text");
            _mailSenderMock.Received().SendMail("admin1@test.test", "Subject", "Text");
            _mailSenderMock.DidNotReceive().SendMail("admin2@test.test", "Subject", "Text");
            _mailSenderMock.DidNotReceive().SendMail("admin3@test.test", "Subject", "Text");
        }
    }
}
