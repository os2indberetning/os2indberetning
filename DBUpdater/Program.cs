using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Entity.Migrations.Model;
using System.Data.SqlClient;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Core.ApplicationServices;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using DBUpdater.Models;
using Infrastructure.AddressServices;
using Infrastructure.AddressServices.Interfaces;
using Infrastructure.DataAccess;
using Ninject;
using IAddressCoordinates = Core.DomainServices.IAddressCoordinates;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using System.Configuration;

namespace DBUpdater
{
    static class Program
    {
        static void Main(string[] args)
        {

            var ninjectKernel = NinjectWebKernel.CreateKernel();

            ILogger _logger = NinjectWebKernel.CreateKernel().Get<ILogger>();

            _logger.Debug($"-------- DBUPDATER STARTED --------");

            IAddressHistoryService historyService = new AddressHistoryService(ninjectKernel.Get<IGenericRepository<Employment>>(), ninjectKernel.Get<IGenericRepository<AddressHistory>>(), ninjectKernel.Get<IGenericRepository<PersonalAddress>>());

            var service = new UpdateService(ninjectKernel.Get<IGenericRepository<Employment>>(),
                ninjectKernel.Get<IGenericRepository<OrgUnit>>(),
                ninjectKernel.Get<IGenericRepository<Person>>(),
                ninjectKernel.Get<IGenericRepository<CachedAddress>>(),
                ninjectKernel.Get<IGenericRepository<PersonalAddress>>(),
                ninjectKernel.Get<IAddressLaunderer>(),
                ninjectKernel.Get<IAddressCoordinates>(), new DataProvider(),
                ninjectKernel.Get<IMailService>(),
                historyService,
                ninjectKernel.Get<IGenericRepository<DriveReport>>(),
                ninjectKernel.Get<IDriveReportService>(),
                ninjectKernel.Get<ISubstituteService>(),
                ninjectKernel.Get<IGenericRepository<Substitute>>());

            var dbSync = ConfigurationManager.AppSettings["DATABASE_INTEGRATION"] ?? "SOFD";
            _logger.Debug($"Database integration = {dbSync}");

            switch (dbSync)
            {
                case "IDM":
                    service.MigrateOrganisationsIDM();
                    service.MigrateEmployeesIDM();
                    break;
                case "SOFD":
                    service.MigrateOrganisations();
                    service.MigrateEmployees();
                    break;
                default:
                    _logger.Error("Could not read database integration type, check CustomSettings.config. DBUpdater will NOT run.");
                    return;
            }

            historyService.UpdateAddressHistories();
            historyService.CreateNonExistingHistories();
            service.UpdateLeadersOnExpiredOrActivatedSubstitutes();
            service.AddLeadersToReportsThatHaveNone();
            _logger.Debug($"-------- DBUPDATER FINISHED --------");
        }

    }

}

