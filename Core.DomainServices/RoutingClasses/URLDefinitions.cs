using Core.DomainServices.Interfaces;
using System.Configuration;

namespace Core.DomainServices.RoutingClasses
{
    public class UrlDefinitions : IUrlDefinitions
    {
        private const string _coordinateToAddressUrl = @"https://dawa.aws.dk/adgangsadresser/reverse?";
        private const string _coordinatesURL = @"https://dawa.aws.dk/adresser?";
        private const string _launderingUrl = @"https://dawa.aws.dk/adgangsadresser?";
        private ICustomSettings _customSettings;

        public UrlDefinitions(ICustomSettings customSettings)
        {
            _customSettings = customSettings;
        }

        /// <summary>
        /// URL for address laundering.
        /// </summary>
        public string LaunderingUrl
        {
            get { return _launderingUrl; }

        }

        /// <summary>
        /// URL for address coordinates.
        /// </summary>
        public string CoordinatesUrl
        {
            get { return _coordinatesURL; }
        }

        public string CoordinateToAddressUrl
        {
            get { return _coordinateToAddressUrl; }
        }

        /// <summary>
        /// URL for car route construction.
        /// </summary>
        public string RoutingUrl
        {
            get
            {
                var apiKey = _customSettings.SeptimaApiKey;
                return "http://new-routing.septima.dk/" + apiKey + "/car/viaroute?";
            }
        }

        /// <summary>
        /// URL for bike route construction.
        /// </summary>
        public string BikeRoutingUrl
        {
            get
            {
                var apiKey = _customSettings.SeptimaApiKey;
                return "http://new-routing.septima.dk/" + apiKey + "/bicycle/viaroute?";
            }
        }
    }
}
