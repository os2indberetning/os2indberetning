using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.DomainModel
{
   public class EksportModel
    {
        public long DriveDateTimestamp { get; set; }
        public long CreatedDateTimestamp { get; set; }
        public string OrgUnit { get; set; }
        public string Purpose { get; set; }
        public string Route { get; set; }
        public bool? IsExtraDistance { get; set; }
        public bool FourKmRule { get; set; }
        public double distanceFromHomeToBorder {get; set;}
        public double AmountToReimburse { get; set; }
        public virtual Person ApprovedBy { get; set; }
        
    }
}
