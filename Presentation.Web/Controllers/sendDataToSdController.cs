using System;
using System.Configuration;
using System.Linq;
using System.Net;
using System.Web.Http;
using System.Web.Http.Controllers;
using System.Web.Mvc;
using System.Web.Optimization;
using Core.ApplicationServices;
using Core.ApplicationServices.FileGenerator;
using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using Ninject;
using System.Xml;
using System.IO;
using System.Collections.Generic;


namespace OS2Indberetning.Controllers
{
    public class SendDataToSdController : BaseController<DriveReport>
    {
        private readonly IGenericRepository<DriveReport> _repo;
        private readonly IGenericRepository<Person> _personRepo;

        private readonly ILogger _logger;



        public SendDataToSdController(IGenericRepository<DriveReport> repo, IGenericRepository<Person> personRepo, ILogger logger) : base(repo, personRepo)
        {
            _repo = repo;
            _personRepo = personRepo;
            _logger = logger;
        }

        //GET: Generate KMD File
        /// <summary>
        /// Generates KMD file.
        /// </summary>
        /// <returns></returns>
        public IHttpActionResult Get()
        {
            var request = sendDataToSd();
            return Ok();
        }

        public IHttpActionResult sendDataToSd()
        {
            SdService.KoerselOpret20120201OperationRequest opret;
            SdService.KoerselOpret20120201PortTypeClient client;

            try
            {
                opret = new SdService.KoerselOpret20120201OperationRequest();
                client = new SdService.KoerselOpret20120201PortTypeClient();
                client.ClientCredentials.UserName.UserName = ConfigurationManager.AppSettings["PROTECTED_SDUserName"] ?? "";
                client.ClientCredentials.UserName.Password = ConfigurationManager.AppSettings["PROTECTED_SDUserPassword"] ?? "";
                // SdService.KoerselOpret20120201Type type = new SdService.KoerselOpret20120201Type();
                // SdService.KoerselOpret20120201OperationResponse respone = new SdService.KoerselOpret20120201OperationResponse();

                opret.InddataStruktur = new SdService.KoerselOpretRequestType();

            }
            catch (Exception e)
            {
                _logger.Log($"{this.GetType().ToString()}, sendDataToSd(), error when initating SD client", "web", e, 1);
                throw;
            }
            //var reports = _repo.AsQueryable().Where(x => x.Status == ReportStatus.Accepted).ToList();

            try
            {
                foreach (var t in _repo.AsQueryable().Where(x => x.Status == ReportStatus.Accepted).ToList())
                {
                    double koerselDato = t.DriveDateTimestamp;
                    System.DateTime KoerseldateTime = new System.DateTime(1970, 1, 1, 0, 0, 0, 0);
                    KoerseldateTime = KoerseldateTime.AddSeconds(koerselDato);

                    opret.InddataStruktur.AarsagTekst = t.Purpose;
                    opret.InddataStruktur.AnsaettelseIdentifikator = t.EmploymentId.ToString();
                    opret.InddataStruktur.InstitutionIdentifikator = ConfigurationManager.AppSettings["PROTECTED_institutionNumber"];
                    opret.InddataStruktur.PersonnummerIdentifikator = t.Person.CprNumber;
                    opret.InddataStruktur.RegistreringTypeIdentifikator = "MANGLER - MANGLER - MANGLER";
                    opret.InddataStruktur.KoerselDato = KoerseldateTime.Date;
                    opret.InddataStruktur.KilometerMaal = Convert.ToDecimal(t.Distance);

                    var startPoint = t.DriveReportPoints.Where(d => d.DriveReportId == t.Id && d.PreviousPointId == null && d.NextPointId != null).FirstOrDefault();
                    if (startPoint != null) {
                        opret.InddataStruktur.KoertFraTekst = startPoint.StreetName + ", " + startPoint.StreetNumber + ", " + startPoint.ZipCode + ", " + startPoint.Town;
                    }
                    var endpoint = t.DriveReportPoints.Where(d => d.DriveReportId == t.Id && d.PreviousPointId == null && d.NextPointId != null).FirstOrDefault();

                    if (endpoint != null) {
                        opret.InddataStruktur.KoertTilTekst = endpoint.StreetName + ", " + endpoint.StreetNumber + ", " + endpoint.ZipCode + ", " + endpoint.Town;
                    }
                    opret.InddataStruktur.Regel60DageIndikator = false;
                  
                    //send data to SD
                    try
                    {
                        var response = client.KoerselOpret20120201Operation(opret.InddataStruktur);

                        t.Status = ReportStatus.Invoiced;

                        var epoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
                        var deltaTime = DateTime.Now.ToUniversalTime() - epoch;
                        t.ProcessedDateTimestamp = (long)deltaTime.TotalSeconds;

                        _repo.Save();

                    }
                    catch (Exception e) {
                        _logger.Log($"{this.GetType().ToString()}, sendDataToSd(), error when sending data", "web", e, 1);
                        return StatusCode(HttpStatusCode.InternalServerError);
                    }
                }
            }
            catch (Exception e)
            {
                _logger.Log($"{this.GetType().ToString()}, sendDataToSd(), error when iterating over reports to send", "web", e, 1);
                return StatusCode(HttpStatusCode.InternalServerError);
            }

            return Ok();
        }

    }
}