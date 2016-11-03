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

            try
            {
                logger.Log("SyncFromDMZ Initial", "dmz", 3);
                Console.WriteLine("DriveReportsSyncFromDmz");
                driveSync.SyncFromDmz();

            }
            catch (Exception ex)
            {
                logger.Log($"Class DmzSync. Main() Error during drivereport synchronization from DMZ. Method: driveSync.SyncFromDmz()", "dmz", ex, 1);
                throw;
            }

            try
            {
                logger.Log("OrgUnitSyncToDmz Initial", "dmz", 3);
                Console.WriteLine("OrgUnitSyncToDmz");
                orgUnitSync.SyncToDmz();
            }
            catch (Exception ex)
            {
                logger.Log($"Class DmzSync. Main() Error during orgunit synchronization from DMZ. Method: orgUnitSync.SyncToDmz()", "dmz", ex, 1);
                throw;
            }

            try
            {
                logger.Log("PersonSyncToDmz Initial", "dmz", 3);
                Console.WriteLine("PersonSyncToDmz");
                personSync.SyncToDmz();

            }
            catch (Exception ex)
            {
                logger.Log($"Class DmzSync. Main() Error during people synchronization from DMZ. Method: personSync.SyncToDmz()", "dmz", ex, 1);
                throw;
            }

            try
            {
                logger.Log("RateSyncToDmz Initial", "dmz", 3);
                Console.WriteLine("RateSyncToDmz");
                rateSync.SyncToDmz();
            }
            catch (Exception ex)
            {
                logger.Log($"Class DmzSync. Main() Error during rate synchronization from DMZ. Method:  rateSync.SyncToDmz()", "dmz", ex, 1);
                throw;
            }

            try
            {
                logger.Log("UserAuthSyncToDmz Initial", "dmz", 3);
                Console.WriteLine("UserAuthSyncToDmz");
                userAuthSync.SyncToDmz();
            }
            catch (Exception ex)
            {
                logger.Log($"Class DmzSync. Main() Error during userauth synchronization from DMZ. Method:  userAuthSync.SyncToDmz()", "dmz", ex, 1);
                throw;
            }

            logger.Log("DMZ sync done", "dmz", 3);
            Console.WriteLine("Done");



        }
    }
}
