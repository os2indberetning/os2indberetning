using Core.DomainModel;
using Core.DomainServices.RoutingClasses;

namespace Core.ApplicationServices.Interfaces
{
    public interface IReimbursementCalculator
    {
        DriveReport Calculate(RouteInformation drivenRoute, DriveReport report);
        DriveReport CalculateFourKmRuleForReport(DriveReport report);
        bool AreReportsDrivenOnSameDay(long unixTimeStamp1, long unixTimeStamp2);
    }
}