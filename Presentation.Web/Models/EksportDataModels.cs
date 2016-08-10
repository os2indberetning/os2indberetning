using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;

namespace OS2Indberetning.Models
{
    public class EksportModel
    {
        public string Employee { get; set; }

        public string Manr { get; set; }

        public string StartDate { get; set; }

        public string EndDate { get; set; }

        public List<reportData> Data{get; set;}


    }
    public class reportData {
        public string dataFrom { get; set; }
        public string dataTo { get; set; }
        public string Miles { get; set; }


    }

}