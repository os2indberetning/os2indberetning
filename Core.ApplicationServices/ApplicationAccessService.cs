using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.ApplicationServices.Logger;

namespace Core.ApplicationServices
{
    public class ApplicationAccessService : IApplicationAccessService
    {
        private readonly string _enabledApplications;
        private readonly ILogger _logger;

        public ApplicationAccessService(ILogger logger)
        {
            _enabledApplications = System.Configuration.ConfigurationManager.AppSettings["ENABLED_APPLICATIONS"].ToLower();
            _logger = logger;
        }

        public bool CanAccessAllApplications(Person person)
        {
            _logger.Log("ApplicationAccessService. CanAccessAllApplications() initial", "web", 3);
            return CanAccessDrive(person) && CanAccssVacation(person);
        }

        public bool CanAccessDrive(Person person)
        {
            try
            {
                return _enabledApplications.Contains("drive");
            }catch(Exception ex)
            {
                _logger.Log("ApplicationAccessService. CanAccessDrive() didnt contain drive, exception= " + ex.Message, "web", 3);
                return false;
            }
        }

        public bool CanAccssVacation(Person person)
        {
            try
            {
                return _enabledApplications.Contains("vacation") && person.Employments.Any(x => x.OrgUnit.HasAccessToVacation);
            }catch(Exception ex)
            {
                _logger.Log("ApplicationAccessService. CanAccssVacation() didnt contain vacation or has not acces to vacation, exception= " + ex.Message, "web", 3);
                return false;
            }
        }
    }
}
