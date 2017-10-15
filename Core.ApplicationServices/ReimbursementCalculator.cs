using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using Infrastructure.AddressServices.Routing;
using Infrastructure.DataAccess;
using Ninject;
using Core.ApplicationServices.Logger;
using System.Configuration;

namespace Core.ApplicationServices
{

    public class ReimbursementCalculator : IReimbursementCalculator
    {
        private readonly IRoute<RouteInformation> _route;
        private readonly IPersonService _personService;
        private readonly IGenericRepository<Person> _personRepo;
        private readonly IGenericRepository<Employment> _emplrepo;
        private readonly IGenericRepository<AddressHistory> _addressHistoryRepo;
        private readonly IGenericRepository<RateType> _rateTypeRepo;
        private readonly IGenericRepository<DriveReport> _driveReportRepository;
        private const int FourKmAdjustment = 4;
        // Coordinate threshold is the amount two gps coordinates can differ and still be considered the same address.
        // Third decimal is 100 meters, so 0.001 means that addresses within 100 meters of each other will be considered the same when checking if route starts or ends at home.
        private const double CoordinateThreshold = 0.001;

        private readonly ILogger _logger;

        public ReimbursementCalculator(IRoute<RouteInformation> route, IPersonService personService, IGenericRepository<Person> personRepo, IGenericRepository<Employment> emplrepo, IGenericRepository<AddressHistory> addressHistoryRepo, ILogger logger, IGenericRepository<RateType> rateTypeRepo, IGenericRepository<DriveReport> driveReportRepo)
        {
            _route = route;
            _personService = personService;
            _personRepo = personRepo;
            _emplrepo = emplrepo;
            _addressHistoryRepo = addressHistoryRepo;
            _logger = logger;
            _rateTypeRepo = rateTypeRepo;
            _driveReportRepository = driveReportRepo;
        }

        /// <summary>
        /// Takes a DriveReport as input and returns it with data.
        /// 
        /// FourKmRule: If a user has set the FourKmRule to be used, the distance between 
        /// the users home and municipality is used in the correction of the driven distance.
        /// If the rule is not used, the distance between the users home and work address are 
        /// calculated and used, provided that the user has not set a override for this value.
        /// 
        /// Calculated: The driven route is calculated, and based on whether the user starts 
        /// and/or ends at home, the driven distance is corrected by subtracting the distance 
        /// between the users home address and work address. 
        /// Again, this is dependend on wheter the user has overridden this value.
        /// 
        /// Calculated without extra distance: If this method is used, the driven distance is 
        /// still calculated, but the distance is not corrected with the distance between the 
        /// users home address and work address. The distance calculated from the service is 
        /// therefore used directly in the calculation of the amount to reimburse
        /// 
        /// </summary>
        public DriveReport Calculate(RouteInformation drivenRoute, DriveReport report)
        {
            //Check if user has manually provided a distance between home address and work address
            var homeWorkDistance = 0.0;

            var person = _personRepo.AsQueryable().First(x => x.Id == report.PersonId);

            var homeAddress = _personService.GetHomeAddress(person);

            // Get Work and Homeaddress of employment at time of DriveDateTimestamp for report.
            AddressHistory addressHistory = null;

            try
            {
                addressHistory = _addressHistoryRepo.AsQueryable().SingleOrDefault(x => x.EmploymentId == report.EmploymentId && x.StartTimestamp < report.DriveDateTimestamp && x.EndTimestamp > report.DriveDateTimestamp);
            }
            catch (InvalidOperationException)
            {
                _logger.LogForAdmin(report.FullName + " har et overlap i sin adressehistorik");
                throw;
            }

            if (homeAddress.Type != PersonalAddressType.AlternativeHome)
            {
                if (addressHistory != null && addressHistory.HomeAddress != null)
                {
                    // If user doesn't have an alternative address set up then use the homeaddress at the time of DriveDateTimestamp
                    // If the user does have an alternative address then always use that.
                    homeAddress = addressHistory.HomeAddress;
                }
            }


            var employment = _emplrepo.AsQueryable().FirstOrDefault(x => x.Id.Equals(report.EmploymentId));

            Address workAddress = employment.OrgUnit.Address;

            if (addressHistory != null && addressHistory.WorkAddress != null)
            {
                // If an AddressHistory.WorkAddress exists, then use that.
                workAddress = addressHistory.WorkAddress;
            }


            if (employment.AlternativeWorkAddress != null)
            {
                // Overwrite workaddress if an alternative work address exists.
                workAddress = employment.AlternativeWorkAddress;
            }

            if (report.KilometerAllowance != KilometerAllowance.Read && !report.IsFromApp)
            {

                //Check if drivereport starts at users home address.
                report.StartsAtHome = areAddressesCloseToEachOther(homeAddress, report.DriveReportPoints.First());
                //Check if drivereport ends at users home address.
                report.EndsAtHome = areAddressesCloseToEachOther(homeAddress, report.DriveReportPoints.Last());
            }


            homeWorkDistance = employment.WorkDistanceOverride;

            if (homeWorkDistance <= 0)
            {
                homeWorkDistance = _route.GetRoute(DriveReportTransportType.Car, new List<Address>() { homeAddress, workAddress }).Length;
            }

            //Calculate distance to subtract
            //double toSubtract = 0;
            double toSubtractFourKmRule = 0;
            double toSubtractHomeRule = 0;
            double toSubtractAltRule = 0;


            //If user indicated to use the Four Km Rule
            if (report.FourKmRule)
            {
                //Take users provided distance from home to border of municipality. If report is from app, use distance provided in report, else use distance saved on person.
                var borderDistance = report.IsFromApp ? report.HomeToBorderDistance : person.DistanceFromHomeToBorder;

                //Adjust distance based on if user starts or ends at home
                if (report.StartsAtHome)
                {
                    //toSubtract += borderDistance;
                    toSubtractFourKmRule += borderDistance;
                }

                if (report.EndsAtHome)
                {
                    //toSubtract += borderDistance;
                    toSubtractFourKmRule += borderDistance;
                }
            }
            else
            {
                //Same logic as above, but uses calculated distance between home and work
                if (report.StartsAtHome)
                {
                    //toSubtract += homeWorkDistance;
                    toSubtractHomeRule += homeWorkDistance;
                }

                if (report.EndsAtHome)
                {
                    //toSubtract += homeWorkDistance;
                    toSubtractHomeRule += homeWorkDistance;
                }
            }

            switch (report.KilometerAllowance)
            {
                case KilometerAllowance.Calculated:
                    {
                        // Norddjurs Kommune uses an alternative way of calculating the amount to reimburse. Instead of subtracting the distance from home to work from the driven distance,
                        // either the home-to-destination or work-to-destination distance is used, which ever is shortest. This only applies to routes starting from home, in any other case
                        // the standard calculation method is used.
                        //bool useNorddjursAltCalculation;
                        //bool parseSucces = bool.TryParse(ConfigurationManager.AppSettings["AlternativeCalculationMethod"], out useNorddjursAltCalculation);
                        //useNorddjursAltCalculation = parseSucces ? useNorddjursAltCalculation : false;

                        string alternativeCalculationMethod = ConfigurationManager.AppSettings["AlternativeCalculationMethod"];
                        if (alternativeCalculationMethod == null) alternativeCalculationMethod = string.Empty;
                        // Use Norddjurs alternative reimbursemnt calculation method if configured so.
                        if (alternativeCalculationMethod.ToLower() == "ndk")
                        {
                            //// The alternative calculationmethod is only used for reports starting at home.
                            //if (report.StartsAtHome)
                            //{
                            //    // Distance from home.
                            //    var homeDistance = report.Distance;
                            //    if (!report.IsFromApp)
                            //    {
                            //        // In case the report is not from app then get distance from the supplied route.
                            //        homeDistance = drivenRoute.Length;
                            //    }

                            //    // Get distance from work.
                            //    var addresses = new List<Address>();
                            //    addresses.Add(workAddress);
                            //    foreach (Address address in report.DriveReportPoints)
                            //    {
                            //        if (!(address.Latitude == homeAddress.Latitude && address.Longitude == homeAddress.Longitude))
                            //        {
                            //            addresses.Add(address);
                            //        }
                            //    }

                            //    var isBike = _rateTypeRepo.AsQueryable().First(x => x.TFCode.Equals(report.TFCode)).IsBike;

                            //    var workDistance = _route.GetRoute(isBike ? DriveReportTransportType.Bike : DriveReportTransportType.Car, addresses).Length;

                            //    // Compare the distance from home to the distance from work and apply the shortest of them.
                            //    report.Distance = homeDistance < workDistance ? homeDistance : workDistance;

                            //    if (!report.IsFromApp)
                            //    {
                            //        //Get RouteGeometry from driven route if the report is not from app. If it is from App then RouteGeometry is already set.
                            //        report.RouteGeometry = drivenRoute.GeoPoints;
                            //    }

                            //    break;
                            //}
                            //else
                            //{
                            //    if (!report.IsFromApp)
                            //    {
                            //        report.Distance = drivenRoute.Length;

                            //        //Save RouteGeometry
                            //        report.RouteGeometry = drivenRoute.GeoPoints;
                            //    }

                            //    break;
                            //}

                            // Norddjurs Kommune uses an alternative method of calculating the distance to reimburse.
                            // Instead of just subtracting the daily distance from HOME to WORK from the driven distance, the shortest of
                            // either HOME to LOCATION or WORK to LOCATION is used. The "daily "distance from HOME to WORK is newer subtracted!
                            //
                            // 1) When starting from home
                            //    * To select which route is used, the shortest of either the distance between HOME and LOCATION or
                            //      the distance between WORK and LOCATION, is selected.
                            //      The LOCATION is always the second point on the drive route, because the first is HOME.
                            //    * To calculate the distance to subtract, the total distance of the two routes is calculated.
                            //
                            // 2) When finishing at home
                            //    * To select which route is used, the shortest of either the distance between LOCATION and HOME or
                            //      the distance between LOCATION and WORK, is selected.
                            //      The LOCATION is always the second last point on the drive route, because the last is HOME.
                            //    * To calculate the distance to subtract, the total distance of the two routes is calculated.
                            //
                            // Both calculations can apply, when starting and finishing at home.

                            // Newer subtract the "daily" distance between HOME and WORK.
                            toSubtractHomeRule = 0.0;

                            // Get the drive report points.
                            List<DriveReportPoint> altDriveReportPoints = new List<DriveReportPoint>();
                            foreach (DriveReportPoint altDriveReportPoint in report.DriveReportPoints)
                            {
                                altDriveReportPoints.Add(altDriveReportPoint);
                            }

                            // Get if a bike is used for transportation.
                            Boolean altIsBike = _rateTypeRepo.AsQueryable().First(x => x.TFCode.Equals(report.TFCode)).IsBike;
                            DriveReportTransportType altTransportType = (altIsBike == true) ? DriveReportTransportType.Bike : DriveReportTransportType.Car;
                            Double altToSubtract1 = 0.0;
                            Double altToSubtract2 = 0.0;

                            // 1a. When starting from home
                            if (    //(report.IsFromApp == false) &&
                                (report.StartsAtHome == true) &&
                                (altDriveReportPoints.Count >= 2) &&
                                ((altDriveReportPoints[1].Latitude != workAddress.Latitude) && (altDriveReportPoints[1].Longitude != workAddress.Longitude)))
                            {
                                // A1) Get the distance between HOME and the first LOCATION (the route in the report).
                                //     This is used to select the reported route, or the alternative route.
                                List<Address> altAddressesToHome = new List<Address>();
                                altAddressesToHome.Add(homeAddress);
                                altAddressesToHome.Add(altDriveReportPoints[1]);
                                Double altDistanceToHome = _route.GetRoute(altTransportType, altAddressesToHome).Length;


                                // A2) Get the distance for the entire route (the route in the report).
                                //     This is used to calculate the distance to subtract.
                                altAddressesToHome = new List<Address>(altDriveReportPoints);
                                Double altDistanceA = _route.GetRoute(altTransportType, altAddressesToHome).Length;


                                // B1) Get the distance between WORK and the first LOCATION.
                                //     This is used to select the reported route, or the alternative route.
                                List<Address> altAddressesToWork = new List<Address>();
                                altAddressesToWork.Add(workAddress);
                                altAddressesToWork.Add(altDriveReportPoints[1]);
                                Double altDistanceToWork = _route.GetRoute(altTransportType, altAddressesToWork).Length;


                                // B2) Get the distance for the entire alternative route.
                                //     This is used to calculate the distance to subtract.
                                altAddressesToWork = new List<Address>(altDriveReportPoints);
                                altAddressesToWork[0] = workAddress;
                                Double altDistanceB = _route.GetRoute(altTransportType, altAddressesToWork).Length;


                                // The current report distance is including the route between HOME and LOCATION.
                                // Substract the difference, if the distance between WORK and LOCATION is smaller.
                                if (altDistanceToWork < altDistanceToHome)
                                {
                                    altToSubtract1 = (altDistanceA - altDistanceB);
                                }


                            }

                            // 1b. When starting from home
                            if (    //(report.IsFromApp == false) &&
                                (report.StartsAtHome == true) &&
                                (altDriveReportPoints.Count == 2) &&
                                ((altDriveReportPoints[1].Latitude == workAddress.Latitude) && (altDriveReportPoints[1].Longitude == workAddress.Longitude)))
                            {
                                toSubtractHomeRule += homeWorkDistance;
                            }


                                // 2a. When finishing at home 
                                if (    //(report.IsFromApp == false) &&
                                (report.EndsAtHome == true) &&
                                (altDriveReportPoints.Count >= 2) &&
                                ((altDriveReportPoints[altDriveReportPoints.Count - 2].Latitude != workAddress.Latitude) && (altDriveReportPoints[altDriveReportPoints.Count - 2].Longitude != workAddress.Longitude)))
                            {
                                // A1) Get the distance between the second last LOCATION and HOME (the route in the report).
                                //     This is used to select the reported route, or the alternative route.
                                List<Address> altAddressesToHome = new List<Address>();
                                altAddressesToHome.Add(altDriveReportPoints[altDriveReportPoints.Count - 2]);
                                altAddressesToHome.Add(homeAddress);
                                Double altDistanceToHome = _route.GetRoute(altTransportType, altAddressesToHome).Length;


                                // A2) Get the distance for the entire route (the route in the report).
                                //     This is used to calculate the distance to subtract.
                                altAddressesToHome = new List<Address>(altDriveReportPoints);
                                Double altDistanceA = _route.GetRoute(altTransportType, altAddressesToHome).Length;


                                // B1) Get the distance between the second last LOCATION and WORK.
                                //     This is used to select the reported route, or the alternative route.
                                List<Address> altAddressesToWork = new List<Address>();
                                altAddressesToWork.Add(altDriveReportPoints[altDriveReportPoints.Count - 2]);
                                altAddressesToWork.Add(workAddress);
                                Double altDistanceToWork = _route.GetRoute(altTransportType, altAddressesToWork).Length;


                                // B2) Get the distance for the entire alternative route.
                                //     This is used to calculate the distance to subtract.
                                altAddressesToWork = new List<Address>(altDriveReportPoints);
                                altAddressesToWork[altAddressesToWork.Count - 1] = workAddress;
                                Double altDistanceB = _route.GetRoute(altTransportType, altAddressesToWork).Length;


                                // The current report distance is including the route between HOME and LOCATION.
                                // Substract the difference, if the distance between WORK and LOCATION is smaller.
                                if (altDistanceToWork < altDistanceToHome)
                                {
                                    altToSubtract2 = (altDistanceA - altDistanceB);
                                }


                            }

                            // 2b. When finishing at home and 2 points and 1 is work
                            if (    //(report.IsFromApp == false) &&
                                (report.EndsAtHome == true) &&
                                (altDriveReportPoints.Count == 2) &&
                                ((altDriveReportPoints[altDriveReportPoints.Count - 2].Latitude == workAddress.Latitude) && (altDriveReportPoints[altDriveReportPoints.Count - 2].Longitude == workAddress.Longitude)))
                            {
                                toSubtractHomeRule += homeWorkDistance;
                            }

                            // Subtract.
                            toSubtractAltRule = altToSubtract1 + altToSubtract2;


                        }
                        //End NDK

                        if ((report.StartsAtHome || report.EndsAtHome) && !report.FourKmRule)
                        {
                            report.IsExtraDistance = true;
                        }

                        double drivenDistance = report.Distance;

                        if (!report.IsFromApp)
                        {
                            // In case the report is not from app then get distance from the supplied route.
                            drivenDistance = drivenRoute.Length;
                        }
                        //Adjust distance based on FourKmRule and if user start and/or ends at home
                        //var correctDistance = drivenDistance - toSubtract;
                        var correctDistance = drivenDistance - (toSubtractFourKmRule + toSubtractHomeRule + toSubtractAltRule);


                        //Set distance to corrected
                        report.Distance = correctDistance;

                        if (!report.IsFromApp)
                        {
                            //Get RouteGeometry from driven route if the report is not from app. If it is from App then RouteGeometry is already set.
                            report.RouteGeometry = drivenRoute.GeoPoints;
                        }

                        break;
                    }
                case KilometerAllowance.CalculatedWithoutExtraDistance:
                    {
                        report.Distance = drivenRoute.Length;

                        //Save RouteGeometry
                        report.RouteGeometry = drivenRoute.GeoPoints;


                        break;
                    }

                case KilometerAllowance.Read:
                    {
                        if ((report.StartsAtHome || report.EndsAtHome) && !report.FourKmRule)
                        {
                            report.IsExtraDistance = true;
                        }

                        //Take distance from report
                        var manuallyProvidedDrivenDistance = report.Distance;

                        //report.Distance = manuallyProvidedDrivenDistance - toSubtract;
                        report.Distance = manuallyProvidedDrivenDistance - toSubtractFourKmRule - toSubtractHomeRule;

                        break;
                    }
                default:
                    {
                        throw new Exception("No calculation method provided");
                    }
            }

            //Calculate the actual amount to reimburse

            if (report.Distance < 0)
            {
                report.Distance = 0;
            }

            // Multiply the distance by two if the report is a return trip
            if (report.IsRoundTrip == true)
            {
                report.Distance *= 2;
            }

            CalculateFourKmRuleForReport(report);

            SetAmountToReimburse(report);

            return report;
        }

        /// <summary>
        /// Calculates how many of the 4 km from the Four Km Rule should be deducted from this report. 
        /// The calculated amount will then be deducted from the distance, and saved in the FourKmRuleDeducted property
        /// </summary>
        /// <param name="report"></param>
        /// <returns></returns>
        public DriveReport CalculateFourKmRuleForReport(DriveReport report)
        {
            var result = report;

            if (report.FourKmRule)
            {
                // Find all the reports of the employment from the same day that uses the four km rule and has not been rejected, and select the FourKmRuleDeducted property.
                List<DriveReport> reportsFromSameDayWithFourKmRule;
                reportsFromSameDayWithFourKmRule = _driveReportRepository.AsQueryable().Where(x => x.PersonId == report.PersonId
                    && x.Status != ReportStatus.Rejected
                    && x.FourKmRule).ToList();

                reportsFromSameDayWithFourKmRule.RemoveAll(x => !AreReportsDrivenOnSameDay(report.DriveDateTimestamp, x.DriveDateTimestamp));

                // Sum the values selected to get the total deducted amount of the day.
                var totalDeductedFromSameDay = reportsFromSameDayWithFourKmRule.Take(reportsFromSameDayWithFourKmRule.Count()).Sum(x => x.FourKmRuleDeducted);

                // If less than four km has been deducted, deduct the remaining amount from the current report. Cannot deduct more than the distance of the report.
                if (totalDeductedFromSameDay < FourKmAdjustment)
                {
                    if (report.Distance < FourKmAdjustment - totalDeductedFromSameDay)
                    {
                        report.FourKmRuleDeducted = report.Distance;
                        report.Distance = 0;
                    }
                    else
                    {
                        report.FourKmRuleDeducted = FourKmAdjustment - totalDeductedFromSameDay;
                        report.Distance -= report.FourKmRuleDeducted;
                    }
                }
            }

            return report;
        }

        public bool AreReportsDrivenOnSameDay(long unixTimeStamp1, long unixTimeStamp2)
        {
            var result = false;
            DateTime dtDateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
            DateTime driveDate1 = dtDateTime.AddSeconds(unixTimeStamp1).ToLocalTime();
            DateTime driveDate2 = dtDateTime.AddSeconds(unixTimeStamp2).ToLocalTime();
            if (driveDate1.Date.Equals(driveDate2.Date))
            {
                result = true;
            }
            return result;
        }

        private void SetAmountToReimburse(DriveReport report)
        {
            // report.KmRate / 100 to go from ører to kroner.
            report.AmountToReimburse = (report.Distance) * (report.KmRate / 100);

            if (report.AmountToReimburse < 0)
            {
                report.AmountToReimburse = 0;
            }
        }

        /// <summary>
        /// Checks that two addresses are within 100 meters, in
        /// which case we assume they are the same when regarding
        /// if a person starts or ends their route at home.
        /// Longitude and latitude is called different things depending on
        /// whether we get the information from the backend or from septima
        /// </summary>
        /// <param name="address1">First address</param>
        /// <param name="address2">Second address</param>
        /// <returns>true if the two addresses are within 100 meters of each other.</returns>
        private bool areAddressesCloseToEachOther(Address address1, Address address2)
        {
            var long1 = Convert.ToDouble(address1.Longitude, CultureInfo.InvariantCulture);
            var long2 = Convert.ToDouble(address2.Longitude, CultureInfo.InvariantCulture);
            var lat1 = Convert.ToDouble(address1.Latitude, CultureInfo.InvariantCulture);
            var lat2 = Convert.ToDouble(address2.Latitude, CultureInfo.InvariantCulture);

            var longDiff = Math.Abs(long1 - long2);
            var latDiff = Math.Abs(lat1 - lat2);
            return longDiff < CoordinateThreshold && latDiff < CoordinateThreshold;
        }


    }
}