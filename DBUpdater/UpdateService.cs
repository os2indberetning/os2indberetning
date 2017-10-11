﻿using System;
using System.CodeDom.Compiler;
using System.Collections.Generic;
using System.Data;
using System.Data.SqlClient;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using DBUpdater.Models;
using Infrastructure.AddressServices.Interfaces;
using MoreLinq;
using Ninject;
using IAddressCoordinates = Core.DomainServices.IAddressCoordinates;
using Core.ApplicationServices.Interfaces;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices;

namespace DBUpdater
{
    public class UpdateService
    {
        private readonly IGenericRepository<Employment> _emplRepo;
        private readonly IGenericRepository<OrgUnit> _orgRepo;
        private readonly IGenericRepository<Person> _personRepo;
        private readonly IGenericRepository<CachedAddress> _cachedRepo;
        private readonly IGenericRepository<PersonalAddress> _personalAddressRepo;
        private readonly IGenericRepository<Substitute> _subRepo;
        private readonly IAddressLaunderer _actualLaunderer;
        private readonly IAddressCoordinates _coordinates;
        private readonly IDbUpdaterDataProvider _dataProvider;
        private readonly IMailSender _mailSender;
        private readonly IAddressHistoryService _historyService;
        private readonly IGenericRepository<DriveReport> _reportRepo;
        private readonly IDriveReportService _driveService;
        private readonly ISubstituteService _subService;
        private ILogger _logger;

        public UpdateService(IGenericRepository<Employment> emplRepo,
            IGenericRepository<OrgUnit> orgRepo,
            IGenericRepository<Person> personRepo,
            IGenericRepository<CachedAddress> cachedRepo,
            IGenericRepository<PersonalAddress> personalAddressRepo,
            IAddressLaunderer actualLaunderer,
            IAddressCoordinates coordinates,
            IDbUpdaterDataProvider dataProvider,
            IMailSender mailSender,
            IAddressHistoryService historyService,
            IGenericRepository<DriveReport> reportRepo,
            IDriveReportService driveService,
            ISubstituteService subService,
            IGenericRepository<Substitute> subRepo)
        {
            _emplRepo = emplRepo;
            _orgRepo = orgRepo;
            _personRepo = personRepo;
            _cachedRepo = cachedRepo;
            _personalAddressRepo = personalAddressRepo;
            _actualLaunderer = actualLaunderer;
            _coordinates = coordinates;
            _dataProvider = dataProvider;
            _mailSender = mailSender;
            _historyService = historyService;
            _reportRepo = reportRepo;
            _driveService = driveService;
            _subService = subService;
            _subRepo = subRepo;
            _driveService = driveService;
            _logger = NinjectWebKernel.CreateKernel().Get<ILogger>();
        }

        /// <summary>
        /// Splits an address represented as "StreetName StreetNumber" into a list of "StreetName" , "StreetNumber"
        /// </summary>
        /// <param name="address">String to split</param>
        /// <returns>List of StreetName and StreetNumber</returns>
        public List<String> SplitAddressOnNumber(string address)
        {
            var result = new List<string>();
            var index = address.IndexOfAny("0123456789".ToCharArray());
            if (index == -1)
            {
                result.Add(address);
            }
            else if (index == 0)
            {
                // This is for handling a special case of an adress where the street name starts with a number. The street that prompted this fix was "6. Julivej" in Fredericia.
                // This fix may call for a refactor of the entire method to a more generic handling of addresses.

                var addressWithoutFirstChar = address.Substring(1, address.Length - 1);
                var newIndexofFirstDigit = addressWithoutFirstChar.IndexOfAny("0123456789".ToCharArray());

                result.Add(address.Substring(0, newIndexofFirstDigit));
                result.Add(address.Substring(newIndexofFirstDigit + 1, address.Length - (newIndexofFirstDigit + 1)));
            }
            else
            {
                result.Add(address.Substring(0, index - 1));
                result.Add(address.Substring(index, address.Length - index));
            }
            return result;
        }

        /// <summary>
        /// Migrate organisations from Kommune database to OS2 database.
        /// </summary>
        public void MigrateOrganisations()
        {
            _logger.Debug($"{this.GetType().Name}, MigrateOrganisations() started");
            var orgs = _dataProvider.GetOrganisationsAsQueryable().OrderBy(x => x.Level);

            _logger.Debug($"{this.GetType().Name}, MigrateOrganisations(), Amount of orgunits= {orgs.Count()}");
            var i = 0;
            foreach (var org in orgs)
            {
                i++;
                if (i % 10 == 0)
                {
                    
                    Console.WriteLine("Migrating organisation " + i + " of " + orgs.Count() + ".");
                }
                
                var orgToInsert = _orgRepo.AsQueryable().FirstOrDefault(x => x.OrgId == org.LOSOrgId);
                
                var workAddress = GetWorkAddress(org);

                if (workAddress == null)
                {
                    continue;
                }

                if (orgToInsert == null)
                {
                    orgToInsert = _orgRepo.Insert(new OrgUnit());
                    orgToInsert.HasAccessToFourKmRule = false;
                }

                orgToInsert.Level = org.Level;
                orgToInsert.LongDescription = org.Navn;
                orgToInsert.ShortDescription = org.KortNavn;
                orgToInsert.OrgId = org.LOSOrgId;

                var addressChanged = false;
               
                if(workAddress != orgToInsert.Address)
                {
                    addressChanged = true;
                    orgToInsert.Address = workAddress;
                }
                
                if (orgToInsert.Level > 0)
                {
                    orgToInsert.ParentId = _orgRepo.AsQueryable().Single(x => x.OrgId == org.ParentLosOrgId).Id;
                }

                _orgRepo.Save();

                if (addressChanged)
                {
                    workAddress.OrgUnitId = orgToInsert.Id;
                }
            
            }

            _logger.Debug($"{this.GetType().Name}, MigrateOrganisations() done: ");
            Console.WriteLine("Done migrating organisations.");
        }

        /// <summary>
        /// Migrate employees from kommune database to OS2 database.
        /// </summary>
        public void MigrateEmployees()
        {
            _logger.Debug($"{this.GetType().Name}, MigrateEmployees() started");
            foreach (var person in _personRepo.AsQueryable())
            {
                person.IsActive = false;
            }
            _logger.Debug($"{this.GetType().Name}, MigrateEmployees(), All persons IsActive = false. Amount of persons in personrepo= {_personRepo.AsQueryable().Count()}");
            _personRepo.Save();

            var empls = _dataProvider.GetEmployeesAsQueryable();

            var i = 0;
            var distinctEmpls = empls.DistinctBy(x => x.CPR).ToList();

            _logger.Debug($"{this.GetType().Name}, MigrateEmployees() Amount of employees in distinctEmpls: {distinctEmpls.Count()}");
            foreach (var employee in distinctEmpls)
            {
                i++;
                if (i % 10 == 0)
                {
                    Console.WriteLine("Migrating person " + i + " of " + distinctEmpls.Count() + ".");
                }

                var personToInsert = _personRepo.AsQueryable().FirstOrDefault(x => x.CprNumber.Equals(employee.CPR));

                if (personToInsert == null)
                {
                    personToInsert = _personRepo.Insert(new Person());
                    personToInsert.IsAdmin = false;
                    personToInsert.RecieveMail = true;
                }

                personToInsert.CprNumber = employee.CPR ?? "ikke opgivet";
                personToInsert.FirstName = employee.Fornavn ?? "ikke opgivet";
                personToInsert.LastName = employee.Efternavn ?? "ikke opgivet";
                personToInsert.Initials = employee.ADBrugerNavn ?? " ";
                personToInsert.FullName = personToInsert.FirstName + " " + personToInsert.LastName + " [" + personToInsert.Initials + "]";
                personToInsert.Mail = employee.Email ?? "";
                personToInsert.IsActive = true;
            }
            _personRepo.Save();
            _logger.Debug($"{this.GetType().Name}, MigrateEmployees(), Users are active again");

            /**g
             * We need the person id before we can attach personal addresses
             * so we loop through the distinct employees once again and
             * look up the created persons
             */
            i = 0;
            foreach (var employee in distinctEmpls)
            {
                if (i%50 == 0)
                {
                    Console.WriteLine("Adding home address to person " + i + " out of " + distinctEmpls.Count());
                }
                i++;
                var personToInsert = _personRepo.AsQueryable().First(x => x.CprNumber == employee.CPR);
                UpdateHomeAddress(employee, personToInsert.Id);
                if (i % 500 == 0)
                {
                    _personalAddressRepo.Save();
                }
            }
            _logger.Debug($"{this.GetType().Name}, MigrateEmployees(), Home adresses updated.");
            _personalAddressRepo.Save();

            //Sets all employments to end now in the case there was
            //one day where the updater did not run and the employee
            //has been removed from the latest MDM view we are working on
            //The end date will be adjusted in the next loop
            foreach (var employment in _emplRepo.AsQueryable())
            {
                employment.EndDateTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
            }
            _logger.Debug($"{this.GetType().Name}, MigrateEmployees(), All employments end date set to now.");
            _emplRepo.Save();

            i = 0;
            foreach (var employee in empls)
            {
                i++;
                if (i % 10 == 0)
                {
                    Console.WriteLine("Adding employment to person " + i + " of " + empls.Count());
                }
                var personToInsert = _personRepo.AsQueryable().First(x => x.CprNumber == employee.CPR);

                CreateEmployment(employee, personToInsert.Id);
                if (i%500 == 0)
                {
                    _emplRepo.Save();
                }
            }
            _logger.Debug($"{this.GetType().Name}, MigrateEmployees(), Employments added to persons.");
            _personalAddressRepo.Save();
            _emplRepo.Save();

            // Makes all employees wihtout employments inactive.
            foreach(var person in _personRepo.AsQueryable().ToList())
            {
                if (person.Employments == null || !person.Employments.Any())
                {
                    person.IsActive = false;
                }
            }
            _personRepo.Save();

            Console.WriteLine("Before Dirty Adresses");
            var dirtyAddressCount = _cachedRepo.AsQueryable().Count(x => x.IsDirty);
            if (dirtyAddressCount > 0)
            {
                _logger.Debug($"{this.GetType().Name}, MigrateEmployees(), There are {dirtyAddressCount} dirty address(es).");
                foreach (var admin in _personRepo.AsQueryable().Where(x => x.IsAdmin && x.IsActive))
                {
                    _mailSender.SendMail(admin.Mail, "Der er adresser der mangler at blive vasket", "Der mangler at blive vasket " + dirtyAddressCount + "adresser");
                }
            }
            Console.WriteLine("Done migrating employees");
        }

        /// <summary>
        /// Create employment in OS2 database for person identified by personId
        /// </summary>
        /// <param name="empl"></param>
        /// <param name="personId"></param>
        /// <returns></returns>
        public Employment CreateEmployment(Employee empl, int personId)
        {

            if (empl.AnsaettelsesDato == null)
            {
                return null;
            }

            var orgUnit = _orgRepo.AsQueryable().FirstOrDefault(x => x.OrgId == empl.LOSOrgId);

            if (orgUnit == null)
            {
                // Employee employment will not be created, and if the employee does not have any other employments.
                _logger.Error($"{this.GetType().Name}, CreateEmployment(), OrgUnit does not exist. MaNr={empl.MaNr}, orgUnitId={empl.LOSOrgId}");
                _logger.LogForAdmin($"Medarbejderen {empl.Fornavn} {empl.Efternavn} med medarbejdernummer {empl.MaNr} er forsøgt importeret, men er fejlet da organisationsenheden med id {empl.LOSOrgId} ikke kan findes. Medarbejderen er gjort inaktiv, og kan ikke bruge systemet.");
                return null;
            }

            var employment = _emplRepo.AsQueryable().FirstOrDefault(x => x.OrgUnitId == orgUnit.Id && x.EmploymentId == empl.MaNr);

            //It is ok that we do not save after inserting untill
            //we are done as we loop over employments from the view, and 
            //two view employments will not share an employment in the db. 
            if (employment == null)
            {
                employment = _emplRepo.Insert(new Employment());
            }

            employment.OrgUnitId = orgUnit.Id;
            employment.Position = empl.Stillingsbetegnelse ?? "";
            employment.IsLeader = empl.Leder;
            employment.PersonId = personId;
            var startDate = empl.AnsaettelsesDato ?? new DateTime();
            employment.StartDateTimestamp = (Int32)(startDate.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
            employment.ExtraNumber = empl.EkstraCiffer ?? 0;
            employment.EmploymentType = int.Parse(empl.AnsatForhold);
            employment.CostCenter = empl.Omkostningssted;
            employment.EmploymentId = empl.MaNr ?? 0;

            if (empl.OphoersDato != null)
            {
                employment.EndDateTimestamp = (Int32)(((DateTime)empl.OphoersDato).Subtract(new DateTime(1970, 1, 1)).Add(new TimeSpan(1,0,0,0))).TotalSeconds;
            }
            else
            {
                employment.EndDateTimestamp = 0;
            }

            return employment;


        }

        /// <summary>
        /// Updates home address for person identified by personId.
        /// </summary>
        /// <param name="empl"></param>
        /// <param name="personId"></param>
        public void UpdateHomeAddress(Employee empl, int personId)
        {
            if (empl.Adresse == null)
            {
                return;
            }

            var person = _personRepo.AsQueryable().FirstOrDefault(x => x.Id == personId);
            if (person == null)
            {
                _logger.Error($"{this.GetType().Name}, UpdateHomeAddress(), Person does not exist. personId={personId}, MaNr={empl.MaNr}");
                throw new Exception("Person does not exist.");
            }

            var launderer = new CachedAddressLaunderer(_cachedRepo, _actualLaunderer, _coordinates);

            List<string> splitStreetAddress = null;
            try
            {
                splitStreetAddress = SplitAddressOnNumber(empl.Adresse);
            }
            catch (Exception e)
            {
                _logger.Error($"{this.GetType().Name}, UpdateHomeAddress(), Error when splitting address. personId={personId}, address={empl.Adresse}", e);
                throw;
            }

            var addressToLaunder = new Address
            {
                Description = person.FirstName + " " + person.LastName + " [" + person.Initials + "]",
                StreetName = splitStreetAddress.ElementAt(0),
                StreetNumber = splitStreetAddress.Count > 1 ? splitStreetAddress.ElementAt(1) : "1",
                ZipCode = empl.PostNr ?? 0,
                Town = empl.By ?? "",
            };
            addressToLaunder = launderer.Launder(addressToLaunder);

            var launderedAddress = new PersonalAddress()
            {
                PersonId = personId,
                Type = PersonalAddressType.Home,
                StreetName = addressToLaunder.StreetName,
                StreetNumber = addressToLaunder.StreetNumber,
                ZipCode = addressToLaunder.ZipCode,
                Town = addressToLaunder.Town,
                Latitude = addressToLaunder.Latitude ?? "",
                Longitude = addressToLaunder.Longitude ?? "",
                Description = addressToLaunder.Description
            };

            var homeAddr = _personalAddressRepo.AsQueryable().FirstOrDefault(x => x.PersonId.Equals(personId) &&
                x.Type == PersonalAddressType.Home);

            if (homeAddr == null)
            {
                _personalAddressRepo.Insert(launderedAddress);
            }
            else
            {
                if (homeAddr != launderedAddress)
                {
                    // Address has changed
                    // Change type of current (The one about to be changed) home address to OldHome.
                    // Is done in loop because there was an error that created one or more home addresses for the same person.
                    // This will make sure all home addresses are set to old if more than one exists.
                    foreach (var addr in _personalAddressRepo.AsQueryable().Where(x => x.PersonId.Equals(personId) && x.Type == PersonalAddressType.Home).ToList())
                    {
                        addr.Type = PersonalAddressType.OldHome;;
                    }
                    
                    // Update actual current home address.
                    _personalAddressRepo.Insert(launderedAddress);
                    _personalAddressRepo.Save();
                }
            }
        }

        /// <summary>
        /// Gets work address wor organisation.
        /// </summary>
        /// <param name="org"></param>
        /// <returns>WorkAddress</returns>
        public WorkAddress GetWorkAddress(Organisation org)
        {
            var launderer = new CachedAddressLaunderer(_cachedRepo, _actualLaunderer, _coordinates);

            if (org.Gade == null)
            {
                return null;
            }

            var splitStreetAddress = SplitAddressOnNumber(org.Gade);

            var addressToLaunder = new Address
            {
                StreetName = splitStreetAddress.ElementAt(0),
                StreetNumber = splitStreetAddress.Count > 1 ? splitStreetAddress.ElementAt(1) : "1",
                ZipCode = org.Postnr ?? 0,
                Town = org.By,
                Description = org.Navn
            };

            addressToLaunder = launderer.Launder(addressToLaunder);

            var launderedAddress = new WorkAddress()
            {
                StreetName = addressToLaunder.StreetName,
                StreetNumber = addressToLaunder.StreetNumber,
                ZipCode = addressToLaunder.ZipCode,
                Town = addressToLaunder.Town,
                Latitude = addressToLaunder.Latitude ?? "",
                Longitude = addressToLaunder.Longitude ?? "",
                Description = org.Navn
            };

            var existingOrg = _orgRepo.AsQueryable().FirstOrDefault(x => x.OrgId.Equals(org.LOSOrgId));

            // If the address hasn't changed then set the Id to be the same as the existing one.
            // That way a new address won't be created in the database.
            // If the address is not the same as the existing one,
            // Then the Id will be 0, and a new address will be created in the database.
            if (existingOrg != null
                && existingOrg.Address != null
                && existingOrg.Address.StreetName == launderedAddress.StreetName
                && existingOrg.Address.StreetNumber == launderedAddress.StreetNumber
                && existingOrg.Address.ZipCode == launderedAddress.ZipCode
                && existingOrg.Address.Town == launderedAddress.Town
                && existingOrg.Address.Latitude == launderedAddress.Latitude
                && existingOrg.Address.Longitude == launderedAddress.Longitude
                && existingOrg.Address.Description == launderedAddress.Description)
            {
                launderedAddress.Id = existingOrg.AddressId;
            }
            else
            {
                var a = 2;
            }

            return launderedAddress;
        }

        public void UpdateLeadersOnAllReports()
        {
            var i = 0;

            var reports = _reportRepo.AsQueryable().Where(x => x.Employment.OrgUnit.Level > 1).ToList();
            var max = reports.Count();
            foreach (var report in reports)
            {
                if (i % 100 == 0)
                {
                    Console.WriteLine("Updating leaders on report " + i + " of " + max);
                }
                i++;
                report.ResponsibleLeaderId = _driveService.GetResponsibleLeaderForReport(report).Id;
                report.ActualLeaderId = _driveService.GetActualLeaderForReport(report).Id;
                if (i % 1000 == 0)
                {
                    Console.WriteLine("Saving to database");
                    _reportRepo.Save();
                }
            }
            Console.WriteLine("Saving to database");
            _reportRepo.Save();
        }

        /// <summary>
        /// Updates ResponsibleLeader on all reports that had a substitute which expired yesterday or became active today.
        /// </summary>
        public void UpdateLeadersOnExpiredOrActivatedSubstitutes()
        {
            var yesterdayTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds;
            var currentTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

            var endOfDayStamp = _subService.GetEndOfDayTimestamp(yesterdayTimestamp);
            var startOfDayStamp = _subService.GetStartOfDayTimestamp(currentTimestamp);

            var affectedSubstitutes = _subRepo.AsQueryable().Where(s => (s.EndDateTimestamp == endOfDayStamp) || (s.StartDateTimestamp == startOfDayStamp)).ToList();
            Console.WriteLine(affectedSubstitutes.Count() + " substitutes have expired or become active. Updating affected reports.");
            foreach(var sub in affectedSubstitutes)
            {
                _subService.UpdateReportsAffectedBySubstitute(sub);
            }
            _logger.Debug($"{this.GetType().Name}, UpdateLeadersOnExpiredOrActivatedSubstitutes() finished");
        }

        public void AddLeadersToReportsThatHaveNone()
        {
            // Fail-safe as some reports for unknown reasons have not had a leader attached
            Console.WriteLine("Adding leaders to reports that have none");
            var i = 0;
            var reports = _reportRepo.AsQueryable().Where(r => r.ResponsibleLeader == null || r.ActualLeader == null).ToList();
            foreach (var report in reports)
            {
                try
                {
                    i++;
                    Console.WriteLine("Adding leaders to report " + i + " of " + reports.Count);
                    report.ResponsibleLeaderId = _driveService.GetResponsibleLeaderForReport(report).Id;
                    report.ActualLeaderId = _driveService.GetActualLeaderForReport(report).Id;
                    if (i % 100 == 0)
                    {
                        Console.WriteLine("Saving to database");
                        _reportRepo.Save();
                    }
                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().Name}, UpdateLeadersOnExpiredOrActivatedSubstitutes(), Error, report = {report.Id}", e);
                    throw;
                }
            }
            _reportRepo.Save();
            _logger.Debug($"{this.GetType().Name}, AddLeadersToReportsThatHaveNone() finished");
        }
    }
}
