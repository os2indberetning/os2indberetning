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

        public OrgUnitSyncService(IGenericRepository<Core.DmzModel.OrgUnit> orgUnitRepo, IGenericRepository<Core.DomainModel.OrgUnit> masterOrgUnitRepo)
        {
            _dmzOrgUnitRepo = orgUnitRepo;
            _masterOrgUnitRepo = masterOrgUnitRepo;
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

            foreach (var masterOrgUnit in orgUnitList)
            {
                i++;
                if (i % 10 == 0)
                {
                    Console.WriteLine("Syncing OrgUnit " + i + " of " + max);
                }

                var orgUnit = new Core.DmzModel.OrgUnit()
                {
                    OrgId = masterOrgUnit.OrgId,
                    FourKmRuleAllowed = masterOrgUnit.HasAccessToFourKmRule
                };

                var dmzOrgUnit = _dmzOrgUnitRepo.AsQueryable().FirstOrDefault(x => x.OrgId == orgUnit.OrgId);

                if(dmzOrgUnit == null)
                {
                    _dmzOrgUnitRepo.Insert(orgUnit);
                }
                else
                {
                    dmzOrgUnit.FourKmRuleAllowed = orgUnit.FourKmRuleAllowed;
                }
            }
            _dmzOrgUnitRepo.Save();
        }
    }
}
