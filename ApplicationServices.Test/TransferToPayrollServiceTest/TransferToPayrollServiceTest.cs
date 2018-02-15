using Core.ApplicationServices;
using Core.ApplicationServices.FileGenerator;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.SilkeborgData;
using Core.DomainModel;
using Core.DomainServices;
using NSubstitute;
using NUnit.Framework;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ApplicationServices.Test.TransferToPayrollServiceTest
{
    [TestFixture]
    public class TransferToPayrollServiceTest
    {
        private ITransferToPayrollService _transferToPayRollService;
        private IReportGenerator _reportGeneratorMock;
        private List<DriveReport> _reportList;
        private IGenericRepository<DriveReport> _reportRepoMock;
        private ISdClient _sdClientMock;
        private ILogger _loggerMock;
        private ICustomSettings _customSettingsMock;

        [SetUp]
        public void Setup()
        {
            var idCounter = 0;
            _reportList = new List<DriveReport>();
            _reportRepoMock = NSubstitute.Substitute.For<IGenericRepository<DriveReport>>();
            _reportRepoMock.Insert(new DriveReport()).ReturnsForAnyArgs(x => x.Arg<DriveReport>()).AndDoes(x => _reportList.Add(x.Arg<DriveReport>())).AndDoes(x => x.Arg<DriveReport>().Id = idCounter).AndDoes(x => idCounter++);
            _reportRepoMock.AsQueryable().ReturnsForAnyArgs(_reportList.AsQueryable());
            _reportGeneratorMock = NSubstitute.Substitute.For<IReportGenerator>();
            _sdClientMock = NSubstitute.Substitute.For<ISdClient>();
            _loggerMock = NSubstitute.Substitute.For<ILogger>();
            _customSettingsMock = NSubstitute.Substitute.For<ICustomSettings>();
            _customSettingsMock.SdIsEnabled.ReturnsForAnyArgs(true);
            _customSettingsMock.SdUsername.ReturnsForAnyArgs("usernametest");
            _customSettingsMock.SdPassword.ReturnsForAnyArgs("passwordtest");
            _customSettingsMock.SdInstitutionNumber.ReturnsForAnyArgs("institutionnumbertest");
            _loggerMock = NSubstitute.Substitute.For<ILogger>();

            _transferToPayRollService = new TransferToPayrollService(_reportGeneratorMock, _reportRepoMock, _sdClientMock, _loggerMock, _customSettingsMock);
        }

        [Test]
        public void TestTransferReportsToPayrollWithSD()
        {
            // Arrange
            var shouldProcessAndChangeStatus = _reportRepoMock.Insert(new DriveReport
            {
                Status = ReportStatus.Accepted,
                Distance = 50,
                Employment = new Employment()
                {
                    Id = 0
                },
                TFCode = "0000",
                DriveDateTimestamp = 0,
                LicensePlate = "aa 12 123",
                Person = new Person()
                {
                    CprNumber = "1234123400"
                }
            });

            var shouldNotProcessBecausePending = _reportRepoMock.Insert(new DriveReport
            {
                Status = ReportStatus.Pending,
                Distance = 50,
                Employment = new Employment()
                {
                    Id = 0
                },
                TFCode = "0000",
                DriveDateTimestamp = 0,
                LicensePlate = "aa 12 123",
                Person = new Person()
                {
                    CprNumber = "1234123400"
                }
            });

            var shouldNotProcessBecauseRejected = _reportRepoMock.Insert(new DriveReport
            {
                Status = ReportStatus.Rejected,
                Distance = 50,
                Employment = new Employment()
                {
                    Id = 0
                },
                TFCode = "0000",
                DriveDateTimestamp = 0,
                LicensePlate = "aa 12 123",
                Person = new Person()
                {
                    CprNumber = "1234123400"
                }
            });

            var shouldChangeStatus = _reportRepoMock.Insert(new DriveReport
            {
                Status = ReportStatus.Accepted,
                Distance = 0,
                Employment = new Employment()
                {
                    Id = 0
                },
                TFCode = "0000",
                DriveDateTimestamp = 0,
                LicensePlate = "aa 12 123",
                Person = new Person()
                {
                    CprNumber = "1234123400"
                }
            });

            // Act
            _transferToPayRollService.TransferReportsToPayroll();

            // Assert
            Assert.AreEqual(ReportStatus.Invoiced, shouldProcessAndChangeStatus.Status);
            Assert.AreEqual(ReportStatus.Pending, shouldNotProcessBecausePending.Status);
            Assert.AreEqual(ReportStatus.Rejected, shouldNotProcessBecauseRejected.Status);
            Assert.AreEqual(ReportStatus.Invoiced, shouldChangeStatus.Status);
            _sdClientMock.ReceivedWithAnyArgs(1).SendRequest(new Core.ApplicationServices.SdKoersel.AnsaettelseKoerselOpretInputType());
        }
    }
}
