﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Core.ApplicationServices.Interfaces;
using Core.DmzModel;
using Core.DomainModel;
using Core.DomainServices;
using Infrastructure.DataAccess;
using Infrastructure.DmzDataAccess;
using Core.DomainServices.Encryption;
using Infrastructure.DmzSync.Services.Interface;
using Employment = Core.DmzModel.Employment;
using Core.ApplicationServices.Logger;

namespace Infrastructure.DmzSync.Services.Impl
{
    public class PersonSyncService : ISyncService
    {
        private IGenericRepository<Profile> _dmzProfileRepo;
        private IGenericRepository<Person> _masterPersonRepo;
        private IGenericRepository<Employment> _masterEmploymentRepo;
        private readonly IPersonService _personService;
        private readonly ILogger _logger;

        public PersonSyncService(IGenericRepository<Profile> dmzProfileRepo, IGenericRepository<Person> masterPersonRepo, IGenericRepository<Employment> masterEmploymentRepo, IPersonService personService, ILogger logger)
        {
            _dmzProfileRepo = dmzProfileRepo;
            _masterPersonRepo = masterPersonRepo;
            _masterEmploymentRepo = masterEmploymentRepo;
            _personService = personService;
            _logger = logger;
        }

        /// <summary>
        /// Not implemented.
        /// </summary>
        public void SyncFromDmz()
        {
            // We are not interested in migrating profiles from DMZ to os2.
            throw new NotImplementedException();
        }


        /// <summary>
        /// Syncs all People from OS2 database to DMZ database.
        /// </summary>
        public void SyncToDmz()
        {
            var i = 0;
            var personList = _masterPersonRepo.AsQueryable().ToList();
            var max = personList.Count;

            _logger.Debug($"{this.GetType().Name}, SyncToDmz(), Amount of persons= {max}");

            foreach (var person in personList)  
            {
                i++;
                if (i%10 == 0)
                {
                    Console.WriteLine("Syncing person " + i + " of " + max);
                }

                try
                {
                    var dmzPerson = _dmzProfileRepo.AsQueryable().FirstOrDefault(x => x.Id == person.Id);

                    var homeAddress = _personService.GetHomeAddress(person);

                    var profile = new Profile
                    {
                        Id = person.Id,
                        FirstName = person.FirstName,
                        LastName = person.LastName,
                        HomeLatitude = homeAddress != null ? homeAddress.Latitude : "0",
                        HomeLongitude = homeAddress != null ? homeAddress.Longitude : "0",
                        Initials = person.Initials,
                        FullName = person.FullName,
                        IsActive = person.IsActive
                    };

                    profile = Encryptor.EncryptProfile(profile);

                    if (dmzPerson == null)
                    {
                        _dmzProfileRepo.Insert(profile);
                    }
                    else
                    {
                        dmzPerson.FirstName = profile.FirstName;
                        dmzPerson.LastName = profile.LastName;
                        dmzPerson.HomeLatitude = profile.HomeLatitude;
                        dmzPerson.HomeLongitude = profile.HomeLongitude;
                        dmzPerson.Initials = profile.Initials;
                        dmzPerson.FullName = profile.FullName;
                        dmzPerson.IsActive = profile.IsActive;
                    }
                }catch(Exception ex)
                {
                    _logger.Error($"{this.GetType().Name}, SyncToDmz(), Exception during sync to DMZ for personId= {person.Id}", ex);
                }
            }
             _dmzProfileRepo.Save();
            SyncEmployments();

        }

        /// <summary>
        /// Syncs employments for all People in OS2 database to DMZ database.
        /// </summary>
        private void SyncEmployments()
        {
            var i = 0;
            var personList = _masterPersonRepo.AsQueryable().ToList();
            var max = personList.Count;
            _logger.Debug($"{this.GetType().Name}, SyncEmployments(), Amount of persons= {max}");
            foreach (var person in personList)
            {
                i++;
                if (i % 10 == 0)
                {
                    Console.WriteLine("Syncing employments for person " + i + " of " + max);
                }
                try {
                    var pers = _dmzProfileRepo.AsQueryable().First(x => x.Id == person.Id);

                    var employments = pers.Employments;

                    // Migrate list of employees as the model is not the same in DMZ and OS2.
                    foreach (var masterEmployment in person.Employments)
                    {
                        var dmzEmployment = employments.FirstOrDefault(x => x.Id == masterEmployment.Id);

                        var employment = new Employment
                        {
                            Id = masterEmployment.Id,
                            ProfileId = masterEmployment.PersonId,
                            ManNr = masterEmployment.EmploymentId.ToString(),
                            StartDateTimestamp = masterEmployment.StartDateTimestamp,
                            EndDateTimestamp = masterEmployment.EndDateTimestamp,
                            EmploymentPosition =
                                    masterEmployment.Position + " - " + masterEmployment.OrgUnit.LongDescription,
                            OrgUnitId = masterEmployment.OrgUnitId
                        };

                        employment = Encryptor.EncryptEmployment(employment);

                        if (dmzEmployment == null)
                        {
                            employments.Add(employment);
                        }
                        else
                        {
                            dmzEmployment.ProfileId = employment.ProfileId;
                            dmzEmployment.ManNr = employment.ManNr;
                            dmzEmployment.StartDateTimestamp = masterEmployment.StartDateTimestamp;
                            dmzEmployment.EndDateTimestamp = masterEmployment.EndDateTimestamp;
                            dmzEmployment.EmploymentPosition = employment.EmploymentPosition;
                            dmzEmployment.OrgUnitId = employment.OrgUnitId;
                        }

                    }
                }
                catch (Exception ex)
                {
                    _logger.Error($"{this.GetType().Name}, SyncToDmz(), Exception during sync to DMZ for employments from OS2 database to DMZ database, personId= {person.Id}. Exception: {ex.Message}", ex);
                }
            }
          
            _dmzProfileRepo.Save();
        }

        /// <summary>
        /// Not implemented
        /// </summary>
        public void ClearDmz()
        {
            throw new NotImplementedException("This service is no longer used");
        }

    }

}
