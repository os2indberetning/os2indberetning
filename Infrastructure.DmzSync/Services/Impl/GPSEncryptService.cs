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
        private IGenericRepository<Core.DmzModel.DriveReport> _dmzDriveReportRepo;
        private readonly ILogger _logger;

        public GPSEncryptService(IGenericRepository<Core.DmzModel.DriveReport> dmzDriveReportRepo, ILogger logger)
        {
            _dmzDriveReportRepo = dmzDriveReportRepo;
            _logger = logger;
        }

        public void DoGPSEncrypt()
        {
            try
            {
                bool anyChanges = false;
                var reports = _dmzDriveReportRepo.AsQueryable().ToList(); 
                foreach (var report in reports)
                {
                    for (int i = 0; i < report.Route.GPSCoordinates.Count(); i++)
                    {
                        var gpsCoordinate = report.Route.GPSCoordinates.ElementAt(i);
                        if (IsCoordinateDecrypted(gpsCoordinate.Latitude) && IsCoordinateDecrypted(gpsCoordinate.Longitude))
                        {
                            gpsCoordinate = Encryptor.EncryptGPSCoordinate(gpsCoordinate);
                            anyChanges = true;
                        }
                    }
                }
                if (anyChanges)
                {
                    _dmzDriveReportRepo.Save();
                    _logger.Debug("GPSCoordinates has been encrypted");
                }
                else
                {
                    _logger.Debug("All GPSCoordinates is already encrypted");
                }
            }
            catch (Exception)
            {
                throw;
            }
        }

        private bool IsCoordinateDecrypted(string latOrLng)
        {
            if (String.IsNullOrEmpty(latOrLng))
            {
                return false;
            }

            var array = latOrLng.Split('.');
            if (array.Count() == 2)
            {
                double nOne;
                bool isNumericFirst = double.TryParse(array[0], out nOne);
                if (!isNumericFirst)
                {
                    return false;
                }
                double nTwo;
                bool isNumericSecond = double.TryParse(array[1], out nTwo);
                if (!isNumericSecond)
                {
                    return false;
                }

                return true;
            }

            return false;
        }
    }
}
