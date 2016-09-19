using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.DomainModel
{
   public class EksportDrivereport
    {
        public string DriveDateTimestamp { get; set; }
        public string CreatedDateTimestamp { get; set; }
        public string OrgUnit { get; set; }
        public string Purpose { get; set; }
       // public string Route { get; set; }
        public bool? IsExtraDistance { get; set; }
        public bool FourKmRule { get; set; }
        public double distanceFromHomeToBorder {get; set;}
        public double AmountToReimburse { get; set; }
        public string approvedDate { get; set; }
        public string processedDate { get; set; }
        public string ApprovedBy { get; set; }
       public string kontering { get; set; }
       public string Route;
       public double distance { get; set; } 
       public bool? isRoundTrip { get; set; }

    }

    public class EksportModel
    {
        public string adminName;
        public string name;
        public string municipality;
        public string DateInterval;
        public HashSet<string> orgUnits;
        public HashSet<int> MaNumbers;
        public EksportDrivereport[] driveReports;
        public double wholeAmount;
        public double wholeDistance;
      

    }
}
