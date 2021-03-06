﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Web.Http;
using System.Web.OData;
using Core.ApplicationServices;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using Microsoft.Owin.Testing;
using Ninject;
using NUnit.Framework;
using OS2Indberetning;
using Owin;
using NSubstitute;
using Presentation.Web.Test;
using Substitute = NSubstitute.Substitute;
using Core.DomainServices.Interfaces;

namespace ApplicationServices.Test.DriveReportServiceTest
{
    [TestFixture]
    public class GetActualLeaderForReportTests
    {
        private IGenericRepository<Employment> _emplMock;
        private IGenericRepository<OrgUnit> _orgUnitMock;
        private IGenericRepository<Core.DomainModel.Substitute> _subMock;
        private IDriveReportService _uut;
        private IReimbursementCalculator _calculatorMock;
        private IGenericRepository<DriveReport> _reportRepoMock;
        private IGenericRepository<RateType> _rateTypeMock;
        private IRoute<RouteInformation> _routeMock;
        private IAddressCoordinates _coordinatesMock;
        private IMailService _mailServiceMock;
        private IGenericRepository<Person> _personMock;
        private List<DriveReport> repoList;
        private Core.ApplicationServices.Logger.ILogger _logger;
        private ICustomSettings _customSettings;

        [SetUp]
        public void SetUp()
        {
            var idCounter = 0;

            repoList = new List<DriveReport>();
            _emplMock = Substitute.For<IGenericRepository<Employment>>();
            _calculatorMock = Substitute.For<IReimbursementCalculator>();
            _orgUnitMock = Substitute.For<IGenericRepository<OrgUnit>>();
            _routeMock = Substitute.For<IRoute<RouteInformation>>();
            _coordinatesMock = Substitute.For<IAddressCoordinates>();
            _subMock = Substitute.For<IGenericRepository<Core.DomainModel.Substitute>>();
            _mailServiceMock = Substitute.For<IMailService>();
            _reportRepoMock = NSubstitute.Substitute.For<IGenericRepository<DriveReport>>();
            _personMock = Substitute.For<IGenericRepository<Person>>();
            _logger = new Core.ApplicationServices.Logger.Logger();
            _customSettings = new CustomSettings();

            _reportRepoMock.Insert(new DriveReport()).ReturnsForAnyArgs(x => x.Arg<DriveReport>()).AndDoes(x => repoList.Add(x.Arg<DriveReport>())).AndDoes(x => x.Arg<DriveReport>().Id = idCounter).AndDoes(x => idCounter++);
            _reportRepoMock.AsQueryable().ReturnsForAnyArgs(repoList.AsQueryable());

            _calculatorMock.Calculate(new RouteInformation(), new DriveReport()).ReturnsForAnyArgs(x => x.Arg<DriveReport>());

            _coordinatesMock.GetAddressCoordinates(new Address()).ReturnsForAnyArgs(new DriveReportPoint()
            {
                Latitude = "1",
                Longitude = "2",
            });

            _routeMock.GetRoute(DriveReportTransportType.Car, new List<Address>()).ReturnsForAnyArgs(new RouteInformation()
            {
                Length = 2000
            });

            _uut = new DriveReportService(_mailServiceMock, _reportRepoMock, _calculatorMock, _orgUnitMock, _emplMock, _subMock, _coordinatesMock, _routeMock, _rateTypeMock, _personMock, _logger, _customSettings);

        }

        [Test]
        public void IsLeader_TwoLeadersInOrgParent_ShouldPickOneOfThemAsActualLeader()
        {
            _subMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Substitute>().AsQueryable());

            var report = new DriveReport()
            {
                PersonId = 1,
                Person = new Person()
                {
                    Id = 1
                },
                EmploymentId = 1,
                Employment = new Employment()
                {
                    IsLeader = true,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2
                    },
                    OrgUnitId = 2

                }
            };

            _orgUnitMock.AsQueryable().ReturnsForAnyArgs(new List<OrgUnit>()
            {
                new OrgUnit()
                {
                    Id = 1,
                    Level = 0
                },
                new OrgUnit()
                {
                    Id = 2,
                    Level = 1,
                    ParentId = 1,
                    Parent = new OrgUnit()
                    {
                        Id = 1,
                        Level = 0
                    }
                }
            }.AsQueryable());

            _emplMock.AsQueryable().ReturnsForAnyArgs(new List<Employment>()
            {
                new Employment()
                {
                    PersonId = 1,
                    Person = new Person()
                    {
                        Id = 1,
                        FullName = "Jon Badstue"
                    },
                    Id = 1,
                    IsLeader = true,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2,
                    },
                    OrgUnitId = 2
                },
                new Employment()
                {
                    PersonId = 2,
                    Person = new Person()
                    {
                        Id = 2,
                        FullName = "Eva Due",
                    },
                    Id = 12,
                    IsLeader = true,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 1,
                    },
                    OrgUnitId = 1
                },
                new Employment()
                {
                    PersonId = 3,
                    Person = new Person()
                    {
                        Id = 3,
                        FullName = "Trille Due",
                    },
                    Id = 13,
                    IsLeader = true,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 1,
                    },
                    OrgUnitId = 1
                },
            }.AsQueryable());

            var res = _uut.GetActualLeaderForReport(report);
            Assert.AreEqual("Eva Due", res.FullName);
        }
    }
}
