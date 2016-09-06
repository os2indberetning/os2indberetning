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
    public class sendDataToSdController : BaseController<DriveReport>
    {
        private readonly IGenericRepository<DriveReport> _repo;
        private readonly IGenericRepository<Person> _personRepo;

        private readonly ILogger _logger;



        public sendDataToSdController(IGenericRepository<DriveReport> repo, IGenericRepository<Person> personRepo, ILogger logger) : base(repo, personRepo)
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
            // var _url = "http://xxxxxxxxx/Service1.asmx";
            //var _action = "http://xxxxxxxx/Service1.asmx?op=HelloWorld"; 

            // HttpWebRequest webRequest = CreateWebRequest(_url, _action);

            XmlDocument soapEnvelopeXml = CreateSoapEnvelope();

            SdService.KoerselOpret20120201OperationRequest opret = new SdService.KoerselOpret20120201OperationRequest();
            opret.InddataStruktur = new SdService.KoerselOpretRequestType();
         


                /*
            SdService.KoerselOpret20120201PortTypeClient client = new SdService.KoerselOpret20120201PortTypeClient();
            var test = client.Endpoint;
            
            client.KoerselOpret20120201Operation(opret.InddataStruktur);
            client.ClientCredentials.UserName.UserName = "";
            

            SdService.KoerselOpret20120201Type type = new SdService.KoerselOpret20120201Type();
            
            
            SdService.KoerselOpret20120201OperationResponse respone = new SdService.KoerselOpret20120201OperationResponse();
            */
            return Ok();
        }

        private XmlDocument CreateSoapEnvelope()
        {
            var xmlResult = "";
            XmlDocument soapEnvelop = new XmlDocument();
            List<Models.InddataStruktur> result = new List<Models.InddataStruktur>();
            var reports = _repo.AsQueryable().Where(x=> x.Status != ReportStatus.Invoiced).ToList();
            
            foreach (var t in reports)
            {
                double koerselDato = t.DriveDateTimestamp;
                System.DateTime KoerseldateTime = new System.DateTime(1970, 1, 1, 0, 0, 0, 0);
                KoerseldateTime = KoerseldateTime.AddSeconds(koerselDato);
                
               

                try
                {
                    xmlResult +=
                        "<m:InddataStruktur xmlns:m=\"urn:oio:sd:snitflader:2012.02.01\">" +
                        "<m:InstitutionIdentifikator>" + t.Employment.OrgUnitId.ToString() + "</m:InstitutionIdentifikator>" +
                        "<m:PersonnummerIdentifikator>" + t.Person.Id.ToString() + "</m:PersonnummerIdentifikator>" +
                        "<m:AnsaettelseIdentifikator>" + t.EmploymentId + "</m:AnsaettelseIdentifikator>" +
                        "<m:RegistreringTypeIdentifikator>" + "MANGLER" + "</m:RegistreringTypeIdentifikator>" +
                        "<m:KoerselDato>" + KoerseldateTime.Date.ToString().Substring(0, 10) + "</m:KoerselDato>" +
                        "<m:KilometerMaal>" + t.Distance + "</m:KilometerMaal>" +
                        "<m:Regel60DageIndikator>" + false + "</m:Regel60DageIndikator>" +
                        "<m:KoertFraTekst>" + "MANGLER" + "</m:KoertFraTekst>" +
                        "<m:KoertTilTekst>" + "MANGLER" + "</m:KoertTilTekst>" +
                        "<m:AarsagTekst>" + XmlConvert.EncodeName(t.Purpose) + "</m:AarsagTekst>" +
                        "</m:InddataStruktur>";
                }
                catch (Exception e) { }
            }
            try
            {
                soapEnvelop.LoadXml(@"<SOAP-ENV:Envelope xmlns:SOAP-ENV=""http://schemas.xmlsoap.org/soap/envelope/"" xmlns:SOAP-ENC=""http://schemas.xmlsoap.org/soap/encoding/"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"" xmlns:xsd=""http://www.w3.org/2001/XMLSchema""><SOAP-ENV:Body>" + xmlResult + "</SOAP-ENV:Body></SOAP-ENV:Envelope>");
            }
            catch (Exception e)
            {

            }
            return soapEnvelop;
        }

        private void InsertSoapEnvelopeIntoWebRequest(XmlDocument soapEnvelopeXml, HttpWebRequest webRequest)
        {
            using (Stream stream = webRequest.GetRequestStream())
            {
                soapEnvelopeXml.Save(stream);
            }
        }

        private HttpWebRequest CreateWebRequest(string url, string action)
        {
            HttpWebRequest webRequest = (HttpWebRequest)WebRequest.Create(url);
            webRequest.Headers.Add("SOAPAction", action);
            webRequest.ContentType = "text/xml;charset=\"utf-8\"";
            webRequest.Accept = "text/xml";
            webRequest.Method = "POST";
            return webRequest;
        }



    }
}