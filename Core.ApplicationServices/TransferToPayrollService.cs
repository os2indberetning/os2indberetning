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
            int countFailed = 0;
            int countSucces = 0;

            _logger.Debug("----- Begynd afsendelse -----");

            SdService1.KoerselOpret20120201OperationRequest opret;
            SdService1.KoerselOpret20120201PortTypeClient client;

            try
            {
                opret = new SdService1.KoerselOpret20120201OperationRequest();
                client = new SdService1.KoerselOpret20120201PortTypeClient();
                client.ClientCredentials.UserName.UserName = ConfigurationManager.AppSettings["PROTECTED_SDUserName"] ?? "";
                client.ClientCredentials.UserName.Password = ConfigurationManager.AppSettings["PROTECTED_SDUserPassword"] ?? "";
                // SdService.KoerselOpret20120201Type type = new SdService.KoerselOpret20120201Type();
                // SdService.KoerselOpret20120201OperationResponse respone = new SdService.KoerselOpret20120201OperationResponse();

                opret.InddataStruktur = new SdService1.KoerselOpretRequestType();
            }
            catch (Exception e)
            {
                _logger.Debug($"{this.GetType().ToString()}, sendDataToSd(), error when initating SD client");
                _logger.Debug("Fejl ved initialisering af kontakt til server, ingen indberetninger er afsendt.");
                throw e;
            }

            _logger.Debug($"{this.GetType().ToString()}, sendDataToSd(), ----- Loop start -----");

            foreach (var t in _driveReportRepo.AsQueryable().Where(x => x.Status == ReportStatus.Accepted).ToList())
            {
                double koerselDato = t.DriveDateTimestamp;
                System.DateTime KoerseldateTime = new System.DateTime(1970, 1, 1, 0, 0, 0, 0);
                KoerseldateTime = KoerseldateTime.AddSeconds(koerselDato);

                try
                {
                    opret.InddataStruktur.AarsagTekst = t.Purpose;
                    opret.InddataStruktur.AnsaettelseIdentifikator = t.Employment.EmploymentId;
                    opret.InddataStruktur.InstitutionIdentifikator = ConfigurationManager.AppSettings["PROTECTED_institutionNumber"] ?? "";
                    opret.InddataStruktur.PersonnummerIdentifikator = t.Person.CprNumber;
                    opret.InddataStruktur.RegistreringTypeIdentifikator = t.TFCode;
                    opret.InddataStruktur.KoerselDato = KoerseldateTime.Date;
                    opret.InddataStruktur.KilometerMaal = Convert.ToDecimal(t.Distance);

                    var startPoint = t.DriveReportPoints.Where(d => d.DriveReportId == t.Id && d.PreviousPointId == null && d.NextPointId != null).FirstOrDefault();
                    if (startPoint != null)
                    {
                        opret.InddataStruktur.KoertFraTekst = startPoint.StreetName + ", " + startPoint.StreetNumber + ", " + startPoint.ZipCode + ", " + startPoint.Town;
                    }
                    var endpoint = t.DriveReportPoints.Where(d => d.DriveReportId == t.Id && d.PreviousPointId == null && d.NextPointId != null).FirstOrDefault();

                    if (endpoint != null)
                    {
                        opret.InddataStruktur.KoertTilTekst = endpoint.StreetName + ", " + endpoint.StreetNumber + ", " + endpoint.ZipCode + ", " + endpoint.Town;
                    }
                    opret.InddataStruktur.Regel60DageIndikator = false;

                    //send data to SD

                    var response = client.KoerselOpret20120201Operation(opret.InddataStruktur);

                    t.Status = ReportStatus.Invoiced;

                    var epoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
                    var deltaTime = DateTime.Now.ToUniversalTime() - epoch;
                    t.ProcessedDateTimestamp = (long)deltaTime.TotalSeconds;

                    _driveReportRepo.Save();

                    countSucces++;

                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().ToString()}, sendDataToSd(), error when sending data, Servicenummer = {t.Employment.EmploymentId}, EmploymentId = {t.EmploymentId}, Kørselsdato = {KoerseldateTime.Date}", e);
                    _logger.Error($"Fejl for medarbejder: Servicenummer = {t.Employment.EmploymentId}, Kørselsdato = {KoerseldateTime.Date} --- Fejlbesked fra SD server: {e.Message}");
                    countFailed++;
                }
            }
            _logger.Debug($"----- Afsendelse afsluttet. {countFailed} ud af {countFailed + countSucces} afsendelser fejlede -----");
        }
    }
}
