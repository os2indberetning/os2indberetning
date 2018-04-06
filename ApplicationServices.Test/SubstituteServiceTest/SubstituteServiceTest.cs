using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using ApplicationServices.Test.FileGenerator;
using Core.ApplicationServices;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using NSubstitute;
using NUnit.Framework;
using Substitute = Core.DomainModel.Substitute;

namespace ApplicationServices.Test.SubstituteServiceTest
{
    [TestFixture]
    public class SubstituteServiceTest
    {

        private SubstituteService _uut;
        private List<Substitute> _repo;
        private IGenericRepository<Substitute> _repoMock;
        private IGenericRepository<OrgUnit> _orgUnitMock;
        private IGenericRepository<Employment> _emplMock;

        private IOrgUnitService _orgService;
        private IDriveReportService _driveService;
        private IGenericRepository<DriveReport> _driveRepo;
        private ILogger _logger;

        [SetUp]
        public void SetUp()
        {
            _orgService = NSubstitute.Substitute.For<IOrgUnitService>();
            _repoMock = NSubstitute.Substitute.For<IGenericRepository<Substitute>>();
            _driveRepo = NSubstitute.Substitute.For<IGenericRepository<DriveReport>>();
            _emplMock = NSubstitute.Substitute.For<IGenericRepository<Employment>>();
            _orgUnitMock = NSubstitute.Substitute.For<IGenericRepository<OrgUnit>>();
            _driveService = NSubstitute.Substitute.For<IDriveReportService>();

            _repo = new List<Substitute>
            {
                new Substitute()
                {
                    Sub = new Person()
                    {
                        CprNumber = "123123",
                        FirstName = "Jacob",
                        LastName = "Jensen",
                        Initials = "JOJ"
                    },
                    Leader = new Person()
                    {
                        CprNumber = "123123",
                        FirstName = "Morten",
                        LastName = "Rasmussen",
                        Initials = "MR"
                    },
                    Person = new Person()
                    {
                        CprNumber = "123123",
                        FirstName = "Morten",
                        LastName = "Rasmussen",
                        Initials = "MR"

                    },
                },
                new Substitute()
                {
                    Sub = new Person()
                    {
                        CprNumber = "123123",
                        FirstName = "Jacob",
                        LastName = "Jensen",
                        Initials = "JOJ"
                    },
                    Leader = new Person()
                    {
                        CprNumber = "123123",
                        FirstName = "Morten",
                        LastName = "Rasmussen",
                        Initials = "MR"
                    },
                    Person = new Person()
                    {
                        CprNumber = "123123",
                        FirstName = "Jacob",
                        LastName = "Jensen",
                        Initials = "JOJ"
                    },
                }
            };

            var leader1 = new Person()
            {
                Id = 1,
                FirstName = "Test",
                LastName = "Testesen",
                Initials = "TT",
                FullName = "Test Testesen [TT]"
            };

            var leader2 = new Person()
            {
                Id = 3,
                FirstName = "Test2",
                LastName = "Testesen2",
                Initials = "2",
                FullName = "Test Testesen [2]"
            };

            var user1 = new Person()
            {
                Id = 2,
                FirstName = "User",
                LastName = "Usersen",
                Initials = "UU",
                FullName = "User Usersen [UU]"
            };

            var user2 = new Person()
            {
                Id = 4,
                FirstName = "User",
                LastName = "Usersen",
                Initials = "UU",
                FullName = "User Usersen [UU]"
            };

            var orgUnit = new OrgUnit()
            {
                Id = 1,
            };

            var orgUnit2 = new OrgUnit()
            {
                Id = 12,
            };

            var leaderEmpl1 = new Employment()
            {
                Id = 1,
                OrgUnitId = 1,
                OrgUnit = orgUnit,
                Person = leader1,
                PersonId = leader1.Id,
                IsLeader = true
            };

            var leaderEmpl2 = new Employment()
            {
                Id = 2,
                OrgUnitId = 2,
                OrgUnit = orgUnit2,
                Person = leader1,
                PersonId = leader1.Id,
                IsLeader = true
            };

            var userEmpl1 = new Employment()
            {
                Id = 3,
                OrgUnitId = 1,
                OrgUnit = orgUnit,
                Person = user1,
                PersonId = user1.Id,
                IsLeader = false
            };

            var userEmpl2 = new Employment()
            {
                Id = 4,
                OrgUnitId = 2,
                OrgUnit = orgUnit2,
                Person = user2,
                PersonId = user2.Id,
                IsLeader = false
            };

            _emplMock.AsQueryable().ReturnsForAnyArgs(new List<Employment>()
            {
                leaderEmpl1,
                userEmpl1,
                leaderEmpl2,
                userEmpl2
            }.AsQueryable());

            _orgUnitMock.AsQueryable().ReturnsForAnyArgs(new List<OrgUnit>()
            {
                orgUnit,
                orgUnit2
            }.AsQueryable());

            var reports = new List<DriveReport>()
            {
                new DriveReport()
                {
                    Id = 1,
                    Employment = userEmpl1,
                    EmploymentId = userEmpl1.Id,
                    PersonId = user1.Id,
                    Person = user1,
                },
                new DriveReport()
                {
                    Id = 2,
                    Employment = userEmpl1,
                    EmploymentId = userEmpl1.Id,
                    PersonId = user1.Id,
                    Person = user1,
                },
                new DriveReport()
                {
                    Id = 3,
                    Employment = userEmpl2,
                    EmploymentId = userEmpl2.Id,
                    PersonId = user2.Id,
                    Person = user2,
                },
                new DriveReport()
                {
                    Id = 4,
                    Employment = userEmpl2,
                    EmploymentId = userEmpl2.Id,
                    PersonId = user2.Id,
                    Person = user2,
                },
                new DriveReport()
                {
                    Id = 5,
                    Employment = leaderEmpl1,
                    EmploymentId = leaderEmpl1.Id,
                    PersonId = 1
                },
                new DriveReport()
                {
                    Id = 6,
                    Employment = leaderEmpl2,
                    EmploymentId = leaderEmpl2.Id,
                    PersonId = 3
                }
            };

            _driveRepo.AsQueryable().ReturnsForAnyArgs(reports.AsQueryable());

            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);
        }

        [Test]
        public void ScrubCPR_ShouldRemoveCPR_FromLeaderAndSubAndPersons()
        {

            // Precondition

            Assert.AreEqual("123123", _repo[0].Leader.CprNumber);
            Assert.AreEqual("123123", _repo[0].Sub.CprNumber);
            Assert.AreEqual("123123", _repo[0].Person.CprNumber);


            Assert.AreEqual("123123", _repo[1].Leader.CprNumber);
            Assert.AreEqual("123123", _repo[1].Sub.CprNumber);
            Assert.AreEqual("123123", _repo[1].Person.CprNumber);

            // Act
            _uut.ScrubCprFromPersons(_repo.AsQueryable());

            // Postcondition
            Assert.AreEqual("", _repo[0].Leader.CprNumber);
            Assert.AreEqual("", _repo[0].Sub.CprNumber);

            Assert.AreEqual("", _repo[0].Person.CprNumber);


            Assert.AreEqual("", _repo[1].Leader.CprNumber);
            Assert.AreEqual("", _repo[1].Sub.CprNumber);

            Assert.AreEqual("", _repo[1].Person.CprNumber);
        }

        [Test]
        public void GetStartOfDayTimestamp_shouldreturn_correctvalue()
        {
            var res = _uut.GetStartOfDayTimestamp(1431341025);
            var dateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0);
            dateTime = dateTime.AddSeconds(res).ToLocalTime();
            Assert.AreEqual(11, dateTime.Day);
            Assert.AreEqual(0, dateTime.Hour);
            Assert.AreEqual(0, dateTime.Minute);
            Assert.AreEqual(0, dateTime.Second);

        }

        [Test]
        public void GetEndOfDayTimestamp_shouldreturn_correctvalue()
        {
            var res = _uut.GetEndOfDayTimestamp(1431304249);
            var dateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0);
            dateTime = dateTime.AddSeconds(res).ToLocalTime();
            Assert.AreEqual(11, dateTime.Day);
            Assert.AreEqual(23, dateTime.Hour);
            Assert.AreEqual(59, dateTime.Minute);
            Assert.AreEqual(59, dateTime.Second);
        }

        [Test]
        public void CheckIfNewSubIsAllowed_NoExistingSubs_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 12,
                EndDateTimestamp = 2000,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12
            };

            _repo = new List<Substitute>();
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingSubBeforeNewSub_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1432166400,
                EndDateTimestamp = 1432252800,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1431993600,
                    EndDateTimestamp = 1432080000,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    OrgUnitId = 12
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingSubAfterNewSub_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432080000,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1432166400,
                    EndDateTimestamp = 1432252800,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    OrgUnitId = 12
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingSub_SamePeriod_DifferentOrg_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432080000,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1431993600,
                    EndDateTimestamp = 1432080000,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    OrgUnitId = 13
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingSub_SamePeriod_SameOrg_ShouldReturnFalse()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432080000,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12,
                Id = 1,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1431993600,
                    EndDateTimestamp = 1432080000,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    OrgUnitId = 12,
                    Id = 2
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsFalse(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingSub_DifferentPersonId_SameOrg_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432080000,
                PersonId = 2,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12,
                Id = 1,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1431993600,
                    EndDateTimestamp = 1432080000,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    OrgUnitId = 12,
                    Id = 2
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingSub_SamePersonId_SameOrg_DifferentPeriod_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1531993600,
                EndDateTimestamp = 1532080000,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12,
                Id = 1,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1431993600,
                    EndDateTimestamp = 1432080000,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    OrgUnitId = 12,
                    Id = 2
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingSub_OverlappingPeriod_SameOrg_ShouldReturnFalse()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432166400,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12,
                Id = 1
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1432080000,
                    EndDateTimestamp = 1432252800,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    OrgUnitId = 12,
                    Id = 2,
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsFalse(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingSub_OverlappingPeriod2_SameOrg_ShouldReturnFalse()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1432166400,
                EndDateTimestamp = 1432339200,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12,
                Id = 1,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1432080000,
                    EndDateTimestamp = 1432252800,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    OrgUnitId = 12,
                    Id = 2,
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsFalse(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_NoExistingApprovers_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 12,
                EndDateTimestamp = 2000,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
                OrgUnitId = 12
            };

            _repo = new List<Substitute>();
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingApproverBeforeNewApprover_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1432166400,
                EndDateTimestamp = 1432252800,
                PersonId = 1,
                LeaderId = 3,
                SubId = 2,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1431993600,
                    EndDateTimestamp = 1432080000,
                    PersonId = 1,
                    LeaderId = 3,
                    SubId = 2,
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingApproverAfterNewApprover_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432080000,
                PersonId = 1,
                LeaderId = 3,
                SubId = 2,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1432166400,
                    EndDateTimestamp = 1432252800,
                    PersonId = 1,
                    LeaderId = 3,
                    SubId = 2,
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingApprover_SamePeriod_DifferentPerson_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432080000,
                PersonId = 5,
                LeaderId = 3,
                SubId = 2,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1431993600,
                    EndDateTimestamp = 1432080000,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingApprover_SamePeriod_SamePerson_ShouldReturnFalse()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432080000,
                PersonId = 3,
                LeaderId = 1,
                SubId = 2,
                Id = 1,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1431993600,
                    EndDateTimestamp = 1432080000,
                    PersonId = 3,
                    LeaderId = 1,
                    SubId = 2,
                    Id = 2,
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsFalse(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingApprover_OverlappingPeriod_SamePerson_ShouldReturnFalse()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1431993600,
                EndDateTimestamp = 1432166400,
                PersonId = 1,
                LeaderId = 3,
                SubId = 2,
                Id = 1,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1432080000,
                    EndDateTimestamp = 1432252800,
                    PersonId = 1,
                    LeaderId = 3,
                    SubId = 2,
                    Id = 2
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsFalse(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingApprover_OverlappingPeriod2_SamePerson_ShouldReturnFalse()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1432166400,
                EndDateTimestamp = 1432339200,
                PersonId = 1,
                LeaderId = 3,
                SubId = 2,
                Id = 1
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1432080000,
                    EndDateTimestamp = 1432252800,
                    PersonId = 1,
                    LeaderId = 3,
                    SubId = 2,
                    Id = 2
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsFalse(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_ExistingApprover_SamePeriod_AddSub_ShouldReturnTrue()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1432166400,
                EndDateTimestamp = 1432339200,
                PersonId = 1,
                LeaderId = 1,
                SubId = 2,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1432166400,
                    EndDateTimestamp = 1432339200,
                    PersonId = 1,
                    LeaderId = 3,
                    SubId = 2,
                    OrgUnitId = 12
                },
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }

        [Test]
        public void CheckIfNewSubIsAllowed_SamePeriod_SameOrgUnit_DifferentSubId()
        {
            var substitute = new Substitute()
            {
                StartDateTimestamp = 1432166400,
                EndDateTimestamp = 1432339200,
                PersonId = 1,
                OrgUnitId = 12,
                LeaderId = 1,
                SubId = 3,
            };

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = 1432166400,
                    EndDateTimestamp = 1432339200,
                    PersonId = 1,
                    LeaderId = 3,
                    SubId = 2,
                    OrgUnitId = 12
                },

                new Substitute()
                {
                    StartDateTimestamp = 1432166400,
                    EndDateTimestamp = 1432339200,
                    PersonId = 1,
                    OrgUnitId = 12,
                    LeaderId = 1,
                    SubId = 2
                }
            };

            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());
            _uut = new SubstituteService(_repoMock, _orgService, _driveService, _driveRepo, _logger);

            Assert.IsTrue(_uut.CheckIfNewSubIsAllowed(substitute));
        }
        
        [Test]
        public void UpdateReportsAffectedBySubstitute_PersonalApproverUser1()
        {
            var todayUnix = Utilities.ToUnixTime(DateTime.Now);
            var tomorrowUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(1));
            var yesterdayUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(-1));
            var weekAgoUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(-7));

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = weekAgoUnix,
                    EndDateTimestamp = todayUnix,
                    PersonId = 2,
                    LeaderId = 1,
                    SubId = 4,
                    Sub = new Person{ Id = 4},
                    OrgUnitId = 1
                }
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());

            _uut.UpdateReportsAffectedBySubstitute(_repoMock.AsQueryable().ToList()[0]);

            // Asserts
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[0]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[1]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[2]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[3]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[4]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[5]);
        }

        [Test]
        public void UpdateReportsAffectedBySubstitute_SubstituteLeaderId3()
        {
            var todayUnix = Utilities.ToUnixTime(DateTime.Now);
            var tomorrowUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(1));
            var yesterdayUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(-1));
            var weekAgoUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(-7));

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = weekAgoUnix,
                    EndDateTimestamp = todayUnix,
                    PersonId = 3,
                    LeaderId = 3,
                    SubId = 2,
                    Sub = new Person{ Id = 2},
                    OrgUnitId = 2
                }
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());

            _uut.UpdateReportsAffectedBySubstitute(_repoMock.AsQueryable().ToList()[0]);

            // Asserts
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[2]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[3]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[5]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[0]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[1]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[4]);
        }

        [Test]
        public void UpdateReportsAffectedBySubstitute_SubstituteLeaderId1()
        {
            var todayUnix = Utilities.ToUnixTime(DateTime.Now);
            var tomorrowUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(1));
            var yesterdayUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(-1));
            var weekAgoUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(-7));

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = weekAgoUnix,
                    EndDateTimestamp = todayUnix,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    Sub = new Person{Id = 4},
                    OrgUnitId = 1
                }
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());

            _uut.UpdateReportsAffectedBySubstitute(_repoMock.AsQueryable().ToList()[0]);

            // Asserts
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[0]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[1]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[4]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[2]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[3]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[5]);
        }

        [Test]
        public void UpdateResponsibleLeadersDaily_TestSubAndPersApprover()
        {
            var todayUnix = Utilities.ToUnixTime(DateTime.Now);
            var tomorrowUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(1));
            var yesterdayUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(-1));
            var weekAgoUnix = Utilities.ToUnixTime(DateTime.Now.AddDays(-7));

            _repo = new List<Substitute>()
            {
                new Substitute()
                {
                    StartDateTimestamp = weekAgoUnix,
                    EndDateTimestamp = todayUnix,
                    PersonId = 1,
                    LeaderId = 1,
                    SubId = 2,
                    Sub = new Person{Id = 4},
                    OrgUnitId = 1
                },
                new Substitute()
                {
                    StartDateTimestamp = weekAgoUnix,
                    EndDateTimestamp = todayUnix,
                    PersonId = 4,
                    LeaderId = 3,
                    SubId = 2,
                    Sub = new Person{ Id = 2},
                }
            };
            _repoMock.AsQueryable().ReturnsForAnyArgs(_repo.AsQueryable());

            _uut.UpdateResponsibleLeadersDaily();

            // Asserts
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[0]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[1]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[2]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[3]);
            _driveService.Received().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[4]);
            _driveService.DidNotReceive().GetResponsibleLeadersForReport(_driveRepo.AsQueryable().ToList()[5]);
        }
    }
}
