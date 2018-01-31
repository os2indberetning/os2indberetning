using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Configuration;

namespace Core.ApplicationServices.Interfaces
{
    public interface ICustomSettings
    {
        bool SdIsEnabled { get; }
        string SdUsername { get; }
        string SdPassword { get; }
        string SdInstitutionNumber { get; }
        string Municipality { get; }
        IQueryable<string> GetUnProtected();
    }
}
