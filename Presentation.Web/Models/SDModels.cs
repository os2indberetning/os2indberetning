using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Xml.Serialization;

namespace OS2Indberetning.Models
{
    public class InddataStruktur
    {
        public int InstitutionIdentifikator { get; set; }
        public string PersonnummerIdentifikator { get; set;}
        public int AnsaettelseIdentifikator { get; set; }
        public int RegistreringTypeIdentifikator { get; set; }
        public DateTime KoerselDato { get; set; }
        public double KilometerMaal { get; set; }
        public bool Regel60DageIndikator { get; set; }
        public string KoertFraTekst { get; set; }
        public string KoertTilTekst { get; set; }
        public string AarsagTekst { get; set; }


        [XmlAttribute(DataType = "number", AttributeName = "sd:InstitutionIdentifikator")]
        public int InstId
        {
            get { return InstitutionIdentifikator; }
            set { InstitutionIdentifikator = value; }
        }


    }
}