using System;
using System.Linq;
using Core.ApplicationServices;
using Core.ApplicationServices.Logger;
using Core.DmzModel;
using Infrastructure.DataAccess;
using Infrastructure.DmzDataAccess;
using Infrastructure.DMZGPSEncrypt.Services;
using Ninject;

namespace DMZGPSEncrypt
{
    class MainClass
    {
        public static void Main(string[] args)
        {
            var kernel = NinjectWebKernel.GetKernel();

            var x = kernel.GetBindings(typeof(DataContext)).FirstOrDefault();
            kernel.RemoveBinding(x);

            kernel.Bind<DataContext>().ToSelf().InSingletonScope(); // we need to use a single dbcontext

            var logger = kernel.Get<ILogger>();

            // hacks because of error with Entity Framework.
            // This forces the dmzconnection to use MySql.
            new DataContext();

            var gpsEncryptService = new GPSEncryptService(
                new GenericDmzRepository<GPSCoordinate>(new DmzContext()),
                logger);

            logger.Debug("-------- DMZ GPS Encrypt STARTED --------");

            try
            {
                logger.Debug("DoGPSEncrypt started");
                Console.WriteLine("DoGPSEncrypt");
                gpsEncryptService.DoGPSEncrypt();
            }
            catch (Exception ex)
            {
                logger.Error($"Error during encrypting geocoordinates on DMZ", ex);
                logger.LogForAdmin("Fejl under kryptering af geo koodinater i DMZ.");
                throw;
            }

            logger.Debug("-------- DMZ GPS Encrypt ENDED --------");
        }
    }
}
