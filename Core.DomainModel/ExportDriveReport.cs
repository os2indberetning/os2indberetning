using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.DomainModel
{
    public class ExportDriveReport
    {
        public string DriveDateTimestamp { get; set; }
        public string CreatedDateTimestamp { get; set; }
        public string OrgUnit { get; set; }
        public string Purpose { get; set; }
        public bool? IsExtraDistance { get; set; }
        public bool FourKmRule { get; set; }
        public double DistanceFromHomeToBorder { get; set; }
        public double AmountToReimburse { get; set; }
        public string ApprovedDate { get; set; }
        public string ProcessedDate { get; set; }
        public string ApprovedBy { get; set; }
        public string Accounting { get; set; }
        public string Route { get; set; }
        public double Distance { get; set; }
        public bool? IsRoundTrip { get; set; }
        public string LicensePlate { get; set; }
        public double Rate { get; set; }
        public string HomeAddress { get; set; }
    }
}
