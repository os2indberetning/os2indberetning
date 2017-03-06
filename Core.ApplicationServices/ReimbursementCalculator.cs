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
        private const int FourKmAdjustment = 4;
        // Coordinate threshold is the amount two gps coordinates can differ and still be considered the same address.
        // Third decimal is 100 meters, so 0.001 means that addresses within 100 meters of each other will be considered the same when checking if route starts or ends at home.
        private const double CoordinateThreshold = 0.001;

        private readonly ILogger _logger;

        public ReimbursementCalculator(IRoute<RouteInformation> route, IPersonService personService, IGenericRepository<Person> personRepo, IGenericRepository<Employment> emplrepo, IGenericRepository<AddressHistory> addressHistoryRepo, ILogger logger, IGenericRepository<RateType> rateTypeRepo)
        {
            _route = route;
            _personService = personService;
            _personRepo = personRepo;
            _emplrepo = emplrepo;
            _addressHistoryRepo = addressHistoryRepo;
            _logger = logger;
            _rateTypeRepo = rateTypeRepo;
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
                _logger.Log(report.FullName + " har et overlap i sin adressehistorik", "web", 3);
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
            double toSubtractFourKmRule = 0;
            double toSubtractHomeRule = 0;
            double toSubtractAltRule = 0;

            //If user indicated to use the Four KM Rule
            if (report.FourKmRule)
            {
                //Take users provided distance from home to border of municipality. If report is from app, use distance provided in report, else use distance saved on person.
                var borderDistance = report.IsFromApp? report.HomeToBorderDistance : person.DistanceFromHomeToBorder;

                //Adjust distance based on if user starts or ends at home
                if (report.StartsAtHome)
                {
                    toSubtractFourKmRule += borderDistance;
                }

                if (report.EndsAtHome)
                {
                    toSubtractFourKmRule += borderDistance;
                }
            }
            else
            {
                //Same logic as above, but uses calculated distance between home and work
                if (report.StartsAtHome)
                {
                    toSubtractHomeRule += homeWorkDistance;
                }

                if (report.EndsAtHome)
                {
                    toSubtractHomeRule += homeWorkDistance;
                }
            }

            switch (report.KilometerAllowance)
            {
                case KilometerAllowance.Calculated:
                    {
                        // Alternative calculating methods.
                        string alternativeCalculationMethod = ConfigurationManager.AppSettings["AlternativeCalculationMethod"];
                        if (alternativeCalculationMethod == null) alternativeCalculationMethod = string.Empty;
                        switch (alternativeCalculationMethod.ToLower())
                        {
                            case "true":    // Should be removed, when our config files is updated from boolean to string (name).
                            case "ndk":
                            case "norddjurs":
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
                                foreach (DriveReportPoint altDriveReportPoint in report.DriveReportPoints) {
                                    altDriveReportPoints.Add(altDriveReportPoint);
                                }

                                // Get if a bike is used for transportation.
                                Boolean altIsBike = _rateTypeRepo.AsQueryable().First(x => x.TFCode.Equals(report.TFCode)).IsBike;
                                DriveReportTransportType altTransportType = (altIsBike == true) ? DriveReportTransportType.Bike : DriveReportTransportType.Car;
                                Double altToSubtract1 = 0.0;
                                Double altToSubtract2 = 0.0;

                                // 1. When starting from home
                                if (    //(report.IsFromApp == false) &&
                                    (report.StartsAtHome == true) &&
                                    (altDriveReportPoints.Count >= 2) &&
                                    ((altDriveReportPoints[1].Latitude != workAddress.Latitude) && (altDriveReportPoints[1].Longitude != workAddress.Longitude))) {
                                    // A) Get the distance between HOME and the first LOCATION (the route in the report).
                                    //    This is used to select the reported route, or the alternative route.
                                    List<Address> altAddressesToHome = new List<Address>();
                                    altAddressesToHome.Add(homeAddress);
                                    altAddressesToHome.Add(altDriveReportPoints[1]);
                                    Double altDistanceToHome = _route.GetRoute(altTransportType, altAddressesToHome).Length;

                                    // A) Get the distance for the entire route (the route in the report).
                                    //    This is used to calculate the distance to subtract.
                                    altAddressesToHome = new List<Address>(altDriveReportPoints);
                                    Double altDistanceA = _route.GetRoute(altTransportType, altAddressesToHome).Length;

                                    // B) Get the distance between WORK and the first LOCATION.
                                    //    This is used to select the reported route, or the alternative route.
                                    List<Address> altAddressesToWork = new List<Address>();
                                    altAddressesToWork.Add(workAddress);
                                    altAddressesToWork.Add(altDriveReportPoints[1]);
                                    Double altDistanceToWork = _route.GetRoute(altTransportType, altAddressesToWork).Length;

                                    // B) Get the distance for the entire alternative route.
                                    //    This is used to calculate the distance to subtract.
                                    altAddressesToWork = new List<Address>(altDriveReportPoints);
                                    altAddressesToWork[0] = workAddress;
                                    Double altDistanceB = _route.GetRoute(altTransportType, altAddressesToWork).Length;

                                    // The current report distance is including the route between HOME and LOCATION.
                                    // Substract the difference, if the distance between WORK and LOCATION is smaller.
                                    if (altDistanceToWork < altDistanceToHome) {
                                        altToSubtract1 = (altDistanceA - altDistanceB);
                                    }

                                }




                                // 2. When finishing at home
                                if (    //(report.IsFromApp == false) &&
                                    (report.EndsAtHome == true) &&
                                    (altDriveReportPoints.Count >= 2) &&
                                    ((altDriveReportPoints[altDriveReportPoints.Count - 2].Latitude != workAddress.Latitude) && (altDriveReportPoints[altDriveReportPoints.Count - 2].Longitude != workAddress.Longitude))) {
                                    // A) Get the distance between the second last LOCATION and HOME (the route in the report).
                                    //    This is used to select the reported route, or the alternative route.
                                    List<Address> altAddressesToHome = new List<Address>();
                                    altAddressesToHome.Add(altDriveReportPoints[altDriveReportPoints.Count - 2]);
                                    altAddressesToHome.Add(homeAddress);
                                    Double altDistanceToHome = _route.GetRoute(altTransportType, altAddressesToHome).Length;

                                    // A) Get the distance for the entire route (the route in the report).
                                    //    This is used to calculate the distance to subtract.
                                    altAddressesToHome = new List<Address>(altDriveReportPoints);
                                    Double altDistanceA = _route.GetRoute(altTransportType, altAddressesToHome).Length;

                                    // B) Get the distance between the second last LOCATION and WORK.
                                    //    This is used to select the reported route, or the alternative route.
                                    List<Address> altAddressesToWork = new List<Address>();
                                    altAddressesToWork.Add(altDriveReportPoints[altDriveReportPoints.Count - 2]);
                                    altAddressesToWork.Add(workAddress);
                                    Double altDistanceToWork = _route.GetRoute(altTransportType, altAddressesToWork).Length;

                                    // B) Get the distance for the entire alternative route.
                                    //    This is used to calculate the distance to subtract.
                                    altAddressesToWork = new List<Address>(altDriveReportPoints);
                                    altAddressesToWork[altAddressesToWork.Count - 1] = workAddress;
                                    Double altDistanceB = _route.GetRoute(altTransportType, altAddressesToWork).Length;

                                    // The current report distance is including the route between HOME and LOCATION.
                                    // Substract the difference, if the distance between WORK and LOCATION is smaller.
                                    if (altDistanceToWork < altDistanceToHome) {
                                        altToSubtract2 = (altDistanceA - altDistanceB);
                                    }
                                }

                                // Subtract.
                                toSubtractAltRule = altToSubtract1 + altToSubtract2;

                                break;
                            case "false":
                            case "":
                                // No alternative calculation method.
                                break;
                            default:
                                // Unknown alternative calculation method.
                                throw new Exception("Invalid alternative calculatio method configured: " + alternativeCalculationMethod);
                                break;
                        }

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

                        //Set distance to corrected
                        report.Distance = manuallyProvidedDrivenDistance - (toSubtractFourKmRule + toSubtractHomeRule + toSubtractAltRule);

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

            if (report.FourKmRule)
            {
                report.Distance -= FourKmAdjustment;
            }

            SetAmountToReimburse(report);

            return report;
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