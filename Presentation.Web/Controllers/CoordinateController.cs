using System.Collections.Generic;
using System.Linq;
using System.Web.Http;
using Core.DomainModel;
using Core.DomainServices;
using Core.ApplicationServices.Logger;
using OS2Indberetning.Filters;

namespace OS2Indberetning.Controllers
{
    [AuditlogFilter]
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
            var result = addresses.Select(address => _coordinates.GetAddressCoordinates(address,true)).ToList();
            return Ok(result);
        }


    }
}