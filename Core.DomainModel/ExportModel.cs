using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.DomainModel
{
    public class ExportModel
    {
        public string AdminName { get; set; }
        public string Name { get; set; }
        public string Municipality { get; set; }
        public string DateInterval { get; set; }
        public string OrgUnit { get; set; }
        public ExportDriveReport[] DriveReports { get; set; }
        public double TotalAmount { get; set; }
        public double TotalDistance { get; set; }
        public string LicensePlates { get; set; }
        public string HomeAddressStreetAndNumber { get; set; }
        public string HomeAddressZipCodeAndTown { get; set; }
    }
}
