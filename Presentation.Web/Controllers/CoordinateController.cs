using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;
using Core.DomainModel;
using Core.DomainServices;
using Core.ApplicationServices.Logger;

namespace OS2Indberetning.Controllers
{
    public class CoordinateController : ApiController
    {
        private readonly IAddressCoordinates _coordinates;
        private readonly ILogger _logger;
        /// <summary>
        /// 
        /// </summary>
        /// <param name="coordinates"></param>
        public CoordinateController(IAddressCoordinates coordinates, ILogger log)
        {
            _coordinates = coordinates;
            _logger = log;
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="addresses"></param>
        /// <returns></returns>
        public IHttpActionResult SetCoordinatesOnAddressList(IEnumerable<Address> addresses)
        {
            _logger.Log("CoordinateController. SetCoordinatesOnAddressList() initial.", "web", 3);
            var result = addresses.Select(address => _coordinates.GetAddressCoordinates(address,true)).ToList();
            _logger.Log("CoordinateController. SetCoordinatesOnAddressList() end. AdressCoordinates=" + result, "web", 3);
            return Ok(result);
        }


    }
}