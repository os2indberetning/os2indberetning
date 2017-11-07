using Core.ApplicationServices;
using Core.ApplicationServices.FileGenerator;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
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
        private IGenericRepository<DriveReport> _reportRepoMock;
        private List<DriveReport> _reportList;
        private ILogger _loggerMock;
        

        [SetUp]
        public void Setup()
        {
            var idCounter = 0;
            _reportRepoMock.Insert(new DriveReport()).ReturnsForAnyArgs(x => x.Arg<DriveReport>()).AndDoes(x => _reportList.Add(x.Arg<DriveReport>())).AndDoes(x => x.Arg<DriveReport>().Id = idCounter).AndDoes(x => idCounter++);
            _reportRepoMock.AsQueryable().ReturnsForAnyArgs(_reportList.AsQueryable());

            _reportGeneratorMock = NSubstitute.Substitute.For<IReportGenerator>();
            _loggerMock = NSubstitute.Substitute.For<ILogger>();

            _transferToPayRollService = new TransferToPayrollService(_reportGeneratorMock, _reportRepoMock, _loggerMock);
        }

        [Test]
        public void test()
        {

        }
    }
}
