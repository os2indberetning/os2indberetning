using Core.ApplicationServices.FileGenerator;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.Interfaces;
using NSubstitute;
using NUnit.Framework;

namespace ApplicationServices.Test.FileGenerator
{
    [TestFixture]
    public class FileRecordTest
    {
        private ICustomSettings _customSettings;

        private static readonly Employment _employment = new Employment
        {
            EmploymentId = "2",
            EmploymentType = 1,
            CostCenter = 1234,
        };

        private readonly DriveReport _report = new DriveReport
        {
            DriveDateTimestamp = 1425982953,
            TFCode = "310-4",
            Distance = 100,
            Employment = _employment
        };

        private const string cpr = "1234567890";

        [SetUp]
        public void Setup()
        {
            _customSettings = NSubstitute.Substitute.For<ICustomSettings>();
            _customSettings.KMDBackupFilePath.Returns("/sti/til/kmd/mappe");
            _customSettings.KMDFileName.Returns("kmdFilNavn");
            _customSettings.KMDHeader.Returns("første linje i kmd fil");
            _customSettings.KMDStaticNumber.Returns("DA6");
            _customSettings.KMDMunicipalityNumber.Returns("2222");
            _customSettings.KMDReservedNumber.Returns("000000");
        }

        [Test]
        public void DistanceWithoutDecimalsShouldHave00Appended()
        {
            _report.Distance = 3999;
            var record = new FileRecord(_report, cpr, _customSettings);
            var recordString = record.ToString();
            Assert.AreEqual("399900", getDistanceFromRecordString(recordString));
        }

        [Test]
        public void DistanceShouldBePaddedToFourDigitsBeforeDecimal()
        {
            _report.Distance = 39.99;
            var record = new FileRecord(_report, cpr, _customSettings);
            var recordString = record.ToString();
            Assert.AreEqual("003999", getDistanceFromRecordString(recordString));
        }

        [Test]
        public void DistanceShouldBePaddedToTwoDigitsAfterDecimal()
        {
            _report.Distance = 3999.9;
            var record = new FileRecord(_report, cpr, _customSettings);
            var recordString = record.ToString();
            Assert.AreEqual("399990", getDistanceFromRecordString(recordString));
        }

        private string getDistanceFromRecordString(string recordString)
        {
            var distance = recordString.Substring(24, 6);
            return distance;
        }
    }

}
