using System.Collections.Generic;
using Core.DomainModel;
using Presentation.Web.Test.Controllers;

namespace ApplicationServices.Test.FileGenerator
{
    /**
     * Person 1 has 3 reports where two is accepted and one is invoiced.
     * The accepted reports are both from March 13 2015 and has a 
     * total distance of 600, and are both of the same tfcode
     * 
     * Person 2 has 5 reports, 1 rejected and 4 accepted.
     * 3 of the accepted are from march, with two with tfcode 310-4
     * and a total distance of 800, while the other in
     * march has another tfcode and a distance of 600.
     * The last accepted report is from april 2015.
     * and has tfcode 310-4 and a distance of 600
     */
    public class ReportRepositoryMock : GenericRepositoryMock<DriveReport>
    {
        public ReportRepositoryMock()
        {
            ReSeed();
        }

        private static readonly Employment Employment = new Employment
        {
            EmploymentId = "2",
            EmploymentType = 1
        };

        private static readonly Person Person1 = new Person
        {
            CprNumber = "1111111111"
        };

        private static readonly Person Person2 = new Person
        {
            CprNumber = "2222222222"
        };

        public DriveReport Report1Accepted = new DriveReport
        {
            DriveDateTimestamp = 1426248000, //March 13 2015
            TFCode = "310-4",
            Distance = 100,
            Employment = Employment,
            Person = Person1,
            Status = ReportStatus.Accepted,
            AccountNumber = "",
            ApprovedBy = Person2,
            LicensePlate = "aaa1213"
        };

        public DriveReport Report2Accepted = new DriveReport
        {
            DriveDateTimestamp = 1426248000,  //March 13 2015
            TFCode = "310-4",
            Distance = 200,
            Employment = Employment,
            Person = Person2,
            Status = ReportStatus.Accepted,
            AccountNumber = "",
            ApprovedBy = Person1,
            LicensePlate = "bba1213"
        };

        public DriveReport Report3Rejected = new DriveReport
        {
            DriveDateTimestamp = 1426334400, //March 14 2015
            TFCode = "310-4",
            Distance = 300,
            Employment = Employment,
            Person = Person2,
            Status = ReportStatus.Rejected,
            AccountNumber = "",
            ApprovedBy = Person1,
            LicensePlate = "bba1213"
        };

        public DriveReport Report4Invoiced = new DriveReport
        {
            DriveDateTimestamp = 1429012800,  //April 13 2015
            TFCode = "310-4",
            Distance = 400,
            Employment = Employment,
            Person = Person1,
            Status = ReportStatus.Invoiced,
            AccountNumber = "",
            ApprovedBy = Person2,
            LicensePlate = "aaa1213"
        };

        public DriveReport Report5Accepted = new DriveReport
        {
            DriveDateTimestamp = 1426248000,  //March 13 2015
            TFCode = "310-4",
            Distance = 500,
            Employment = Employment,
            Person = Person1,
            Status = ReportStatus.Accepted,
            AccountNumber = "",
            ApprovedBy = Person2,
            LicensePlate = "aaa1213"
        };

        public DriveReport Report6Accepted = new DriveReport
        {
            DriveDateTimestamp = 1426334400, //March 14 2015
            TFCode = "310-4",
            Distance = 600,
            Employment = Employment,
            Person = Person2,
            Status = ReportStatus.Accepted,
            AccountNumber = "",
            ApprovedBy = Person1,
            LicensePlate = "bba1213"
        };

        public DriveReport Report7Accepted = new DriveReport
        {
            DriveDateTimestamp = 1429012800,  //April 13 2015
            TFCode = "310-4",
            Distance = 600,
            Employment = Employment,
            Person = Person2,
            Status = ReportStatus.Accepted,
            AccountNumber = "",
            ApprovedBy = Person1,
            LicensePlate = "bba1213"
        };

        public DriveReport Report8Accepted = new DriveReport
        {
            DriveDateTimestamp = 1426334400, //March 14 2015
            TFCode = "310-3",
            Distance = 600,
            Employment = Employment,
            Person = Person2,
            Status = ReportStatus.Accepted,
            AccountNumber = "",
            ApprovedBy = Person1,
            LicensePlate = "bba1213"
        };

        public DriveReport Report9Accepted = new DriveReport
        {
            DriveDateTimestamp = 1426334400, //March 14 2015
            TFCode = "310-9",
            TFCodeOptional = "310-4",
            Distance = 600,
            Employment = Employment,
            Person = Person2,
            Status = ReportStatus.Accepted,
            AccountNumber = "",
            ApprovedBy = Person1,
            LicensePlate = "bba1213"
        };

        protected override List<DriveReport> Seed()
        {
            return new List<DriveReport>
            {
                Report1Accepted, Report2Accepted, Report3Rejected, Report4Invoiced, Report5Accepted, Report6Accepted, Report7Accepted, Report8Accepted, Report9Accepted
            };
        }
    }
}