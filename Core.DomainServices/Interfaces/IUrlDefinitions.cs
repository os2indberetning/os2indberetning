namespace Core.DomainServices.RoutingClasses
{
    public interface IUrlDefinitions
    {
        string LaunderingUrl { get; }
        string CoordinatesUrl { get; }
        string CoordinateToAddressUrl { get; }
        string RoutingUrl { get; }
        string BikeRoutingUrl { get; }
    }
}