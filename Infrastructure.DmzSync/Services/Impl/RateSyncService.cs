using System;
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
    public class RateSyncService : ISyncService
    {
        private IGenericRepository<Core.DmzModel.Rate> _dmzRateRepo;
        private IGenericRepository<Core.DomainModel.Rate> _masterRateRepo;
        private readonly ILogger _logger;

        public RateSyncService(IGenericRepository<Core.DmzModel.Rate> dmzRateRepo, IGenericRepository<Core.DomainModel.Rate> masterRateRepo, ILogger logger)
        {
            _dmzRateRepo = dmzRateRepo;
            _masterRateRepo = masterRateRepo;
            _logger = logger;
        }

        /// <summary>
        /// Not implemented.
        /// </summary>
        public void SyncFromDmz()
        {
            // We are not interested in migrating rates from DMZ to os2.
            throw new NotImplementedException();
        }


        /// <summary>
        /// Syncs all rates from OS2 database to DMZ database.
        /// </summary>
        public void SyncToDmz()
        {
            var i = 0;
            var currentYear = DateTime.Now.Year;
            var rateList = _masterRateRepo.AsQueryable().Where(x => x.Active && x.Year == currentYear).ToList();
            var max = rateList.Count;

            _logger.Log($"{this.GetType().Name}. SyncEmployments(). Amount of rates= {max}", "dmz", 3);

            foreach (var masterRate in rateList)  
            {
                i++;
                if (masterRate.Active)
                {
                    if (i % 10 == 0)
                    {
                        Console.WriteLine("Syncing rate " + i + " of " + max);
                    }

                    try
                    {
                        var rate = new Core.DmzModel.Rate()
                        {
                            Id = masterRate.Id,
                            Description = masterRate.Type.Description,
                            Year = masterRate.Year.ToString(),
                            IsActive = true
                        };

                        var dmzRate = _dmzRateRepo.AsQueryable().FirstOrDefault(x => x.Id == rate.Id);

                        if (dmzRate == null)
                        {
                            _dmzRateRepo.Insert(rate);
                        }
                        else
                        {
                            dmzRate.Description = rate.Description;
                            dmzRate.Year = rate.Year;
                        }
                    }catch(Exception ex)
                    {
                        _logger.Log($"{this.GetType().Name}. SyncToDmz(). Exception during sync to DMZ for rates from OS2 database to DMZ database. Rate= {masterRate}, ID= {masterRate.Id}. Exception: {ex.Message}", "dmz", 1);
                    }
                }
            }
             _dmzRateRepo.Save();
        }


        /// <summary>
        /// Clears DMZ database of all rates.
        /// </summary>
        public void ClearDmz()
        {
            throw new NotImplementedException("This service is no longer used");
        }
    }
}
