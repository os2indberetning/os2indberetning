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
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.DomainServices;
using Ninject;
using Core.DomainServices.Interfaces;

namespace OS2Indberetning.Controllers
{
    public class FileController : BaseController<DriveReport>
    {
        private readonly IGenericRepository<DriveReport> _repo;
        private readonly ITransferToPayrollService _transferToPayrollService;

        private readonly ILogger _logger;



        public FileController(IGenericRepository<DriveReport> repo, IGenericRepository<Person> personRepo, ITransferToPayrollService transferToPayrollService, ILogger logger) : base(repo, personRepo)
        {
            _repo = repo;
            _transferToPayrollService = transferToPayrollService;
            _logger = logger;
        }

        //GET: Generate KMD File
        /// <summary>
        /// Generates KMD file.
        /// </summary>
        /// <returns></returns>
        public IHttpActionResult Get()
        {
            _logger.Debug($"{GetType().Name}, Get(), Generate KMD file initialized");
            if (!CurrentUser.IsAdmin)
            {
                _logger.Error($"{GetType().Name}, Get(), {CurrentUser} is not admin, file generation aborted, Status code:403 Forbidden");
                return StatusCode(HttpStatusCode.Forbidden);
            }
            try
            {
                _transferToPayrollService.TransferReportsToPayroll();
                _logger.Debug($"{GetType().Name}, Get(), Transfer to payroll finished");
                return Ok();
            }
            catch (Exception e)
            {
                _logger.LogForAdmin("Fejl ved overførsel til lønsystem. Overførslen blev ikke gennemført.");
                _logger.Error($"{GetType().Name}, Get(), Error when transfering reports to payroll, Status Code: 500 Internal Server Error", e);
                return InternalServerError();
            }
        }


    }
}