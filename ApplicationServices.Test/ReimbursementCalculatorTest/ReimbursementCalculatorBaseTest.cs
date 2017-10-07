using System.Collections.Generic;
using System.Linq;
using Core.ApplicationServices;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using NSubstitute;
using NUnit.Framework;
using Substitute = NSubstitute.Substitute;

namespace ApplicationServices.Test.ReimbursementCalculatorTest
{
    public class ReimbursementCalculatorBaseTest
    {


        protected IPersonService GetPersonServiceMock()
        {



            var personService = Substitute.For<IPersonService>();

            personService.GetHomeAddress(new Person()).ReturnsForAnyArgs(info =>
                new PersonalAddress()
                {
                    Description = "TestHomeAddress",
                    Id = 1,
                    Type = PersonalAddressType.Home,
                    PersonId = 1,
                    StreetName = "Jens Baggesens Vej",
                    StreetNumber = "46",
                    ZipCode = 8210,
                    Town = "Aarhus",
                    Latitude = "12341234",
                    Longitude = "12341234"
                });

            return personService;
        }

        protected IGenericRepository<Person> GetPersonRepository()
        {
            var repo = Substitute.For<IGenericRepository<Person>>();

            repo.AsQueryable().Returns(info => new List<Person>()
            {
                new Person()
                {
                    Id = 1,
                    FirstName = "Jacob",
                    LastName = "Jensen",
                    DistanceFromHomeToBorder = 2,
                }
            }.AsQueryable());

            return repo;
        }

        protected IGenericRepository<Employment> GetEmplRepository(List<Employment> mockData)
        {
            var repo = Substitute.For<IGenericRepository<Employment>>();

            repo.AsQueryable().Returns(info => mockData.AsQueryable());

            return repo;
        } 

        protected IGenericRepository<RateType> GetRateTypeRepository()
        {
            var repo = Substitute.For<IGenericRepository<RateType>>();
            
            return repo;
        }

        protected IGenericRepository<DriveReport> GetDriveReportRepository(List<DriveReport> driveReportMockData = null)
        {
            var repo = Substitute.For<IGenericRepository<DriveReport>>();
            var list = driveReportMockData ?? new List<DriveReport>();

            // repo.AsQueryable().Returns(x => driveReportMockData.AsQueryable());
            repo.AsQueryable().Returns(info => list.AsQueryable());

            return repo;
        }

        protected IReimbursementCalculator GetCalculator(List<Employment> emplMockData, List<DriveReport> driveReportMockData = null)
        { //TODO changed to make the code compile
            var historyMock = NSubstitute.Substitute.For<IGenericRepository<AddressHistory>>();
            historyMock.AsQueryable().ReturnsForAnyArgs(new List<AddressHistory>().AsQueryable());

            var driveReportRepo = GetDriveReportRepository(driveReportMockData);

            return new ReimbursementCalculator(new RouterMock(), GetPersonServiceMock(), GetPersonRepository(), GetEmplRepository(emplMockData),historyMock, NSubstitute.Substitute.For<ILogger>(), GetRateTypeRepository(), driveReportRepo);
        }

        protected IReimbursementCalculator GetCalculator(List<Employment> emplMockData, List<AddressHistory> historyMockData)
        { //TODO changed to make the code compile
            var historyMock = NSubstitute.Substitute.For<IGenericRepository<AddressHistory>>();
            historyMock.AsQueryable().ReturnsForAnyArgs(historyMockData.AsQueryable());

            return new ReimbursementCalculator(new RouterMock(), GetPersonServiceMock(), GetPersonRepository(), GetEmplRepository(emplMockData), historyMock, NSubstitute.Substitute.For<ILogger>(), GetRateTypeRepository(), GetDriveReportRepository());
        }

        protected DriveReport GetDriveReport()
        {
            return new DriveReport()
            {
                KmRate = 1337,
                Distance = 42,
                PersonId = 1,
                DriveReportPoints = new List<DriveReportPoint>()
                {
                    new DriveReportPoint()
                    {
                        Id = 1
                    },
                    new DriveReportPoint()
                    {
                        Id = 2
                    }
                }
            };
        }
    }

    class RouterMock : IRoute<RouteInformation>
    {
        public RouteInformation GetRoute(DriveReportTransportType transportType, IEnumerable<Address> addresses)
        {
            return new RouteInformation()
            {
                Duration = 1337,
                EndStreet = "Katrinebjergvej 95, 8200 Aarhus",
                StartStreet = "Katrinebjergvej 40, 8200 Aarhus",
                GeoPoints = "",
                Length = 42
            };
        }
    }
}