using Core.ApplicationServices.Logger;
using Core.DmzModel;
using Core.DomainServices;
using Core.DomainServices.Encryption;
using Infrastructure.DmzSync.Services.Impl;
using Infrastructure.DmzSync.Services.Interface;
using NSubstitute;
using NUnit.Framework;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace DmzSync.Test
{
    [TestFixture]
    public class GPSEncryptServiceTests
    {
        private IGPSEncryptService _gpsEncryptService;
        private List<Core.DmzModel.DriveReport> _dmzReportList = new List<Core.DmzModel.DriveReport>();
        private IGenericRepository<Core.DmzModel.DriveReport> _dmzRepoMock;
        private ILogger _logger;

        [SetUp]
        public void SetUp()
        {
            
            _dmzRepoMock = NSubstitute.Substitute.For<IGenericRepository<Core.DmzModel.DriveReport>>();
            _logger = NSubstitute.Substitute.For<ILogger>();

            _dmzRepoMock.WhenForAnyArgs(x => x.Delete(new Core.DmzModel.DriveReport())).Do(p => _dmzReportList.Remove(p.Arg<Core.DmzModel.DriveReport>()));
            _dmzRepoMock.WhenForAnyArgs(x => x.Insert(new Core.DmzModel.DriveReport())).Do(t => _dmzReportList.Add(t.Arg<Core.DmzModel.DriveReport>()));

            _dmzReportList = new List<DriveReport>()
            {
                new DriveReport()
                {
                    Id = 1,
                    Purpose = "Test",
                    StartsAtHome = false,
                    EndsAtHome = false,
                    ManualEntryRemark = "ManualEntry",
                    Date = "2015-05-27",
                    EmploymentId = 1,
                    ProfileId = 1,
                    RateId = 1,
                    Profile = new Profile()
                    {
                        FullName = "Test Testesen [TT]"
                    },
                    Route = new Route()
                    {
                        Id = 1,
                        GPSCoordinates = new List<GPSCoordinate>()
                        {
                            new GPSCoordinate()
                            {
                                Latitude = StringCipher.Encrypt("1", Encryptor.EncryptKey),
                                Longitude = StringCipher.Encrypt("1", Encryptor.EncryptKey),
                            },
                            new GPSCoordinate()
                            {
                                Latitude = StringCipher.Encrypt("2", Encryptor.EncryptKey),
                                Longitude = StringCipher.Encrypt("2", Encryptor.EncryptKey),
                            }
                        }
                    }
                },
                new DriveReport()
                {
                    Id = 2,
                    Purpose = "Test2",
                    StartsAtHome = true,
                    EndsAtHome = true,
                    ManualEntryRemark = "ManualEntry",
                    Date = "2015-05-26",
                    EmploymentId = 1,
                    ProfileId = 1,
                    RateId = 1,
                    Profile = new Profile()
                    {
                        FullName = "Test Testesen [TT]"
                    },
                    Route = new Route()
                    {
                        Id = 2,
                        GPSCoordinates = new List<GPSCoordinate>()
                        {
                            new GPSCoordinate()
                            {
                                Latitude = "57.0482206",
                                Longitude = "9.9193939",
                            },
                            new GPSCoordinate()
                            {
                                Latitude = "A",
                                Longitude = "9.9193939",
                            }
                        }
                    }
                }
            };

            _dmzRepoMock.AsQueryable().ReturnsForAnyArgs(_dmzReportList.AsQueryable());
            _gpsEncryptService = new GPSEncryptService(_dmzRepoMock, _logger);
        } 

        [Test]
        public void GPSEncrypt_ShouldEncrypCoordinate()
        {
            var items = _dmzRepoMock.AsQueryable().ToList();
            var coordinate = items[1].Route.GPSCoordinates.ToList()[0];
            var lat = coordinate.Latitude;
            var lng = coordinate.Longitude;

            _gpsEncryptService.DoGPSEncrypt();

            var itemsAfter = _dmzRepoMock.AsQueryable().ToList();
            var coordinateAfter = items[1].Route.GPSCoordinates.ToList()[0];

            Assert.AreNotEqual(coordinateAfter.Latitude, lat);
            Assert.AreNotEqual(coordinateAfter.Longitude, lng);
        }

        [Test]
        public void GPSEncrypt_ShouldSkipEncryptingCoordinate()
        {
            var items = _dmzRepoMock.AsQueryable().ToList();
            var coordinate = items[1].Route.GPSCoordinates.ToList()[1];
            var lat = coordinate.Latitude;
            var lng = coordinate.Longitude;

            _gpsEncryptService.DoGPSEncrypt();

            var itemsAfter = _dmzRepoMock.AsQueryable().ToList();
            var coordinateAfter = items[1].Route.GPSCoordinates.ToList()[1];

            Assert.AreEqual(coordinateAfter.Latitude, lat);
            Assert.AreEqual(coordinateAfter.Longitude, lng);
        }

        [Test]
        public void GPSEncrypt_EncryptAndDecryptShouldMatch()
        {
            var items = _dmzRepoMock.AsQueryable().ToList();
            var coordinate = items[1].Route.GPSCoordinates.ToList()[0];
            var lat = coordinate.Latitude;
            var lng = coordinate.Longitude;

            _gpsEncryptService.DoGPSEncrypt();

            var itemsAfter = _dmzRepoMock.AsQueryable().ToList();
            var coordinateAfter = items[1].Route.GPSCoordinates.ToList()[0];
            var latEncrypted = coordinateAfter.Latitude;
            var lngEncrypted = coordinateAfter.Longitude;

            var decrypted = Encryptor.DecryptGPSCoordinate(items[1].Route.GPSCoordinates.ToList()[0]);

            Assert.AreNotEqual(latEncrypted, lat);
            Assert.AreNotEqual(lngEncrypted, lng);
            Assert.AreEqual(decrypted.Latitude, lat);
            Assert.AreEqual(decrypted.Longitude, lng);
        }
    }
}
    
