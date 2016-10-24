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

namespace OS2Indberetning.Controllers
{
    public class FileController : BaseController<DriveReport>
    {
        private readonly IGenericRepository<DriveReport> _repo;

        private readonly ILogger _logger;



        public FileController(IGenericRepository<DriveReport> repo, IGenericRepository<Person> personRepo, ILogger logger) : base(repo, personRepo)
        {
            _repo = repo;
            _logger = logger;
        }

        //GET: Generate KMD File
        /// <summary>
        /// Generates KMD file.
        /// </summary>
        /// <returns></returns>
        public IHttpActionResult Get()
        {
            _logger.Log("FileController. GET() initial", "web", 3);
            if (!CurrentUser.IsAdmin)
            {
                _logger.Log("FileController. GET() Forbidden", "web", 3);
                return StatusCode(HttpStatusCode.Forbidden);
            }
            try
            {
                new ReportGenerator(_repo, new ReportFileWriter()).WriteRecordsToFileAndAlterReportStatus();
                _logger.Log("FileController. GET() END OK", "web", 3);
                return Ok();
            }
            catch (Exception e)
            {
                _logger.Log("Fejl ved generering af fil til KMD. Filen blev ikke genereret.", "web",e,1);
                _logger.Log("FileController. GET() Exception", "web",e, 3);
                return InternalServerError();
            }
        }


    }
}