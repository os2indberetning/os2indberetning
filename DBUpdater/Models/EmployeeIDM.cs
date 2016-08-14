using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace DBUpdater.Models
{
    public class EmployeeIDM
    {
        public string Institutionskode { get; set; }
        public string Tjenestenummer { get; set; }
        public DateTime? AnsættelseFra { get; set; }
        public DateTime? AnsættelseTil { get; set; }
        public string Fornavn { get; set; }
        public string Efternavn { get; set; }
        public string APOSUID { get; set; }
        public string BrugerID { get; set; }
        public string OrgEnhed { get; set; }
        public string OrgEnhedOUID { get; set; }
        public string Email { get; set; }
        public string Stillingsbetegnelse { get; set; }
        public string CPRNummer { get; set; }
        public string Vejnavn { get; set; }
        public string PostNr { get; set; }
        public string PostDistrikt { get; set; }
    }
}
