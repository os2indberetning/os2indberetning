using Core.ApplicationServices.Logger;
using Core.DomainServices;
using Infrastructure.DmzSync.Services.Interface;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Infrastructure.DmzSync.Services.Impl
{
    class OrgUnitSyncService : ISyncService
    {
        private IGenericRepository<Core.DmzModel.OrgUnit> _dmzOrgUnitRepo;
        private IGenericRepository<Core.DomainModel.OrgUnit> _masterOrgUnitRepo;
        private readonly ILogger _logger;

        public OrgUnitSyncService(IGenericRepository<Core.DmzModel.OrgUnit> orgUnitRepo, IGenericRepository<Core.DomainModel.OrgUnit> masterOrgUnitRepo, ILogger logger)
        {
            _dmzOrgUnitRepo = orgUnitRepo;
            _masterOrgUnitRepo = masterOrgUnitRepo;
            _logger = logger;
        }

        public void ClearDmz()
        {
            throw new NotImplementedException();
        }

        public void SyncFromDmz()
        {
            // We are not interested in migrating OrgUnits from DMZ to os2.
            throw new NotImplementedException();
        }

        public void SyncToDmz()
        {
            int i = 0;
            var orgUnitList = _masterOrgUnitRepo.AsQueryable().ToList();
            var max = orgUnitList.Count;

            _logger.Debug($"{this.GetType().Name}, SyncToDmz(), Amount of orgUnits= {max}");
            foreach (var masterOrgUnit in orgUnitList)
            {
                try
                {
                    i++;
                    if (i % 10 == 0)
                    {
                        Console.WriteLine("Syncing OrgUnit " + i + " of " + max);
                    }

                    var orgUnit = new Core.DmzModel.OrgUnit()
                    {
                        Id = masterOrgUnit.Id,
                        OrgId = masterOrgUnit.OrgId,
                        FourKmRuleAllowed = masterOrgUnit.HasAccessToFourKmRule
                    };

                    var dmzOrgUnit = _dmzOrgUnitRepo.AsQueryable().FirstOrDefault(x => x.Id == masterOrgUnit.Id);

                    if (dmzOrgUnit == null)
                    {
                        _dmzOrgUnitRepo.Insert(orgUnit);
                    }
                    else
                    {
                        dmzOrgUnit.FourKmRuleAllowed = orgUnit.FourKmRuleAllowed;
                    }
                }catch(Exception ex)
                {
                    _logger.Error($"{this.GetType().Name}, SyncToDmz(), Exception during sync to DMZ for orgUnit= {masterOrgUnit}, ID= {masterOrgUnit.Id}.", ex);
                }
            }
            _dmzOrgUnitRepo.Save();
        }
    }
}
