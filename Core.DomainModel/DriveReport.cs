
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
        public double HomeToBorderDistance { get; set; } //Used with 4 km rule, when report is from app.
        public bool StartsAtHome { get; set; }
        public bool EndsAtHome { get; set; }
        public string LicensePlate { get; set; }
        public string FullName { get; set; }
        public string AccountNumber { get; set; }
        public string TFCode { get; set; }
        public KilometerAllowance KilometerAllowance { get; set; }
        public bool IsFromApp { get; set; }
        public string UserComment { get; set; }
        public string RouteGeometry { get; set; }
        public bool? IsExtraDistance { get; set; }
        public bool? IsOldMigratedReport { get; set; }
        public bool? IsRoundTrip { get; set; }

        /// <summary>
        /// Contains the transport allowance from a report. It is used for calculating the originally driven distance when editing a report with kilometer allowance as "read".
        /// If the four km rule has been used, TransportAllowance will contain the distance from home to border with the 4km added.
        /// </summary>
        public double? TransportAllowance { get; set; }


        public virtual ICollection<DriveReportPoint> DriveReportPoints { get; set; }
    }
}
