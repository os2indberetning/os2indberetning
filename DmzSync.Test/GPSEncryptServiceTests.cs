using System.Collections.Generic;
using System.Linq;
using Core.ApplicationServices.Logger;
using Core.DmzModel;
using Core.DomainServices;
using Core.DomainServices.Encryption;
using Infrastructure.DMZGPSEncrypt.Services;
using NSubstitute;
using NUnit.Framework;

namespace DmzSync.Test
{
    [TestFixture]
    public class GPSEncryptServiceTests
    {
        private GPSEncryptService _gpsEncryptService;
        private List<Core.DmzModel.GPSCoordinate> _dmzGPSList = new List<Core.DmzModel.GPSCoordinate>();
        private IGenericRepository<Core.DmzModel.GPSCoordinate> _dmzGPSRepoMock;
        private ILogger _logger;

        [SetUp]
        public void SetUp()
        {            
            _dmzGPSRepoMock = NSubstitute.Substitute.For<IGenericRepository<Core.DmzModel.GPSCoordinate>>();
            _logger = NSubstitute.Substitute.For<ILogger>();

            _dmzGPSRepoMock.WhenForAnyArgs(x => x.Delete(new Core.DmzModel.GPSCoordinate())).Do(p => _dmzGPSList.Remove(p.Arg<Core.DmzModel.GPSCoordinate>()));
            _dmzGPSRepoMock.WhenForAnyArgs(x => x.Insert(new Core.DmzModel.GPSCoordinate())).Do(t => _dmzGPSList.Add(t.Arg<Core.DmzModel.GPSCoordinate>()));


            _dmzGPSList = new List<GPSCoordinate>()
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
                },
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
            };

            _dmzGPSRepoMock.AsQueryable().ReturnsForAnyArgs(_dmzGPSList.AsQueryable());
            _gpsEncryptService = new GPSEncryptService(_logger);
        } 

        //[Test]
        //public void GPSEncrypt_ShouldEncrypCoordinate()
        //{
        //    var items = _dmzGPSRepoMock.AsQueryable().ToList();
        //    var coordinate = items[2];
        //    var lat = coordinate.Latitude;
        //    var lng = coordinate.Longitude;

        //    _gpsEncryptService.DoGPSEncrypt();

        //    var itemsAfter = _dmzGPSRepoMock.AsQueryable().ToList();
        //    var coordinateAfter = itemsAfter[2];

        //    Assert.AreNotEqual(coordinateAfter.Latitude, lat);
        //    Assert.AreNotEqual(coordinateAfter.Longitude, lng);
        //}

        //[Test]
        //public void GPSEncrypt_ShouldSkipEncryptingCoordinate()
        //{
        //    var items = _dmzGPSRepoMock.AsQueryable().ToList();
        //    var coordinate = items[1];
        //    var lat = coordinate.Latitude;
        //    var lng = coordinate.Longitude;

        //    _gpsEncryptService.DoGPSEncrypt();

        //    var itemsAfter = _dmzGPSRepoMock.AsQueryable().ToList();
        //    var coordinateAfter = itemsAfter[1];

        //    Assert.AreEqual(coordinateAfter.Latitude, lat);
        //    Assert.AreEqual(coordinateAfter.Longitude, lng);
        //}

        //[Test]
        //public void GPSEncrypt_EncryptAndDecryptShouldMatch()
        //{
        //    var items = _dmzGPSRepoMock.AsQueryable().ToList();
        //    var coordinate = items[2];
        //    var lat = coordinate.Latitude;
        //    var lng = coordinate.Longitude;

        //    _gpsEncryptService.DoGPSEncrypt();

        //    var itemsAfter = _dmzGPSRepoMock.AsQueryable().ToList();
        //    var coordinateAfter = itemsAfter[2];
        //    var latEncrypted = coordinateAfter.Latitude;
        //    var lngEncrypted = coordinateAfter.Longitude;

        //    var decrypted = Encryptor.DecryptGPSCoordinate(items[2]);

        //    Assert.AreNotEqual(latEncrypted, lat);
        //    Assert.AreNotEqual(lngEncrypted, lng);
        //    Assert.AreEqual(decrypted.Latitude, lat);
        //    Assert.AreEqual(decrypted.Longitude, lng);
        //}
    }
}
    
