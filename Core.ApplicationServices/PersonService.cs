﻿
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Remoting.Messaging;
using System.Web.OData.Routing;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.RoutingClasses;
using Microsoft.Ajax.Utilities;
using Ninject;
using Core.ApplicationServices.Logger;
using Core.DomainServices.Interfaces;

namespace Core.ApplicationServices
{
    public class PersonService : IPersonService
    {
        private readonly IGenericRepository<PersonalAddress> _personalAddressRepo;
        private readonly IGenericRepository<Address> _addressRepo;
        private readonly IGenericRepository<Employment> _employmentRepo;
        private readonly IGenericRepository<OrgUnit> _orgUnitsRepo;

        private readonly IRoute<RouteInformation> _route;
        private readonly IAddressCoordinates _coordinates;
        private readonly ILogger _logger;

        public PersonService(IGenericRepository<PersonalAddress> personalAddressRepo,IGenericRepository<Address> addressRepo, IGenericRepository<Employment> employmentRepo, IGenericRepository<OrgUnit> orgUnitsRepo, IRoute<RouteInformation> route, IAddressCoordinates coordinates, ILogger logger)
        {
            _personalAddressRepo = personalAddressRepo;
            _addressRepo = addressRepo;
            _route = route;
            _coordinates = coordinates;
            _logger = logger;
            _employmentRepo = employmentRepo;
            _orgUnitsRepo = orgUnitsRepo;
        }

        /// <summary>
        /// Removes CPR-number from all People in queryable.
        /// </summary>
        /// <param name="queryable"></param>
        /// <returns>List of People with CPR-number removed.</returns>
        public IQueryable<Person> ScrubCprFromPersons(IQueryable<Person> queryable)
        {
            var set = queryable.ToList();

            // Add fullname to the resultset
            foreach (var person in set)
            {
                person.CprNumber = "";
            }


            return set.AsQueryable();
        }

        /// <summary>
        /// Returns AlternativeHome Address for person if one exists.
        /// Otherwise the real home address is returned.
        /// </summary>
        /// <param name="person"></param>
        /// <returns></returns>
        public virtual PersonalAddress GetHomeAddress(Person person)
        {
            var alternative = _personalAddressRepo.AsQueryable()
                    .FirstOrDefault(x => x.PersonId == person.Id && x.Type == PersonalAddressType.AlternativeHome);

            if (alternative != null)
            {
                AddCoordinatesToAddressIfNonExisting(alternative);
                return alternative;
            }

            var home = _personalAddressRepo.AsQueryable()
                    .FirstOrDefault(x => x.PersonId == person.Id && x.Type == PersonalAddressType.Home);

            if (home != null) { 
                AddCoordinatesToAddressIfNonExisting(home);
            }

            return home;
        }

        public double GetDistanceFromHome(Person person, int addressId)
        {
            var homeAddress = person.PersonalAddresses.AsQueryable().FirstOrDefault(x => x.Type == PersonalAddressType.AlternativeHome);
            // Get primary home address if alternative doesnt exist.
            homeAddress = homeAddress ?? person.PersonalAddresses.AsQueryable().FirstOrDefault(x => x.Type == PersonalAddressType.Home);
            var workAddress = _addressRepo.AsQueryable().FirstOrDefault(a => a.Id == addressId);
            return _route.GetRoute(DriveReportTransportType.Car, new List<Address>() { homeAddress, workAddress }).Length;
        }

        /// <summary>
        /// Performs a coordinate lookup if Address a does not have coordinates.
        /// </summary>
        /// <param name="a"></param>
        private void AddCoordinatesToAddressIfNonExisting(Address a)
        {
            try
            {
                if (string.IsNullOrEmpty(a.Latitude) || a.Latitude.Equals("0"))
                {
                    var result = _coordinates.GetAddressCoordinates(a);
                    a.Latitude = result.Latitude;
                    a.Longitude = result.Longitude;
                    _personalAddressRepo.Save();
                }
            }
            catch (AddressCoordinatesException ade)
            {
                
            }
        }       

        public List<Person> GetEmployeesOfLeader(Person currentUser)
        {
            List<Person> employees = new List<Person>();
            if (currentUser.Employments.Where(e => e.IsLeader).Any() || (currentUser.SubstituteLeaders != null && currentUser.SubstituteLeaders.Count > 0))
            {
                var orgUnits = GetOrgUnitsForLeader(currentUser);
                if (orgUnits != null)
                {
                    var orgUnitsIds = orgUnits.Select(x => x.Id).Distinct();
                    var empDupes = new List<Person>();
                    foreach (var orgUnitId in orgUnitsIds)
                    {
                        var employments = _employmentRepo.AsQueryable().Where(e => e.OrgUnitId == orgUnitId).ToList();
                        foreach (var employment in employments)
                        {
                            empDupes.Add(employment.Person);
                        }
                    }

                    employees = empDupes.Distinct().ToList();
                }
            }           

            return employees;
        }

        public List<OrgUnit> GetOrgUnitsForLeader(Person currentUser)
        {
            var orgUnitsDupes = new List<OrgUnit>();
            if (currentUser.Employments.Where(e => e.IsLeader).Any())
            {
                foreach (Employment e in currentUser.Employments.Where(e => e.IsLeader))
                {
                    OrgUnit org = e.OrgUnit;
                    orgUnitsDupes.Add(org);
                    orgUnitsDupes.AddRange(getChildrenOrgUnits(org.Id));
                }
            }

            if (currentUser.SubstituteLeaders != null && currentUser.SubstituteLeaders.Count > 0)
            {

                foreach (Substitute subFor in currentUser.SubstituteLeaders)
                {
                    Person leader = subFor.Leader;
                    foreach (Employment e in leader.Employments.Where(e => e.IsLeader))
                    {
                        OrgUnit org = e.OrgUnit;
                        orgUnitsDupes.Add(org);
                        orgUnitsDupes.AddRange(getChildrenOrgUnits(org.Id));
                    }
                }
            }

            return orgUnitsDupes.Distinct().ToList();            
        }

        /// <summary>
        /// Find the children orgUnits based on the parent id
        /// </summary>
        /// <param name="orgUnitId"></param>
        /// <returns></returns>
        private List<OrgUnit> getChildrenOrgUnits(int orgUnitId)
        {
            List<OrgUnit> unitsToReturn = new List<OrgUnit>();
            List<OrgUnit> childrenFound = _orgUnitsRepo.AsQueryable().Where(org => org.ParentId == orgUnitId).ToList();
            if (childrenFound != null && childrenFound.Count > 0)
            {
                foreach (var unit in childrenFound)
                {
                    unitsToReturn.Add(unit);
                    unitsToReturn.AddRange(getChildrenOrgUnits(unit.Id));
                }
            }
            return unitsToReturn;
        }
    }
}
