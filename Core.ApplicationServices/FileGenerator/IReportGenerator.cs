using Core.DomainModel;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.ApplicationServices.FileGenerator
{
    public interface IReportGenerator
    {
        void WriteRecordsToFileAndAlterReportStatus();

        List<DriveReport> ReceiveReportsToInvoiceSD();
    }
}
