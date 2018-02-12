﻿using System;
using System.Collections.Generic;
using System.Linq;
using Core.ApplicationServices.Interfaces;
using Core.DmzModel;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.Encryption;
using Infrastructure.DmzSync.Services.Impl;
using Infrastructure.DmzSync.Services.Interface;
using NSubstitute;
using NUnit.Framework;
using Employment = Core.DomainModel.Employment;
using Core.ApplicationServices.Logger;

namespace DmzSync.Test
{
    [TestFixture]
    public class PersonSyncServiceTests
    {

        private ISyncService _uut;
        private IGenericRepository<Person> _masterRepoMock;
        private IGenericRepository<Core.DmzModel.Employment> _masterEmploymentMock;
        private IGenericRepository<Profile> _dmzRepoMock;
        private List<Profile> _dmzProfileList = new List<Profile>();
        private List<Person> _masterPersonList = new List<Person>();
        private IPersonService _personServiceMock;

        [SetUp]
        public void SetUp()
        {
            _dmzRepoMock = NSubstitute.Substitute.For<IGenericRepository<Profile>>();
            _masterRepoMock = NSubstitute.Substitute.For<IGenericRepository<Person>>();
            _personServiceMock = NSubstitute.Substitute.For<IPersonService>();
            _masterEmploymentMock = NSubstitute.Substitute.For<IGenericRepository<Core.DmzModel.Employment>>();

            _dmzRepoMock.WhenForAnyArgs(x => x.Delete(new Profile())).Do(p => _dmzProfileList.Remove(p.Arg<Profile>()));

            _personServiceMock.GetHomeAddress(new Person()).ReturnsForAnyArgs(new PersonalAddress()
            {
                Latitude = "1",
                Longitude = "2"
            });

            _dmzRepoMock.WhenForAnyArgs(x => x.Insert(new Profile())).Do(t => _dmzProfileList.Add(t.Arg<Profile>()));

            _dmzProfileList = new List<Profile>();
            _masterPersonList = new List<Person>()
            {
                new Person()
                {
                    Id = 1,
                    IsActive = true,
                    FirstName = "Test",
                    LastName = "Testesen",
                    Initials = "TT",
                    FullName = "Test Testesen [TT]",
                    Employments = new List<Employment>()
                    {
                        new Employment()
                        {
                            Id = 1,
                            EmploymentId = "1",
                            PersonId = 1,
                            Position = "Tester",
                            OrgUnit = new Core.DomainModel.OrgUnit()
                            {
                                LongDescription = "IT Minds"
                            }
                        }
                    }
                },
                new Person()
                {
                    Id = 2,
                    FirstName = "Lars",
                    IsActive = true,
                    LastName = "Testesen",
                    Initials = "LT",
                    FullName = "Lars Testesen [LT]",
                    Employments = new List<Employment>()
                    {
                        new Employment()
                        {
                            Id = 1,
                            EmploymentId = "2",
                            PersonId = 2,
                            Position = "Tester2",
                            OrgUnit = new Core.DomainModel.OrgUnit()
                            {
                                LongDescription = "IT Minds"
                            }
                        }
                    }
                },
                new Person()
                {
                    Id = 3,
                   IsActive = true,
                    FirstName = "Preben",
                    LastName = "Testesen",
                    Initials = "PT",
                    FullName = "Preben Testesen [PT]",
                    Employments = new List<Employment>()
                    {
                        new Employment()
                        {
                            Id = 1,
                            EmploymentId = "3",
                            PersonId = 3,
                            Position = "Tester3",
                            OrgUnit = new Core.DomainModel.OrgUnit()
                            {
                                LongDescription = "IT Minds"
                            }
                        }
                    }
                }
            };


            _masterRepoMock.AsQueryable().ReturnsForAnyArgs(_masterPersonList.AsQueryable());
            _dmzRepoMock.AsQueryable().ReturnsForAnyArgs(_dmzProfileList.AsQueryable());

            ILogger _logger = new Logger();
            _uut = new PersonSyncService(_dmzRepoMock, _masterRepoMock, _masterEmploymentMock, _personServiceMock, _logger);
        }

        [Test]
        public void ClearDmz_ShouldCallDeleteRange()
        {
            Assert.Throws<NotImplementedException>(() => _uut.ClearDmz());
        }

        [Test]
        public void SyncFromDmz_ShouldThrow_NotImplemented()
        {
            Assert.Throws<NotImplementedException>(() => _uut.SyncFromDmz());
        }

        [Test]
        public void SyncToDmz_ShouldCreateProfilesInDmz()
        {
            Assert.AreEqual(0, _dmzProfileList.Count);
            _uut.SyncToDmz();
            Assert.AreEqual(3, _dmzProfileList.Count);
        }

        [Test]
        public void SyncToDmz_ShouldSetEmploymentsCorrectly()
        {
            _uut.SyncToDmz();
            Assert.AreEqual(StringCipher.Encrypt("Tester - IT Minds", Encryptor.EncryptKey), _dmzProfileList.ElementAt(0).Employments.ElementAt(0).EmploymentPosition);
            Assert.AreEqual(StringCipher.Encrypt("Tester2 - IT Minds", Encryptor.EncryptKey), _dmzProfileList.ElementAt(1).Employments.ElementAt(0).EmploymentPosition);
            Assert.AreEqual(StringCipher.Encrypt("Tester3 - IT Minds", Encryptor.EncryptKey), _dmzProfileList.ElementAt(2).Employments.ElementAt(0).EmploymentPosition);
        }

        [Test]
        public void SyncToDmz_ShouldSetFullNameCorrectly()
        {
            _uut.SyncToDmz();
            Assert.AreEqual(StringCipher.Encrypt("Test Testesen [TT]", Encryptor.EncryptKey), _dmzProfileList.ElementAt(0).FullName);
            Assert.AreEqual(StringCipher.Encrypt("Lars Testesen [LT]", Encryptor.EncryptKey), _dmzProfileList.ElementAt(1).FullName);
            Assert.AreEqual(StringCipher.Encrypt("Preben Testesen [PT]", Encryptor.EncryptKey), _dmzProfileList.ElementAt(2).FullName);

        }

    }
}
