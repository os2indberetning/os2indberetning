using Core.ApplicationServices.Logger;
using Core.DomainServices;
using Core.DomainServices.Encryption;
using Infrastructure.DmzSync.Services.Interface;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Infrastructure.DmzSync.Services.Impl
{
    public class GPSEncryptService : IGPSEncryptService
    {
        private IGenericRepository<Core.DmzModel.GPSCoordinate> _gpsCoordinatesRepo;

        private readonly ILogger _logger;

        public GPSEncryptService(IGenericRepository<Core.DmzModel.GPSCoordinate> gpsCoordinatesRepo, ILogger logger)
        {
            _gpsCoordinatesRepo = gpsCoordinatesRepo;
            _logger = logger;
        }

        public void DoGPSEncrypt()
        {
            try
            {
                var skip = 0;
                var takeIncrement = 10000;

                var max = _gpsCoordinatesRepo.AsQueryable().Count();

                while (skip < max)
                {
                    var take = (skip + takeIncrement) > max ? (max - skip) : takeIncrement;

                    var gpsCoordinates = _gpsCoordinatesRepo.AsQueryable()
                        .OrderBy(x => x.Id)
                        .Skip(skip)
                        .Take(take)
                        .ToList();

                    var decryptedGPSCoordinates = gpsCoordinates.Where(x =>
                        ValidateLatitude(x.Latitude) &&
                        ValidateLongitude(x.Longitude))
                        .ToList();

                    bool anyChanges = false;

                    if (decryptedGPSCoordinates.Any())
                    {
                        for (int i = 0; i < decryptedGPSCoordinates.Count(); i++)
                        {
                            var gpsCoordinate = decryptedGPSCoordinates.ElementAt(i);

                            try
                            {
                                gpsCoordinate = Encryptor.EncryptGPSCoordinate(gpsCoordinate);
                                anyChanges = true;
                            }
                            catch (Exception ex)
                            {
                                Console.WriteLine($"DoGPSEncrypt, failed to decrypt coordinate: {gpsCoordinate.Id} with exception: {ex}");
                                _logger.Error($"DoGPSEncrypt, failed to decrypt coordinate: {gpsCoordinate.Id}", ex);
                                throw;
                            }
                        }
                    }

                    if (anyChanges)
                    {
                        _gpsCoordinatesRepo.Save();
                        _logger.Debug($"GPSCoordinates between id: {gpsCoordinates.FirstOrDefault().Id} and id: {gpsCoordinates.LastOrDefault().Id} has been encrypted");
                    }
                    else
                    {
                        _logger.Debug($"GPSCoordinates between id: {gpsCoordinates.FirstOrDefault().Id} and id: {gpsCoordinates.LastOrDefault().Id} was already encrypted");
                    }

                    skip += take;
                }
            }
            catch (Exception)
            {
                throw;
            }
        }

        private bool ValidateLatitude(string latitude)
        {
            if (string.IsNullOrEmpty(latitude))
            {
                return false;
            }

            double d;
            bool isNumeric = double.TryParse(latitude, out d);
            if (!isNumeric)
            {
                return false;
            }

            if (d <= 90.0 && d >= -90.0)
            {
                return true;
            }

            return false;
        }

        private bool ValidateLongitude(string longitude)
        {
            if (string.IsNullOrEmpty(longitude))
            {
                return false;
            }

            double d;
            bool isNumeric = double.TryParse(longitude, out d);
            if (!isNumeric)
            {
                return false;
            }

            if (d <= 180.0 && d >= -180.0)
            {
                return true;
            }

            return false;
        }
    }
}
