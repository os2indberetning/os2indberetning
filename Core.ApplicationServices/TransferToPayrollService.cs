﻿using Core.ApplicationServices.FileGenerator;
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
            bool useSdAsIntegration = _customSettings.SdIsEnabled;
            _logger.Debug($"{GetType().Name}, TransferReportsToPayroll(), UseSd configuration = {useSdAsIntegration}");
            if (useSdAsIntegration)
            {
                SendDataToSD();
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

        private void SendDataToSD()
        {
            var reportsToInvoice = _driveReportRepo.AsQueryable().Where(x => x.Status == ReportStatus.Accepted && x.Distance > 0).ToList();
            _logger.Error($"{this.GetType().ToString()}, SendDataToSD(), Number of reports to invoice: {reportsToInvoice.Count}");

            foreach(DriveReport report in reportsToInvoice)
            {
                SdKoersel.AnsaettelseKoerselOpretInputType requestData = new SdKoersel.AnsaettelseKoerselOpretInputType();
                try
                {
                    requestData = PrepareRequestData(requestData, report);
                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().ToString()}, sendDataToSd(), Error when preparing data, Servicenummer = {report.Employment.EmploymentId}, EmploymentId = {report.EmploymentId}, Report = {report.Id}", e);
                    continue;
                }

                try
                {
                    var response = _sdClient.SendRequest(requestData);
                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().ToString()}, sendDataToSd(), Error when sending data to SD, Servicenummer = {report.Employment.EmploymentId}, EmploymentId = {report.EmploymentId}, Report = {report.Id}", e);
                    continue;
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
                    _logger.Error($"{this.GetType().ToString()}, sendDataToSd(), Error when saving invoice status for report after sending to SD. Report has been sent, but status has NOT been changed, Servicenummer = {report.Employment.EmploymentId}, EmploymentId = {report.EmploymentId}, Report = {report.Id}", e);
                    _logger.LogForAdmin($"En indberetning er blevet sendt til udbetaling via SD Løn, men dens status er ikke blevet ændret i OS2 Indberetning. Den vil dermed potentielt kunne sendes til udbetaling igen. Det drejer sig om medarbejder: {report.Person.Initials}, og indberetning med med ID: {report.Id}, kørt den: {new System.DateTime(1970, 1, 1, 0, 0, 0, 0).AddSeconds(report.DriveDateTimestamp)}");
                }
            }
        }

        private SdKoersel.AnsaettelseKoerselOpretInputType PrepareRequestData(SdKoersel.AnsaettelseKoerselOpretInputType opretInputType, DriveReport report)
        {
            opretInputType.Item = _customSettings.SdInstitutionNumber; // InstitutionIdentifikator
            if (string.IsNullOrEmpty(opretInputType.Item))
            {
                throw new Exception("PROTECTED_institutionNumber må ikke være tom");
            }
            opretInputType.ItemElementName = SdKoersel.ItemChoiceType.InstitutionIdentifikator;
            opretInputType.BrugerIdentifikator = report.Person.CprNumber;
            opretInputType.Item1 = report.Employment.EmploymentId; // AnsaettelseIdentifikator 
            opretInputType.RegistreringTypeIdentifikator = report.TFCode;
            opretInputType.GodkendtIndikator = true;
            opretInputType.KoerselDato = new System.DateTime(1970, 1, 1, 0, 0, 0, 0).AddSeconds(report.DriveDateTimestamp);
            opretInputType.RegistreringNummerIdentifikator = report.LicensePlate;
            opretInputType.KontrolleretIndikator = true;
            opretInputType.KilometerMaal = Convert.ToDecimal(report.Distance);
            opretInputType.Regel60DageIndikator = false; // TODO: skal denne sættes?

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
