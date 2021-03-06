using System;
using System.ComponentModel;
using System.Reflection;
using System.Web;
using System.Web.Http;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.MailerService.Impl;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using Infrastructure.AddressServices;
using Infrastructure.AddressServices.Routing;
using Infrastructure.DataAccess;
using Infrastructure.DmzDataAccess;
using Ninject;
using Ninject.Web.Common;
using OS2Indberetning;
using Core.ApplicationServices.FileGenerator;
using Core.ApplicationServices.SilkeborgData;
using Core.DomainServices.Interfaces;
using Infrastructure.AddressServices.Interfaces;

namespace Core.ApplicationServices
{
    public static class NinjectWebKernel 
    {
        private static IKernel _currentInstance;

        public static IKernel GetKernel(bool isWebProject = false)
        {
            if(_currentInstance == null)
            {
                CreateKernel(isWebProject);
            }
            return _currentInstance;
        }

        /// <summary>
        /// Creates the kernel that will manage your application.
        /// </summary>
        /// <param name="getInjections"></param>
        /// <returns>The created kernel.</returns>
        private static void CreateKernel(bool isWebProject)
        {
            var kernel = new StandardKernel();
            kernel.Load(Assembly.GetExecutingAssembly());
            try
            {
                kernel.Bind<Func<IKernel>>().ToMethod(ctx => () => new Bootstrapper().Kernel);
                kernel.Bind<IHttpModule>().To<HttpApplicationInitializationHttpModule>();

                RegisterServices(kernel, isWebProject);

                // Install our Ninject-based IDependencyResolver into the Web API config
                GlobalConfiguration.Configuration.DependencyResolver = new NinjectDependencyResolver(kernel);

                _currentInstance = kernel;
            }
            catch
            {
                kernel.Dispose();
                throw;
            }
        }

        /// <summary>
        /// Load your modules or register your services here!
        /// </summary>
        /// <param name="kernel">The kernel.</param>
        public static void RegisterServices(IKernel kernel, bool isWebProject)
        {
            if (isWebProject)
            {
                kernel.Bind<DataContext>().ToSelf().InRequestScope();
            }
            else
            {
                kernel.Bind<DataContext>().ToSelf().InSingletonScope();
            }

            // Core
            // DomainServices
            kernel.Bind(typeof (IGenericRepository<>)).To(typeof (GenericRepository<>));

            kernel.Bind<IAddressCoordinates>().To<AddressCoordinates>();
            kernel.Bind<IRoute<RouteInformation>>().To<BestRoute>();
            kernel.Bind<IAddressLaunderer>().To<AddressLaundering>();
            kernel.Bind<IUrlDefinitions>().To<UrlDefinitions>();

            kernel.Bind<ICustomSettings>().To<CustomSettings>();

            // ApplicationServices
            kernel.Bind<IPersonService>().To<PersonService>();
            kernel.Bind<IMobileTokenService>().To<MobileTokenService>();
            kernel.Bind<ISubstituteService>().To<SubstituteService>();
            kernel.Bind<IDriveReportService>().To<DriveReportService>();
            kernel.Bind<ILicensePlateService>().To<LicensePlateService>();
            kernel.Bind<IPersonalRouteService>().To<PersonalRouteService>();
            kernel.Bind<IAppLoginService>().To<AppLoginService>();
            kernel.Bind<IOrgUnitService>().To<OrgUnitService>();
            kernel.Bind<ITransferToPayrollService>().To<TransferToPayrollService>();

            kernel.Bind<IReimbursementCalculator>().To<ReimbursementCalculator>();

            kernel.Bind<IMailSender>().To<MailSender>();
            kernel.Bind<IMailService>().To<MailService>();

            kernel.Bind<ISdClient>().To<SdClient>();
            kernel.Bind<IReportGenerator>().To<ReportGenerator>();
            kernel.Bind<IReportFileWriter>().To<ReportFileWriter>();

            kernel.Bind<ILogger>().To<Logger.Logger>();

            // Infrastructure
            kernel.Bind<IRouter>().To<SeptimaRouter>();

            // DBUpdater
        }        
    }
}
