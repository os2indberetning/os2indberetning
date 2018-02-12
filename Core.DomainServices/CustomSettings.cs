using Core.DomainServices.Interfaces;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.DomainServices
{
    public class CustomSettings : ICustomSettings
    {
        private readonly string _protected = "PROTECTED_";
        public string Municipality
        {
            get
            {
                return GetValue(_protected + "muniplicity");
            }
        }

        public string SdInstitutionNumber
        {
            get
            {
                return GetValue(_protected + "institutionNumber");
            }
        }

        public string SdPassword
        {
            get
            {
                return GetValue(_protected + "SDUserPassword");
            }
        }

        public string SdUsername
        {
            get
            {
                return GetValue(_protected + "SDUserName");
            }
        }

        public bool SdIsEnabled
        {
            get
            {
                bool result;
                bool parseSucces = bool.TryParse(GetValue("UseSd"), out result);
                if (!parseSucces)
                {
                    return false;
                }
                else
                {
                    return result;
                }
            }
        }

        private string GetValue(string key)
        {
            string result;
            try
            {
                result = ConfigurationManager.AppSettings[key];
            }
            catch (Exception)
            {
                result = "";
            }
            return result ?? "";
        }

        public IQueryable<string> GetUnProtected()
        {
            throw new NotImplementedException();
        }
    }
}
