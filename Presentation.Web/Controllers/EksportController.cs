using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using OS2Indberetning.Controllers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Http;
using System.Web.Mvc;

namespace OS2Indberetning.Controllers
{
    public class EksportController : BaseController<DriveReport>
    {
        private readonly IDriveReportService _driveService;
        private readonly IGenericRepository<Employment> _employmentRepo;

        private readonly ILogger _logger;

        public EksportController(IGenericRepository<DriveReport> repo, IDriveReportService driveService, IGenericRepository<Person> personRepo, IGenericRepository<Employment> employmentRepo, ILogger logger): base(repo, personRepo)
        {
            _driveService = driveService;
            _employmentRepo = employmentRepo;
            _logger = logger;
        }




        public IHttpActionResult Get(string Employee,string Manr, string from, string to)
        {
                var model = new Models.EksportModel();
            model.Employee = Employee;
            model.Manr = Manr;
            model.StartDate = from;
            model.EndDate = to;

            var test = _employmentRepo.AsQueryable().Where(x=> x.Person.FirstName == Employee);

            return Ok(test);
        }
        

    }
}