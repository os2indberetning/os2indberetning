using System;
using System.Collections.Generic;
using System.Linq;
namespace DBUpdater.Models
{
    public class IDMOrganisation
    {
        public string OUID { get; set; }
        public string Navn { get; set; }
        public string OverliggendeOUID { get; set; }
        public string OverliggendeOrg { get; set; }
        public string Leder { get; set; }
        public string Vejnavn { get; set; }
        public string PostNr { get; set; }
        public string PostDistrikt { get; set; }
    }
}
