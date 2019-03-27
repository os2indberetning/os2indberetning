
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;
using System.Dynamic;

namespace Core.DomainModel
{

    public enum KilometerAllowance
    {
        Calculated,
        Read,
        CalculatedWithoutExtraDistance
    }

    public enum DriveReportTransportType
    {
        Car,
        Bike
    }

    public class DriveReport : Report
    {
        public double Distance { get; set; }
        public double AmountToReimburse { get; set; }
        public string Purpose { get; set; }
        public double KmRate { get; set; }
        public long DriveDateTimestamp { get; set; }
        public bool FourKmRule { get; set; }
        /// <summary>
        /// Indicates how many of the 4 km from the 4 km rule has been deducted from this report, if the FourKmRule property is true. Will be zero if FourKmRule property is false.
        /// </summary>
        public double FourKmRuleDeducted { get; set; }
        /// <summary>
        /// Used with 4 km rule, when report is from app.
        /// </summary>
        public double HomeToBorderDistance { get; set; }
        public bool StartsAtHome { get; set; }
        public bool EndsAtHome { get; set; }
        public string LicensePlate { get; set; }
        public string FullName { get; set; }
        public string AccountNumber { get; set; }
        public string TFCode { get; set; }
        public string TFCodeOptional { get; set; }
        public KilometerAllowance KilometerAllowance { get; set; }
        public bool IsFromApp { get; set; }
        public string UserComment { get; set; }
        public string RouteGeometry { get; set; }
        public bool? IsExtraDistance { get; set; }
        public bool? IsOldMigratedReport { get; set; }
        public bool? IsRoundTrip { get; set; }
        public bool SixtyDaysRule { get; set; }
        public bool IsUsingDivergentAddress { get; set; }


        public virtual ICollection<DriveReportPoint> DriveReportPoints { get; set; }
    }
}
