using System;
using System.Collections.Generic;
using System.Linq;
using Core.ApplicationServices;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using NUnit.Framework;
using NSubstitute;
using Substitute = NSubstitute.Substitute;
using Core.DomainServices.Interfaces;

namespace ApplicationServices.Test.DriveReportServiceTest
{
    [TestFixture] //TODO rewrite tests, did not catch that the person was always set as responsible leader
    /** Things to test: 
     *      person is an employee
     *      person is a leader (approver is leader of next level
     *      Person is leader on two levels
     *      Person has personal approver
     *      persons leader has substitute
     */

    public class GetResponsibleLeaderTests
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
        public void NoSubs_NoLeaderInReportOrg_ShouldReturn_ClosestParentOrgLeader()
        {
            _subMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Substitute>().AsQueryable());

            var report = new DriveReport()
            {
                PersonId = 1,
                Person = new Person()
                {
                    Id = 1,
                },
                EmploymentId = 1,
                Employment = new Employment()
                {
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
                    IsLeader = false,
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
            }.AsQueryable());

            var res = _uut.GetResponsibleLeadersForReport(report);
            Assert.AreEqual("Eva Due", res[0].FullName);
        }

        [Test]
        public void SubstituteForLeader_ShouldReturnSubstitute()
        {
            var yesterdayStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds;
            var tomorrowStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(-1))).TotalSeconds;

            _subMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Substitute>()
            {
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2,
                    },
                    OrgUnitId = 2,
                    Person = new Person()
                    {
                        Id = 2,
                    },
                    PersonId = 2,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 3,
                        FullName = "Heidi Huber"
                    },
                    SubId = 3
                }
            }.AsQueryable());

            var report = new DriveReport()
            {
                PersonId = 1,
                Person = new Person()
                {
                    Id = 1,
                },
                EmploymentId = 1,
                Employment = new Employment()
                {
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
                    IsLeader = false,
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
            }.AsQueryable());

            var res = _uut.GetResponsibleLeadersForReport(report);
            Assert.AreEqual("Eva Due", res[0].FullName);
            Assert.AreEqual("Heidi Huber", res[1].FullName);
        }

        [Test]
        public void PersonalApproverForReportOwner_ShouldReturnPersonalApprover()
        {
            var yesterdayStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds;
            var tomorrowStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(-1))).TotalSeconds;

            _subMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Substitute>()
            {
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    Person = new Person()
                    {
                        Id = 1,
                    },
                    OrgUnit = new OrgUnit()
                    {
                        Id = 1,
                    },
                    OrgUnitId = 1,
                    PersonId = 1,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 3,
                        FullName = "Heidi Huber Approves"
                    },
                    SubId = 3
                }
            }.AsQueryable());

            var report = new DriveReport()
            {
                PersonId = 1,
                Person = new Person()
                {
                    Id = 1,
                },
                EmploymentId = 1,
                Employment = new Employment()
                {
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
                    IsLeader = false,
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
            }.AsQueryable());

            var res = _uut.GetResponsibleLeadersForReport(report);
            Assert.AreEqual("Heidi Huber Approves", res[0].FullName);
        }

        [Test]
        public void PersonalApproverForReportOwner_AndSubstituteForLeader_ShouldReturnPersonalApprover()
        {
            var yesterdayStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds;
            var tomorrowStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(-1))).TotalSeconds;

            _subMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Substitute>()
            {
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    Person = new Person()
                    {
                        Id = 1,
                    },
                    PersonId = 1,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 3,
                        FullName = "Heidi Huber Approves"
                    },
                    SubId = 3
                },
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2,
                    },
                    OrgUnitId = 2,
                    Person = new Person()
                    {
                        Id = 2,
                    },
                    PersonId = 2,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 3,
                        FullName = "Heidi Huber"
                    },
                    SubId = 3
                }
            }.AsQueryable());

            var report = new DriveReport()
            {
                PersonId = 1,
                Person = new Person()
                {
                    Id = 1,
                },
                EmploymentId = 1,
                Employment = new Employment()
                {
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
                    IsLeader = false,
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
            }.AsQueryable());

            var res = _uut.GetResponsibleLeadersForReport(report);
            Assert.AreEqual("Heidi Huber Approves", res[0].FullName);
        }

        [Test]
        public void SubstituteTakingOverLeaderReports_ShouldOnlyReturnSubstitute()
        {
            var yesterdayStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds;
            var tomorrowStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(-1))).TotalSeconds;

            _subMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Substitute>()
            {
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2,
                    },
                    OrgUnitId = 2,
                    Person = new Person()
                    {
                        Id = 2,
                    },
                    PersonId = 2,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 3,
                        FullName = "Heidi Huber"
                    },
                    SubId = 3,
                    TakesOverOriginalLeaderReports = true
                }
            }.AsQueryable());

            var report = new DriveReport()
            {
                PersonId = 1,
                Person = new Person()
                {
                    Id = 1,
                },
                EmploymentId = 1,
                Employment = new Employment()
                {
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
                    IsLeader = false,
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
            }.AsQueryable());

            var res = _uut.GetResponsibleLeadersForReport(report);
            Assert.AreEqual(res.Count, 1);
            Assert.AreEqual("Heidi Huber", res[0].FullName);
        }

        [Test]
        public void SubstituteTakingOverLeaderReports_With_Multiple_Substitutes_ShouldOnlyReturnAllSubstitutes()
        {
            var yesterdayStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds;
            var tomorrowStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(-1))).TotalSeconds;

            _subMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Substitute>()
            {
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2,
                    },
                    OrgUnitId = 2,
                    Person = new Person()
                    {
                        Id = 2,
                    },
                    PersonId = 2,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 3,
                        FullName = "Heidi Huber"
                    },
                    SubId = 3,
                    TakesOverOriginalLeaderReports = true
                },
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2,
                    },
                    OrgUnitId = 2,
                    Person = new Person()
                    {
                        Id = 2,
                    },
                    PersonId = 2,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 4,
                        FullName = "Tom Tester"
                    },
                    SubId = 4,
                }
            }.AsQueryable());

            var report = new DriveReport()
            {
                PersonId = 1,
                Person = new Person()
                {
                    Id = 1,
                },
                EmploymentId = 1,
                Employment = new Employment()
                {
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
                    IsLeader = false,
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
            }.AsQueryable());

            var res = _uut.GetResponsibleLeadersForReport(report);

            Assert.AreEqual(res.Count, 2);
            Assert.AreEqual("Heidi Huber", res[0].FullName);
            Assert.AreEqual("Tom Tester", res[1].FullName);
        }

        [Test]
        public void SubstituteTakingOverLeaderReports_And_Personal_Approver_ShouldOnlyReturnPersonalApprover()
        {
            var yesterdayStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds;
            var tomorrowStamp = (int)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(-1))).TotalSeconds;

            _subMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Substitute>()
            {
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2,
                    },
                    OrgUnitId = 2,
                    Person = new Person()
                    {
                        Id = 2,
                    },
                    PersonId = 2,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 3,
                        FullName = "Heidi Huber"
                    },
                    SubId = 3,
                    TakesOverOriginalLeaderReports = true
                },
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    OrgUnit = new OrgUnit()
                    {
                        Id = 2,
                    },
                    OrgUnitId = 2,
                    Person = new Person()
                    {
                        Id = 2,
                    },
                    PersonId = 2,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 4,
                        FullName = "Tom Tester"
                    },
                    SubId = 4,
                },
                new Core.DomainModel.Substitute()
                {
                    StartDateTimestamp = yesterdayStamp,
                    EndDateTimestamp = tomorrowStamp,
                    Person = new Person()
                    {
                        Id = 1,
                    },
                    OrgUnit = new OrgUnit()
                    {
                        Id = 1,
                    },
                    OrgUnitId = 1,
                    PersonId = 1,
                    LeaderId = 2,
                    Leader = new Person()
                    {
                        Id = 2
                    },
                    Sub = new Person()
                    {
                        Id = 5,
                        FullName = "Karsten Kravspecifikation"
                    },
                    SubId = 5
                }
            }.AsQueryable());

            var report = new DriveReport()
            {
                PersonId = 1,
                Person = new Person()
                {
                    Id = 1,
                },
                EmploymentId = 1,
                Employment = new Employment()
                {
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
                    IsLeader = false,
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
            }.AsQueryable());

            var res = _uut.GetResponsibleLeadersForReport(report);

            Assert.AreEqual(res.Count, 1);
            Assert.AreEqual("Karsten Kravspecifikation", res[0].FullName);
        }
    }
}
