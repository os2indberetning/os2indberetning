using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Core.DomainModel;
using Core.DomainServices.RoutingClasses;

namespace Core.DomainServices.Interfaces
{
    public interface IAddressCoordinates
    {
        Address GetAddressFromCoordinates(Address addressCoord);
        Address GetAddressCoordinates(Address address, bool correctAddresses = false);
        Coordinates GetCoordinates(Address address, Coordinates.CoordinatesType type, bool correctAddresses = false);
    }
}
