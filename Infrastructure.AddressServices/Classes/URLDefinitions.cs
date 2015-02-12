﻿
namespace Infrastructure.AddressServices.Classes
{
    class UrlDefinitions
    {
        private const string _launderingURL = @"http://service.adressevask.dk/favrskov-HIEEPKWJED/json?json=";
        private const string _coordinatesURL = @"http://dawa.aws.dk/adresser?";
        private const string _routingURL = @"http://routing.septima.dk/favrskov-HIEEPKWJED/car/viaroute?";

        /// <summary>
        /// URL for address laundering.
        /// </summary>
        public static string LaunderingUrl
        {
            get { return _launderingURL; }
        }

        /// <summary>
        /// URL for address coordinates.
        /// </summary>
        public static string CoordinatesUrl
        {
            get { return _coordinatesURL; }
        }

        /// <summary>
        /// URL for route construction.
        /// </summary>
        public static string RoutingUrl
        {
            get { return _routingURL; }
        }
    }
}
