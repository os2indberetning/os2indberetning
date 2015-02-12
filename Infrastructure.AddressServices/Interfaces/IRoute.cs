﻿using System.Collections.Generic;
using Infrastructure.AddressServices.Classes;

namespace Infrastructure.AddressServices.Interfaces
{
    public interface IRoute
    {
        /// <summary>
        /// Returns a route for a set of addresses.
        /// </summary>
        /// <param name="addresses"></param>
        /// <returns></returns>
        RouteInformation GetRoute(IEnumerable<Address> addresses);
    }
}
