using System;
using System.Linq;
using System.Threading;
using Core.ApplicationServices;
using Ninject;
using Core.ApplicationServices.Logger;

namespace FileGenerationScheduler
{
    public class FileGenerator
    {
        static void Main(string[] args)
        {
            // Checks if any files need to be generated and transfered to the payroll system daily at kl 16:00
            ILogger _logger = NinjectWebKernel.GetKernel().Get<ILogger>();
            _logger.Debug($"-------- FILE GENERATIONS STARTED --------");
            var service = NinjectWebKernel.GetKernel().Get<FileGenerationService>();
            service.RunFileGenerationService();
            _logger.Debug($"-------- FILE GENERATION FINISHED --------");
            
        }
    }
}
