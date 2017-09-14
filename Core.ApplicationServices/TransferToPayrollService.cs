using Core.ApplicationServices.FileGenerator;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.ApplicationServices
{
    public class TransferToPayrollService : ITransferToPayrollService
    {
        private readonly IReportGenerator _reportGenerator;
        private readonly IGenericRepository<DriveReport> _driveReportRepo;
        private readonly ILogger _logger;

        public TransferToPayrollService(IReportGenerator reportGenerator, ILogger logger)
        {
            _reportGenerator = reportGenerator;
            _logger = logger;
        }

        public void TransferReportsToPayroll()
        {
            bool useSdAsIntegration = false; // use KMD as defualt, since that's currently what most use.
            var parseResult = bool.TryParse(ConfigurationManager.AppSettings["UseSd"], out useSdAsIntegration);
            _logger.Debug($"{GetType().Name}, TransferReportsToPayroll(), UseSd configuration = {useSdAsIntegration}");
            if (useSdAsIntegration)
            {
                SendDataToSDWebservice();
            }
            else
            {
                GenerateFileForKMD();
            }
        }

        private void GenerateFileForKMD()
        {
            _reportGenerator.WriteRecordsToFileAndAlterReportStatus();
        }

        private void SendDataToSDWebservice()
        {

        }
    }
}
