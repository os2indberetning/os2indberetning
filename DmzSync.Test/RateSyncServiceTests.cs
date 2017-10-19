﻿using System;
using System.CodeDom;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading.Tasks;
using Core.DmzModel;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.Encryption;
using Infrastructure.DmzSync.Services.Impl;
using Infrastructure.DmzSync.Services.Interface;
using NSubstitute;
using NUnit.Framework;
using Rate = Core.DmzModel.Rate;
using Core.ApplicationServices.Logger;

namespace DmzSync.Test
{
    [TestFixture]
    public class RateSyncServiceTests
    {

        private ISyncService _uut;
        private IGenericRepository<Core.DomainModel.Rate> _masterRepoMock; 
        private IGenericRepository<Core.DmzModel.Rate> _dmzRepoMock;
        private List<Core.DmzModel.Rate> _dmzRateList;
        private List<Core.DomainModel.Rate> _masterRateList;

        [SetUp]
        public void SetUp()
        {
            _dmzRateList = new List<Rate>();
            _masterRateList = new List<Core.DomainModel.Rate>();
            _dmzRepoMock = NSubstitute.Substitute.For<IGenericRepository<Core.DmzModel.Rate>>();
            _masterRepoMock = NSubstitute.Substitute.For<IGenericRepository<Core.DomainModel.Rate>>();
            _dmzRepoMock.WhenForAnyArgs(x => x.Insert(new Core.DmzModel.Rate())).Do(p => _dmzRateList.Add(p.Arg<Core.DmzModel.Rate>()));
            ILogger _logger = new Logger();
            _uut = new RateSyncService(_dmzRepoMock,_masterRepoMock, _logger);
        }

        [Test]
        public void SyncFromDmz_ShouldThrow_NotImplemented()
        {
            Assert.Throws<NotImplementedException>(() => _uut.SyncFromDmz());
        }

        [Test]
        public void ClearDmz_ShouldCallDeleteRange()
        {
            Assert.Throws<NotImplementedException>(() => _uut.ClearDmz());
        }

        [Test]
        public void SyncToDmz_ShouldInsertIntoDmz()
        {
            _dmzRepoMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DmzModel.Rate>().AsQueryable());

            _masterRepoMock.AsQueryable().ReturnsForAnyArgs(new List<Core.DomainModel.Rate>()
            {
                new Core.DomainModel.Rate()
                {
                    Active = true,
                    Id = 1,
                    Year = DateTime.Now.Year,
                    Type = new RateType(){Description = "TEST"}
                },
                new Core.DomainModel.Rate()
                {
                    Active = true,
                    Id = 2,
                    Year = DateTime.Now.Year,
                    Type = new RateType(){Description = "TEST"}
                },
                new Core.DomainModel.Rate()
                {
                    Active = false,
                    Id = 3,
                    Year = DateTime.Now.AddYears(-1).Year,
                    Type = new RateType(){Description = "TEST"}
                }

            }.AsQueryable());

            _uut.SyncToDmz();
            Assert.AreEqual(2,_dmzRateList.Count);
        }
    }
}
