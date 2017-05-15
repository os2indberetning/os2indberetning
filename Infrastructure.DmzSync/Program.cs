using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Core.ApplicationServices;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.DmzModel;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using Infrastructure.AddressServices;
using Infrastructure.DmzDataAccess;
using Infrastructure.DataAccess;
using Core.DomainServices.Encryption;
using Infrastructure.DmzSync.Services.Impl;
using Ninject;
using DriveReport = Core.DmzModel.DriveReport;
using Rate = Core.DomainModel.Rate;


namespace Infrastructure.DmzSync
{
    class Program
    {
        static void Main(string[] args)
        {
           
            var logger = NinjectWebKernel.CreateKernel().Get<ILogger>();
       
            // hacks because of error with Entity Framework.
            // This forces the dmzconnection to use MySql.
            new DataContext();

            var personSync = new PersonSyncService(new GenericDmzRepository<Profile>(new DmzContext()),
                new GenericRepository<Person>(new DataContext()), new GenericDmzRepository<Core.DmzModel.Employment>(new DmzContext()),
                NinjectWebKernel.CreateKernel().Get<IPersonService>(), logger);

            var driveSync = new DriveReportSyncService(new GenericDmzRepository<DriveReport>(new DmzContext()),
               new GenericRepository<Core.DomainModel.DriveReport>(new DataContext()), new GenericRepository<Rate>(new DataContext()), new GenericRepository<LicensePlate>(new DataContext()), NinjectWebKernel.CreateKernel().Get<IDriveReportService>(), NinjectWebKernel.CreateKernel().Get<IRoute<RouteInformation>>(), NinjectWebKernel.CreateKernel().Get<IAddressCoordinates>(), NinjectWebKernel.CreateKernel().Get<IGenericRepository<Core.DomainModel.Employment>>(), logger);

            var rateSync = new RateSyncService(new GenericDmzRepository<Core.DmzModel.Rate>(new DmzContext()),
                new GenericRepository<Rate>(new DataContext()),logger);

            var orgUnitSync = new OrgUnitSyncService(new GenericDmzRepository<Core.DmzModel.OrgUnit>(new DmzContext()),
                new GenericRepository<Core.DomainModel.OrgUnit>(new DataContext()),logger);

            var userAuthSync = new UserAuthSyncService(new GenericRepository<Core.DomainModel.AppLogin>(new DataContext()), 
                new GenericDmzRepository<Core.DmzModel.UserAuth>(new DmzContext()),logger);

            logger.Debug("-------- DMZSYNC STARTED --------");

            try
            {
                logger.Debug("SyncFromDMZ started");
                Console.WriteLine("DriveReportsSyncFromDmz");
                driveSync.SyncFromDmz();

            }
            catch (Exception ex)
            {
                logger.Error($"Error during drivereport synchronization from DMZ", ex);
                logger.LogForAdmin("Fejl under synkronisering af indberetninger fra DMZ. Mobilindberetninger er ikke synkroniserede.");
                throw;
            }

            try
            {
                logger.Debug("OrgUnitSyncToDmz started");
                Console.WriteLine("OrgUnitSyncToDmz");
                orgUnitSync.SyncToDmz();
            }
            catch (Exception ex)
            {
                logger.Error($"Error during orgunit synchronization to DMZ", ex);
                logger.LogForAdmin("Fejl ved synkronisering af organisationsenheder til DMZ.");
                throw;
            }

            try
            {
                logger.Debug("PersonSyncToDmz started");
                Console.WriteLine("PersonSyncToDmz");
                personSync.SyncToDmz();

            }
            catch (Exception ex)
            {
                logger.Error($"Error during people synchronization to DMZ", ex);
                logger.LogForAdmin("Fejl ved synkronisering af medarbejdere til DMZ.");
                throw;
            }

            try
            {
                logger.Debug("RateSyncToDmz started");
                Console.WriteLine("RateSyncToDmz");
                rateSync.SyncToDmz();
            }
            catch (Exception ex)
            {
                logger.Error($"Error during rate synchronization from DMZ", ex);
                logger.LogForAdmin("Fejl ved synkronisering af takster til DMZ.");
                throw;
            }

            try
            {
                logger.Debug("UserAuthSyncToDmz started");
                Console.WriteLine("UserAuthSyncToDmz");
                userAuthSync.SyncToDmz();
            }
            catch (Exception ex)
            {
                logger.Error($"Error during userauth synchronization from DMZ", ex);
                logger.LogForAdmin("Fejl ved synkronisering af app-logins til DMZ. Nogle brugere vil muligvis ikke kunne logge på app.");
                throw;
            }

            logger.Debug("-------- DMZSYNC FINISHED --------");
            Console.WriteLine("Done");
        }
    }
}
