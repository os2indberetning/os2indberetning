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
        private RepoMocker<AddressHistory> _addressHistoryMocker;
        private RepoMocker<DriveReport> _driveReportMocker;
        private RepoMocker<Person> _personMocker;
        private RepoMocker<Employment> _employmentMocker;
        private RepoMocker<RateType> _rateTypeMocker;
        private ILogger _logger;

        [SetUp]
        public void Setup()
        {
            _addressHistoryMocker = new RepoMocker<AddressHistory>();
            _driveReportMocker = new RepoMocker<DriveReport>();
            _personMocker = new RepoMocker<Person>();
            _employmentMocker = new RepoMocker<Employment>();
            _rateTypeMocker = new RepoMocker<RateType>();
            _logger = Substitute.For<ILogger>();
        }

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

        protected IReimbursementCalculator GetCalculator()
        {
            var addressHistoryRepo = _addressHistoryMocker.GetMockedRepo();
            var driveReportRepo = _driveReportMocker.GetMockedRepo();
            var personRepo = _personMocker.GetMockedRepo();
            var employmentRepo = _employmentMocker.GetMockedRepo();
            var rateTypeRepo = _rateTypeMocker.GetMockedRepo();
            var route = new RouterMock();
            var personService = GetPersonServiceMock();

            return new ReimbursementCalculator(route, personService, personRepo, employmentRepo, addressHistoryRepo, _logger, rateTypeRepo, driveReportRepo);
        }

        protected IReimbursementCalculator GetCalculator(List<Employment> emplMockData, List<DriveReport> driveReportMockData = null)
        {
            var addressHistoryRepo = _addressHistoryMocker.GetMockedRepo();
            var driveReportRepo = _driveReportMocker.GetMockedRepo(driveReportMockData);
            var personRepo = _personMocker.GetMockedRepo(new List<Person>()
            {
                new Person()
                {
                    Id = 1,
                    FirstName = "Jacob",
                    LastName = "Jensen",
                    DistanceFromHomeToBorder = 2,
                }
            });
            var employmentRepo = _employmentMocker.GetMockedRepo(emplMockData);
            var rateTypeRepo = _rateTypeMocker.GetMockedRepo();
            var route = new RouterMock();
            var personService = GetPersonServiceMock();

            return new ReimbursementCalculator(route, personService, personRepo, employmentRepo, addressHistoryRepo, _logger, rateTypeRepo, driveReportRepo);
        }

        protected IReimbursementCalculator GetCalculator(List<Employment> emplMockData, List<AddressHistory> historyMockData)
        { 
            var addressHistoryRepo = _addressHistoryMocker.GetMockedRepo(historyMockData);
            var driveReportRepo = _driveReportMocker.GetMockedRepo();
            var personRepo = _personMocker.GetMockedRepo(new List<Person>()
            {
                new Person()
                {
                    Id = 1,
                    FirstName = "Jacob",
                    LastName = "Jensen",
                    DistanceFromHomeToBorder = 2,
                }
            });
            var employmentRepo = _employmentMocker.GetMockedRepo(emplMockData);
            var rateTypeRepo = _rateTypeMocker.GetMockedRepo();

            return new ReimbursementCalculator(new RouterMock(), GetPersonServiceMock(), personRepo, employmentRepo, addressHistoryRepo, _logger, rateTypeRepo, driveReportRepo);
        }
    }

    class RepoMocker<T> where T : class
        {
            public IGenericRepository<T> GetMockedRepo(List<T> mockData = null)
            {
                var repo = Substitute.For<IGenericRepository<T>>();
                var list = mockData ?? new List<T>();
                repo.AsQueryable().ReturnsForAnyArgs(list.AsQueryable());

                return repo;
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