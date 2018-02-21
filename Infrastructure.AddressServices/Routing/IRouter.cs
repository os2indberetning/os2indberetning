using Core.DomainModel;
using Core.DomainServices.RoutingClasses;
using System.Collections.Generic;

namespace Infrastructure.AddressServices.Routing
{
    public interface IRouter
    {
        IEnumerable<RouteInformation> GetRoute(DriveReportTransportType transportType, IEnumerable<Coordinates> routeCoordinates);
    }
}