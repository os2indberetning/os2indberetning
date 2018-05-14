using System;
using System.Linq;
using System.Threading;
using Core.ApplicationServices;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Ninject;
using Core.ApplicationServices.Logger;
using Infrastructure.DataAccess;

namespace Mail
{
    public class Mailer
    {
        
        public static void Main(string[] args)
        {
            var kernel = NinjectWebKernel.GetKernel();

            var x = kernel.GetBindings(typeof(DataContext)).FirstOrDefault();
            kernel.RemoveBinding(x);

            kernel.Bind<DataContext>().ToSelf().InSingletonScope(); // we need to use a single dbcontext

            ILogger _logger = kernel.Get<ILogger>();
            _logger.Debug($"-------- MAIL STARTED --------");
            var service = kernel.Get<ConsoleMailerService>();
            service.UpdateResponsibleLeaders();
            service.RunMailService();
            _logger.Debug($"-------- MAIL FINISHED --------");
        }

        
    }
}
