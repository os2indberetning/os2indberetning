using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace DBUpdater.Models
{
    public class OrganisationIDM
    {
        public string OUID { get; set; }
        public string Navn { get; set; }
        public string OverliggendeOUID { get; set; }
        public string OverliggendeOrg { get; set; }
        public string Leder { get; set; }
        public string Adresse { get; set; }
    }
}
