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

        public TransferToPayrollService(IReportGenerator reportGenerator, IGenericRepository<DriveReport> driveReportRepo, ILogger logger)
        {
            _reportGenerator = reportGenerator;
            _driveReportRepo = driveReportRepo;
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
            SdWebService.KoerselOpret20120201OperationRequest operationRequest;
            SdWebService.KoerselOpret20120201PortTypeClient portTypeClient;
            int countFailed = 0;
            int countSucces = 0;

            try
            {
                operationRequest = new SdWebService.KoerselOpret20120201OperationRequest();
                portTypeClient = new SdWebService.KoerselOpret20120201PortTypeClient();
                portTypeClient.ClientCredentials.UserName.UserName = ConfigurationManager.AppSettings["PROTECTED_SDUserName"] ?? "";
                portTypeClient.ClientCredentials.UserName.Password = ConfigurationManager.AppSettings["PROTECTED_SDUserPassword"] ?? "";

                operationRequest.InddataStruktur = new SdWebService.KoerselOpretRequestType();
            }
            catch (Exception e)
            {
                _logger.Error($"{this.GetType().ToString()}, sendDataToSd(), error when initating SD client", e);
                throw e;
            }

            var reports = _driveReportRepo.AsQueryable().Where(x => x.Status == ReportStatus.Accepted).ToList();
            _logger.Error($"{this.GetType().ToString()}, SendDataToSDWebservice(), Number of reports to send: {reports.Count}");

            foreach (var report in reports)
            {
                double koerselDato = report.DriveDateTimestamp;
                System.DateTime KoerseldateTime = new System.DateTime(1970, 1, 1, 0, 0, 0, 0);
                KoerseldateTime = KoerseldateTime.AddSeconds(koerselDato);

                try
                {
                    operationRequest.InddataStruktur.AarsagTekst = report.Purpose;
                    operationRequest.InddataStruktur.AnsaettelseIdentifikator = report.Employment.EmploymentId;
                    operationRequest.InddataStruktur.InstitutionIdentifikator = ConfigurationManager.AppSettings["PROTECTED_institutionNumber"] ?? "";
                    operationRequest.InddataStruktur.PersonnummerIdentifikator = report.Person.CprNumber;
                    operationRequest.InddataStruktur.RegistreringTypeIdentifikator = report.TFCode;
                    operationRequest.InddataStruktur.KoerselDato = KoerseldateTime.Date;
                    operationRequest.InddataStruktur.KilometerMaal = Convert.ToDecimal(report.Distance);

                    var startPoint = report.DriveReportPoints.Where(d => d.DriveReportId == report.Id && d.PreviousPointId == null && d.NextPointId != null).FirstOrDefault();
                    if (startPoint != null)
                    {
                        operationRequest.InddataStruktur.KoertFraTekst = startPoint.StreetName + ", " + startPoint.StreetNumber + ", " + startPoint.ZipCode + ", " + startPoint.Town;
                    }
                    var endpoint = report.DriveReportPoints.Where(d => d.DriveReportId == report.Id && d.PreviousPointId == null && d.NextPointId != null).FirstOrDefault();

                    if (endpoint != null)
                    {
                        operationRequest.InddataStruktur.KoertTilTekst = endpoint.StreetName + ", " + endpoint.StreetNumber + ", " + endpoint.ZipCode + ", " + endpoint.Town;
                    }
                    operationRequest.InddataStruktur.Regel60DageIndikator = false;

                    // Send data to SD

                    var response = portTypeClient.KoerselOpret20120201Operation(operationRequest.InddataStruktur);

                    report.Status = ReportStatus.Invoiced;

                    var epoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
                    var deltaTime = DateTime.Now.ToUniversalTime() - epoch;
                    report.ProcessedDateTimestamp = (long)deltaTime.TotalSeconds;

                    _driveReportRepo.Save();

                    countSucces++;

                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().ToString()}, sendDataToSd(), error when sending data, Servicenummer = {report.Employment.EmploymentId}, EmploymentId = {report.EmploymentId}, Kørselsdato = {KoerseldateTime.Date}", e);
                    _logger.Error($"Fejl for medarbejder: Servicenummer = {report.Employment.EmploymentId}, Kørselsdato = {KoerseldateTime.Date} --- Fejlbesked fra SD server: {e.Message}");
                    countFailed++;
                }
            }
            _logger.Debug($"----- Afsendelse afsluttet. {countFailed} ud af {countFailed + countSucces} afsendelser fejlede -----");
        }
    }
}
