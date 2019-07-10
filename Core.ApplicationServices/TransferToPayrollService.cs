using Core.ApplicationServices.FileGenerator;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.SilkeborgData;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.Interfaces;
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
        private readonly ISdClient _sdClient;
        private readonly ILogger _logger;
        private readonly ICustomSettings _customSettings;

        public TransferToPayrollService(IReportGenerator reportGenerator, IGenericRepository<DriveReport> driveReportRepo, ISdClient sdClient, ILogger logger, ICustomSettings customSettings)
        {
            _reportGenerator = reportGenerator;
            _driveReportRepo = driveReportRepo;
            _sdClient = sdClient;
            _logger = logger;
            _customSettings = customSettings;
        }

        public void TransferReportsToPayroll()
        {
            _logger.Debug($"{GetType().Name}, TransferReportsToPayroll(), UseSd configuration = {_customSettings.SdIsEnabled}");
            if (_customSettings.SdIsEnabled)
            {
                SendDataToSD();
            }
            else
            {
                GenerateFileForKMD();
            }
            _logger.Debug($"{GetType().Name}, TransferReportsToPayroll(), Transfer finished");
        }

        private void GenerateFileForKMD()
        {
            _reportGenerator.WriteRecordsToFileAndAlterReportStatus();
        }

        private void SendDataToSD()
        {
            _logger.ErrorSd($"{this.GetType().ToString()}, SendDataToSD(), --------- TRANSFER TO SD STARTED -----------");
            
            var reportsToInvoice = _reportGenerator.ReceiveReportsToInvoiceSD();

            _logger.ErrorSd($"{this.GetType().ToString()}, SendDataToSD(), Number of reports to invoice: {reportsToInvoice.Count}");

            foreach(DriveReport report in reportsToInvoice)
            {
                var requestData = new SdKoersel.AnsaettelseKoerselOpretInputType();
                try
                {
                    requestData = PrepareRequestData(report);
                }
                catch(SdConfigException se)
                {
                    throw se; // This error is a general config error, and must break the loop.
                }
                catch (Exception e)
                {
                    _logger.ErrorSd($"{this.GetType().ToString()}, sendDataToSd(), Error when preparing data, Servicenummer = {report.Employment.EmploymentId}, EmploymentId = {report.EmploymentId}, Report = {report.Id}", e);
                    continue;
                }

                SendRequestToSD(requestData, report);
            }

            var reportsToInvoiceWithZeroDistance = _driveReportRepo.AsQueryable().Where(x => x.Status == ReportStatus.Accepted && x.Distance == 0).ToList();

            foreach (DriveReport report in reportsToInvoiceWithZeroDistance)
            {
                report.Status = ReportStatus.Invoiced;

                var epoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
                var deltaTime = DateTime.Now.ToUniversalTime() - epoch;
                report.ProcessedDateTimestamp = (long)deltaTime.TotalSeconds;

                try
                {
                    _driveReportRepo.Save();
                }
                catch (Exception e)
                {
                    _logger.ErrorSd($"{this.GetType().ToString()}, sendDataToSd(), Error when saving invoice status for report with distance = 0", e);
                }
            }

            _logger.ErrorSd($"{this.GetType().ToString()}, SendDataToSD(), --------- TRANSFER TO SD FINISHED -----------");
        }

        private void SendRequestToSD(SdKoersel.AnsaettelseKoerselOpretInputType request, DriveReport report)
        {
            try
            {
                var response = _sdClient.SendRequest(request);
            }
            catch (Exception e)
            {
                _logger.ErrorSd($"{this.GetType().ToString()}, sendDataToSd(), Error when sending data to SD, Servicenummer = {report.Employment.EmploymentId}, EmploymentId = {report.EmploymentId}, Report = {report.Id}", e);
                return;
            }

            report.Status = ReportStatus.Invoiced;

            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
            var deltaTime = DateTime.Now.ToUniversalTime() - epoch;
            report.ProcessedDateTimestamp = (long)deltaTime.TotalSeconds;

            try
            {
                _driveReportRepo.Save();
            }
            catch (Exception e)
            {
                _logger.ErrorSd($"{this.GetType().ToString()}, sendDataToSd(), Error when saving invoice status for report after sending to SD. Report has been sent, but status has NOT been changed, Servicenummer = {report.Employment.EmploymentId}, EmploymentId = {report.EmploymentId}, Report = {report.Id}", e);
                _logger.LogForAdmin($"En indberetning er blevet sendt til udbetaling via SD Løn, men dens status er ikke blevet ændret i OS2 Indberetning. Den vil dermed potentielt kunne sendes til udbetaling igen. Det drejer sig om medarbejder: {report.Person.Initials}, og indberetning med med ID: {report.Id}, kørt den: {new System.DateTime(1970, 1, 1, 0, 0, 0, 0).AddSeconds(report.DriveDateTimestamp)}");
            }
        }

        private SdKoersel.AnsaettelseKoerselOpretInputType PrepareRequestData(DriveReport report)
        {
            SdKoersel.AnsaettelseKoerselOpretInputType opretInputType = new SdKoersel.AnsaettelseKoerselOpretInputType();

            opretInputType.Item = _customSettings.SdInstitutionNumber; // InstitutionIdentifikator
            if (string.IsNullOrEmpty(opretInputType.Item))
            {
                throw new SdConfigException("PROTECTED_institutionNumber må ikke være tom");
            }
            opretInputType.ItemElementName = SdKoersel.ItemChoiceType.InstitutionIdentifikator;
            opretInputType.BrugerIdentifikator = report.ApprovedBy.CprNumber;
            opretInputType.Item1 = report.Employment.EmploymentId; // AnsaettelseIdentifikator 
            opretInputType.RegistreringTypeIdentifikator = report.TFCode;
            opretInputType.GodkendtIndikator = true;
            opretInputType.KoerselDato = new System.DateTime(1970, 1, 1, 0, 0, 0, 0).AddSeconds(report.DriveDateTimestamp).ToLocalTime();
            opretInputType.RegistreringNummerIdentifikator = report.LicensePlate;
            opretInputType.KontrolleretIndikator = true;
            opretInputType.KilometerMaal = Convert.ToDecimal(report.Distance);
            opretInputType.Regel60DageIndikator = false;
            opretInputType.DokumentationEksternIndikator = true;

            return opretInputType;
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
                portTypeClient.ClientCredentials.UserName.UserName = _customSettings.SdUsername ?? "";
                portTypeClient.ClientCredentials.UserName.Password = _customSettings.SdPassword ?? "";

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
                    operationRequest.InddataStruktur.InstitutionIdentifikator = _customSettings.SdInstitutionNumber ?? "";
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

                    //var response = portTypeClient.KoerselOpret20120201Operation(operationRequest.InddataStruktur);

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

    public class SdConfigException : Exception
    {
        public SdConfigException(string message) : base(message)
        {
        }
    }
}
