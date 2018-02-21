using System;
using System.Linq;
using System.Threading;
using Core.ApplicationServices;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Ninject;
using Core.ApplicationServices.Logger;

namespace Mail
{
    public class Mailer
    {
        
        public static void Main(string[] args)
        {
            
            ILogger _logger = NinjectWebKernel.GetKernel().Get<ILogger>();
            _logger.Debug($"-------- MAIL STARTED --------");
            var service = NinjectWebKernel.GetKernel().Get<ConsoleMailerService>();
            service.RunMailService();
            _logger.Debug($"-------- MAIL FINISHED --------");
        }

        
    }
}
