using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using NSubstitute;
using NUnit.Framework;
using Core.ApplicationServices.Logger;
using Core.DomainServices.Interfaces;

namespace ApplicationServices.Test.PersonService
{
    [TestFixture]
    public class PersonServiceTest
    {
        private IQueryable<Person> _persons;
        private IRoute<RouteInformation> _routeMock;
        private IAddressCoordinates _coordinatesMock;
        private IGenericRepository<PersonalAddress> _personalAddressRepoMock;
        private IGenericRepository<Address> _addressRepoMock;
        private IGenericRepository<Employment> _employmentsRepoMock;
        private IGenericRepository<OrgUnit> _orgUnitsRepoMock;

        private IPersonService _uut;
        private ILogger _loggerMock;

        [SetUp]
        public void SetUp()
        {
            _persons = new List<Person>{
                new Person
                {
                    Id = 1,
                    FirstName = "Morten",
                    LastName = "Rasmussen",
                    CprNumber = "1234567890",
                    Initials = "MR"
                },
                new Person
                {
                    Id = 2,
                    FirstName = "Morten",
                    LastName = "Jørgensen",
                    CprNumber = "0987654321",
                    Initials = "MJ"
                },
                new Person
                {
                    Id = 3,
                    FirstName = "Jacob",
                    LastName = "Jensen",
                    CprNumber = "456456456",
                    Initials = "JOJ"
                }
            }.AsQueryable();

            _routeMock = NSubstitute.Substitute.For<IRoute<RouteInformation>>();
            _routeMock.GetRoute(DriveReportTransportType.Car, new List<Address>()).ReturnsForAnyArgs(new RouteInformation());
            _personalAddressRepoMock = NSubstitute.Substitute.For<IGenericRepository<PersonalAddress>>();
            _addressRepoMock = NSubstitute.Substitute.For<IGenericRepository<Address>>();
            _employmentsRepoMock = NSubstitute.Substitute.For<IGenericRepository<Employment>>();
            _orgUnitsRepoMock = NSubstitute.Substitute.For<IGenericRepository<OrgUnit>>();

            _orgUnitsRepoMock.AsQueryable().ReturnsForAnyArgs(new List<OrgUnit>
            {
                new OrgUnit
                {
                    Id = 1,
                    OrgId = 1,
                    ShortDescription = "orgUnit1",
                    LongDescription = "OrgUnit 1.0",
                    Level = 1,
                    ParentId = null
                },
                new OrgUnit
                {
                    Id = 2,
                    OrgId = 2,
                    ShortDescription = "orgUnit2",
                    LongDescription = "OrgUnit 2.1",
                    Level = 2,
                    ParentId = 1
                },
                new OrgUnit
                {
                    Id = 3,
                    OrgId = 3,
                    ShortDescription = "orgUnit3",
                    LongDescription = "OrgUnit 2.2",
                    Level = 2,
                    ParentId = 1
                }
            }.AsQueryable());

            _loggerMock = NSubstitute.Substitute.For<ILogger>();
            _coordinatesMock = NSubstitute.Substitute.For<IAddressCoordinates>();
            _coordinatesMock.GetAddressCoordinates(new Address()).ReturnsForAnyArgs(new Address
            {
                Latitude = "1",
                Longitude = "1"
            });
            _uut = new Core.ApplicationServices.PersonService(_personalAddressRepoMock, _addressRepoMock, _employmentsRepoMock, _orgUnitsRepoMock, _routeMock, _coordinatesMock, _loggerMock);
        }



        [Test]
        public void ScrubCprsShouldRemoveCprNumbers()
        {

            var uut = new Core.ApplicationServices.PersonService(_personalAddressRepoMock, _addressRepoMock, _employmentsRepoMock, _orgUnitsRepoMock, _routeMock, _coordinatesMock, _loggerMock);

            foreach (var person in _persons)
            {
                Assert.AreNotEqual("", person.CprNumber, "Person should have a CPR number before scrubbing");
            }
            _persons = uut.ScrubCprFromPersons(_persons);
            foreach (var person in _persons)
            {
                Assert.AreEqual("", person.CprNumber, "Person should not have a CPR number before scrubbing");
            }
        }

        [Test]
        public void GetHomeAddress_NoAlternative_ShouldReturnActualHomeAddress()
        {
            var testPerson = new Person
            {
                Id = 1
            };

            _personalAddressRepoMock.AsQueryable().ReturnsForAnyArgs(new List<PersonalAddress>
            {
                new PersonalAddress
                {
                    Type = PersonalAddressType.Home,
                    PersonId = 1,
                    Person = testPerson,
                    Latitude = "1",
                    Longitude = "2",
                    StreetName = "Katrinebjergvej",
                    StreetNumber = "93B",
                    ZipCode = 8200,
                    Town = "Aarhus N"
                }
            }.AsQueryable());

            var uut = new Core.ApplicationServices.PersonService(_personalAddressRepoMock, _addressRepoMock, _employmentsRepoMock, _orgUnitsRepoMock, _routeMock, _coordinatesMock, _loggerMock);
            var res = uut.GetHomeAddress(testPerson);
            Assert.AreEqual(PersonalAddressType.Home, res.Type);
            Assert.AreEqual("Katrinebjergvej", res.StreetName);
            Assert.AreEqual("93B", res.StreetNumber);
            Assert.AreEqual(8200, res.ZipCode);
            Assert.AreEqual("Aarhus N", res.Town);
            _coordinatesMock.DidNotReceiveWithAnyArgs().GetAddressCoordinates(new Address());
        }

        [Test]
        public void GetHomeAddress_WithAlternative_ShouldReturnAlternativeHomeAddress()
        {
            var testPerson = new Person
            {
                Id = 1
            };

            _personalAddressRepoMock.AsQueryable().ReturnsForAnyArgs(new List<PersonalAddress>
            {
                new PersonalAddress
                {
                    Type = PersonalAddressType.Home,
                    PersonId = 1,
                    Person = testPerson,
                    Latitude = "1",
                    Longitude = "2",
                    StreetName = "Katrinebjergvej",
                    StreetNumber = "93B",
                    ZipCode = 8200,
                    Town = "Aarhus N"
                },
                new PersonalAddress
                {
                    Type = PersonalAddressType.AlternativeHome,
                    PersonId = 1,
                    Person = testPerson,
                    Latitude = "1",
                    Longitude = "2",
                    StreetName = "Jens Baggesens Vej",
                    StreetNumber = "44",
                    ZipCode = 8210,
                    Town = "Aarhus V"
                }
            }.AsQueryable());

            var uut = new Core.ApplicationServices.PersonService(_personalAddressRepoMock, _addressRepoMock, _employmentsRepoMock, _orgUnitsRepoMock, _routeMock, _coordinatesMock, _loggerMock);
            var res = uut.GetHomeAddress(testPerson);
            Assert.AreEqual(PersonalAddressType.AlternativeHome, res.Type);
            Assert.AreEqual("Jens Baggesens Vej", res.StreetName);
            Assert.AreEqual("44", res.StreetNumber);
            Assert.AreEqual(8210, res.ZipCode);
            Assert.AreEqual("Aarhus V", res.Town);
            _coordinatesMock.DidNotReceiveWithAnyArgs().GetAddressCoordinates(new Address());
        }

        [Test]
        public void GetHomeAddress_WithAlternative_WithNoCoords_ShouldReturnAlternativeHomeAddress_AndCallCoordinates()
        {
            var testPerson = new Person
            {
                Id = 1
            };

            _personalAddressRepoMock.AsQueryable().ReturnsForAnyArgs(new List<PersonalAddress>
            {
                new PersonalAddress
                {
                    Type = PersonalAddressType.Home,
                    PersonId = 1,
                    Person = testPerson,
                    Latitude = "1",
                    Longitude = "2",
                    StreetName = "Katrinebjergvej",
                    StreetNumber = "93B",
                    ZipCode = 8200,
                    Town = "Aarhus N"
                },
                new PersonalAddress
                {
                    Type = PersonalAddressType.AlternativeHome,
                    PersonId = 1,
                    Person = testPerson,
                    StreetName = "Jens Baggesens Vej",
                    StreetNumber = "44",
                    ZipCode = 8210,
                    Town = "Aarhus V"
                }
            }.AsQueryable());

            var uut = new Core.ApplicationServices.PersonService(_personalAddressRepoMock, _addressRepoMock, _employmentsRepoMock, _orgUnitsRepoMock, _routeMock, _coordinatesMock, _loggerMock);
            var res = uut.GetHomeAddress(testPerson);
            Assert.AreEqual(PersonalAddressType.AlternativeHome, res.Type);
            Assert.AreEqual("Jens Baggesens Vej", res.StreetName);
            Assert.AreEqual("44", res.StreetNumber);
            Assert.AreEqual(8210, res.ZipCode);
            Assert.AreEqual("Aarhus V", res.Town);
            _coordinatesMock.ReceivedWithAnyArgs().GetAddressCoordinates(new Address());
        }

        [Test]
        public void GetHomeAddress_WithActual_WithNoCoords_ShouldReturnActualHomeAddress_AndCallCoordinates()
        {
            var testPerson = new Person
            {
                Id = 1
            };

            _personalAddressRepoMock.AsQueryable().ReturnsForAnyArgs(new List<PersonalAddress>
            {
                new PersonalAddress
                {
                    Type = PersonalAddressType.Home,
                    PersonId = 1,
                    Person = testPerson,
                    StreetName = "Katrinebjergvej",
                    StreetNumber = "93B",
                    ZipCode = 8200,
                    Town = "Aarhus N"
                }
            }.AsQueryable());


            var res = _uut.GetHomeAddress(testPerson);
            Assert.AreEqual(PersonalAddressType.Home, res.Type);
            Assert.AreEqual("Katrinebjergvej", res.StreetName);
            Assert.AreEqual("93B", res.StreetNumber);
            Assert.AreEqual(8200, res.ZipCode);
            Assert.AreEqual("Aarhus N", res.Town);
            _coordinatesMock.ReceivedWithAnyArgs().GetAddressCoordinates(new Address());
        }


        [Test]
        public void GetEmploymentForLeader_MultipleLayers()
        {
            _persons = new List<Person>{
                new Person
                {
                    Id = 1,
                    FirstName = "Morten",
                    LastName = "Rasmussen",
                    CprNumber = "1234567890",
                    Initials = "MR",
                    Employments = new List<Employment>() {
                        new Employment
                        {
                            Id = 1,
                            OrgUnit = new OrgUnit
                            {
                                Id = 1,
                                OrgId = 1,
                                ShortDescription = "orgUnit1",
                                LongDescription = "OrgUnit 1.0",
                                Level = 1,
                                ParentId = null
                            },
                            OrgUnitId = 1,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        },
                        new Employment
                        {
                            Id = 2,
                            OrgUnit = new OrgUnit
                            {
                                Id = 2,
                                OrgId = 2,
                                ShortDescription = "orgUnit2",
                                LongDescription = "OrgUnit 2.1",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 2,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        },
                        new Employment
                        {
                            Id = 3,
                            OrgUnit = new OrgUnit
                            {
                                Id = 2,
                                OrgId = 2,
                                ShortDescription = "orgUnit2",
                                LongDescription = "OrgUnit 2.1",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 3,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        }
                    }
                },
                new Person
                {
                    Id = 2,
                    FirstName = "Morten",
                    LastName = "Jørgensen",
                    CprNumber = "0987654321",
                    Initials = "MJ"
                },
                new Person
                {
                    Id = 3,
                    FirstName = "Jacob",
                    LastName = "Jensen",
                    CprNumber = "456456456",
                    Initials = "JOJ"
                }
            }.AsQueryable();

            _employmentsRepoMock.AsQueryable().ReturnsForAnyArgs(new List<Employment>
            {
                        new Employment
                        {
                            Id = 1,
                            OrgUnit = new OrgUnit
                            {
                                Id = 1,
                                OrgId = 1,
                                ShortDescription = "orgUnit1",
                                LongDescription = "OrgUnit 1.0",
                                Level = 1,
                                ParentId = null
                            },
                            OrgUnitId = 1,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        },
                        new Employment
                        {
                            Id = 2,
                            OrgUnit = new OrgUnit
                            {
                                Id = 2,
                                OrgId = 2,
                                ShortDescription = "orgUnit2",
                                LongDescription = "OrgUnit 2.1",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 2,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        },
                        new Employment
                        {
                            Id = 3,
                            OrgUnit = new OrgUnit
                            {
                                Id = 2,
                                OrgId = 2,
                                ShortDescription = "orgUnit2",
                                LongDescription = "OrgUnit 2.1",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 3,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        },
                        new Employment
                        {
                            Id = 5,
                            OrgUnit = new OrgUnit
                            {
                                Id = 3,
                                OrgId = 2,
                                ShortDescription = "orgUnit2",
                                LongDescription = "OrgUnit 2.1",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 2,
                            IsLeader = false,
                            PersonId = 2,
                            Person = _persons.AsQueryable().ToList()[1]
                        },
                        new Employment
                        {
                            Id = 4,
                            OrgUnit = new OrgUnit
                            {
                                Id = 3,
                                OrgId = 3,
                                ShortDescription = "orgUnit3",
                                LongDescription = "OrgUnit 2.2",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 3,
                            PersonId = 3,
                            Person = _persons.AsQueryable().ToList()[2]
                        }
            }.AsQueryable());

            var result = _uut.GetEmployeesOfLeader(_persons.AsQueryable().ToList()[0]);

            // Asserts
            Assert.AreEqual(3, result.Count);
            Assert.IsTrue(result.Contains(_persons.AsQueryable().ToList()[0]));
            Assert.IsTrue(result.Contains(_persons.AsQueryable().ToList()[1]));
            Assert.IsTrue(result.Contains(_persons.AsQueryable().ToList()[2]));
        }

        [Test]
        public void GetEmploymentForLeader_SingleOrgUnit()
        {
            _persons = new List<Person>{
                new Person
                {
                    Id = 1,
                    FirstName = "Morten",
                    LastName = "Rasmussen",
                    CprNumber = "1234567890",
                    Initials = "MR"
                },
                new Person
                {
                    Id = 2,
                    FirstName = "Morten",
                    LastName = "Jørgensen",
                    CprNumber = "0987654321",
                    Initials = "MJ"
                },
                new Person
                {
                    Id = 3,
                    FirstName = "Jacob",
                    LastName = "Jensen",
                    CprNumber = "456456456",
                    Initials = "JOJ",
                    Employments = new List<Employment>
                    {
                        new Employment
                        {
                            Id = 4,
                            OrgUnit = new OrgUnit
                            {
                                Id = 3,
                                OrgId = 3,
                                ShortDescription = "orgUnit3",
                                LongDescription = "OrgUnit 2.2",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 3,
                            IsLeader = true,
                            PersonId = 3,
                            Person = _persons.AsQueryable().ToList()[2]
                        }
                    }
                }
            }.AsQueryable();

            _employmentsRepoMock.AsQueryable().ReturnsForAnyArgs(new List<Employment>
            {
                        new Employment
                        {
                            Id = 1,
                            OrgUnit = new OrgUnit
                            {
                                Id = 1,
                                OrgId = 1,
                                ShortDescription = "orgUnit1",
                                LongDescription = "OrgUnit 1.0",
                                Level = 1,
                                ParentId = null
                            },
                            OrgUnitId = 1,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        },
                        new Employment
                        {
                            Id = 2,
                            OrgUnit = new OrgUnit
                            {
                                Id = 2,
                                OrgId = 2,
                                ShortDescription = "orgUnit2",
                                LongDescription = "OrgUnit 2.1",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 2,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        },
                        new Employment
                        {
                            Id = 2,
                            OrgUnit = new OrgUnit
                            {
                                Id = 3,
                                OrgId = 2,
                                ShortDescription = "orgUnit2",
                                LongDescription = "OrgUnit 2.1",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 2,
                            IsLeader = false,
                            PersonId = 2,
                            Person = _persons.AsQueryable().ToList()[1]
                        },
                        new Employment
                        {
                            Id = 4,
                            OrgUnit = new OrgUnit
                            {
                                Id = 3,
                                OrgId = 3,
                                ShortDescription = "orgUnit3",
                                LongDescription = "OrgUnit 2.2",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 3,
                            IsLeader = true,
                            PersonId = 3,
                            Person = _persons.AsQueryable().ToList()[2]
                        }
            }.AsQueryable());

            var result = _uut.GetEmployeesOfLeader(_persons.AsQueryable().ToList()[2]);

            // Asserts
            Assert.AreEqual(1, result.Count);
            Assert.IsFalse(result.Contains(_persons.AsQueryable().ToList()[0]));
            Assert.IsFalse(result.Contains(_persons.AsQueryable().ToList()[1]));
            Assert.IsTrue(result.Contains(_persons.AsQueryable().ToList()[2]));
        }

        [Test]
        public void GetEmploymentForLeader_NotLeader()
        {
            _persons = new List<Person>{
                new Person
                {
                    Id = 3,
                    FirstName = "Jacob",
                    LastName = "Jensen",
                    CprNumber = "456456456",
                    Initials = "JOJ",
                    Employments = new List<Employment>
                    {
                        new Employment
                        {
                            Id = 4,
                            OrgUnit = new OrgUnit
                            {
                                Id = 3,
                                OrgId = 3,
                                ShortDescription = "orgUnit3",
                                LongDescription = "OrgUnit 2.2",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 3,
                            IsLeader = false
                        }
                    }
                }
            }.AsQueryable();

            _employmentsRepoMock.AsQueryable().ReturnsForAnyArgs(new List<Employment>
            {
                new Employment
                {
                    Id = 4,
                    OrgUnit = new OrgUnit
                    {
                        Id = 3,
                        OrgId = 3,
                        ShortDescription = "orgUnit3",
                        LongDescription = "OrgUnit 2.2",
                        Level = 2,
                        ParentId = 1
                    },
                    OrgUnitId = 3,
                    IsLeader = false,
                    PersonId = 3,
                    Person = _persons.AsQueryable().ToList()[0]
                }
            }.AsQueryable());

            var result = _uut.GetEmployeesOfLeader(_persons.AsQueryable().ToList()[0]);

            // Asserts
            Assert.IsEmpty(result);
        }

        [Test]
        public void GetEmploymentForLeader_NotLeader_ButSubstitute()
        {
            _persons = new List<Person>{
                new Person
                {
                    Id = 1,
                    FirstName = "Morten",
                    LastName = "Rasmussen",
                    CprNumber = "1234567890",
                    Initials = "MR",
                    Employments = new List<Employment>
                    {
                        new Employment
                        {
                            Id = 4,
                            OrgUnit = new OrgUnit
                            {
                                Id = 3,
                                OrgId = 3,
                                ShortDescription = "orgUnit3",
                                LongDescription = "OrgUnit 2.2",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 3,
                            IsLeader = true,
                            PersonId = 1,
                            Person = _persons.AsQueryable().ToList()[0]
                        }
                    }
                },
                new Person
                {
                    Id = 2,
                    FirstName = "Morten",
                    LastName = "Jørgensen",
                    CprNumber = "0987654321",
                    Initials = "MJ",
                    Employments = new List<Employment>
                    {
                        new Employment
                        {
                            Id = 4,
                            OrgUnit = new OrgUnit
                            {
                                Id = 3,
                                OrgId = 3,
                                ShortDescription = "orgUnit3",
                                LongDescription = "OrgUnit 2.2",
                                Level = 2,
                                ParentId = 1
                            },
                            OrgUnitId = 3,
                            IsLeader = false,
                            PersonId = 2,
                            Person = _persons.AsQueryable().ToList()[2]
                        }
                    },
                    SubstituteLeaders = new List<Core.DomainModel.Substitute>
                    {
                        new Core.DomainModel.Substitute
                        {
                            Leader = _persons.AsQueryable().ToList()[0]
                        }
                    }
                },
                new Person
                {
                    Id = 3,
                    FirstName = "Jacob",
                    LastName = "Jensen",
                    CprNumber = "456456456",
                    Initials = "JOJ",
                }
            }.AsQueryable();

            _employmentsRepoMock.AsQueryable().ReturnsForAnyArgs(new List<Employment>
            {
                new Employment
                {
                    Id = 1,
                    OrgUnit = new OrgUnit
                    {
                        Id = 1,
                        OrgId = 1,
                        ShortDescription = "orgUnit1",
                        LongDescription = "OrgUnit 1.0",
                        Level = 1,
                        ParentId = null
                    },
                    OrgUnitId = 1,
                    IsLeader = true,
                    PersonId = 1,
                    Person = _persons.AsQueryable().ToList()[0]
                },
                new Employment
                {
                    Id = 2,
                    OrgUnit = new OrgUnit
                    {
                        Id = 2,
                        OrgId = 2,
                        ShortDescription = "orgUnit2",
                        LongDescription = "OrgUnit 2.1",
                        Level = 2,
                        ParentId = 1
                    },
                    OrgUnitId = 2,
                    IsLeader = true,
                    PersonId = 1,
                    Person = _persons.AsQueryable().ToList()[0]
                },
                new Employment
                {
                    Id = 2,
                    OrgUnit = new OrgUnit
                    {
                        Id = 3,
                        OrgId = 2,
                        ShortDescription = "orgUnit2",
                        LongDescription = "OrgUnit 2.1",
                        Level = 2,
                        ParentId = 1
                    },
                    OrgUnitId = 2,
                    IsLeader = false,
                    PersonId = 2,
                    Person = _persons.AsQueryable().ToList()[1]
                },
                new Employment
                {
                    Id = 4,
                    OrgUnit = new OrgUnit
                    {
                        Id = 3,
                        OrgId = 3,
                        ShortDescription = "orgUnit3",
                        LongDescription = "OrgUnit 2.2",
                        Level = 2,
                        ParentId = 1
                    },
                    OrgUnitId = 3,
                    IsLeader = true,
                    PersonId = 3,
                    Person = _persons.AsQueryable().ToList()[2]
                }

            }.AsQueryable());
            _persons.AsQueryable().ToList()[1].SubstituteLeaders.ToList()[0].Leader = _persons.AsQueryable().ToList()[0];
            var result = _uut.GetEmployeesOfLeader(_persons.AsQueryable().ToList()[1]);

            // Asserts
            Assert.AreEqual(1, result.Count);
            Assert.IsFalse(result.Contains(_persons.AsQueryable().ToList()[0]));
            Assert.IsFalse(result.Contains(_persons.AsQueryable().ToList()[1]));
            Assert.IsTrue(result.Contains(_persons.AsQueryable().ToList()[2]));
        }
    }
}
