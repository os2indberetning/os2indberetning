using System.Collections.Generic;
using System.Linq;
using Core.DomainModel;

namespace Core.ApplicationServices.Interfaces
{
    public interface IPersonService
    {
        IQueryable<Person> ScrubCprFromPersons(IQueryable<Person> queryable);
        PersonalAddress GetHomeAddress(Person person);
        List<Person> GetEmployeesOfLeader(Person person);
        List<OrgUnit> GetOrgUnitsForLeader(Person person);
        double GetDistanceFromHome(Person person, int addressId);
    }
}
