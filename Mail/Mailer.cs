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
            
            ILogger _logger = NinjectWebKernel.CreateKernel().Get<ILogger>();
            _logger.Log($"************* Mail started ***************", "mail", 3);
            var service = NinjectWebKernel.CreateKernel().Get<ConsoleMailerService>();
            service.RunMailService();
            _logger.Log($"************* Mail ended ***************", "mail", 3);
        }

        
    }
}
