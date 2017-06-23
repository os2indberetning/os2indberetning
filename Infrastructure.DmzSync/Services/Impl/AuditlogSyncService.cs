using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using Infrastructure.DmzSync.Services.Interface;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Infrastructure.DmzSync.Services.Impl
{
    class AuditlogSyncService : ISyncService
    {
        private IGenericRepository<Core.DmzModel.Auditlog> _dmzAuditlogRepo;
        private IGenericRepository<Auditlog> _masterAuditlogRepo;
        private readonly ILogger _logger;

        public AuditlogSyncService(IGenericRepository<Core.DmzModel.Auditlog> dmzAuditlogRepo, IGenericRepository<Auditlog> masterAuditlogRepo, ILogger logger)
        {
            _dmzAuditlogRepo = dmzAuditlogRepo;
            _masterAuditlogRepo = masterAuditlogRepo;
            _logger = logger;
        }
        public void ClearDmz()
        {
            throw new NotImplementedException();
        }

        public void SyncFromDmz()
        {
            foreach (var auditlogEntry in _dmzAuditlogRepo.AsQueryable())
            {
                // Check if logentry with same id allready exists in masterrepo, which means it was saved earlier, but deletion from dmz failed.
                if (!_masterAuditlogRepo.AsQueryable().Where(x => x.Id == auditlogEntry.Id).Any()) 
                {
                    Auditlog auditlogToInsert = new Auditlog
                    {
                        Id = auditlogEntry.Id,
                        Date = auditlogEntry.Date,
                        User = auditlogEntry.User,
                        Location = auditlogEntry.Location,
                        Controller = auditlogEntry.Controller,
                        Action = auditlogEntry.Action,
                        Parameters = auditlogEntry.Parameters
                    };
                    // Save logentry if it was not found in master repo, and delete from dmz either way.
                    _masterAuditlogRepo.Insert(auditlogToInsert);
                }
                    _dmzAuditlogRepo.Delete(auditlogEntry); 
            }

            try
            {
                _masterAuditlogRepo.Save();
            }
            catch (Exception e)
            {
                _logger.Error("Error when trying to save auditlogs from dmz to masterdatabase", e);
                _logger.LogForAdmin("Auditlogs fra DMZ serveren kunne ikke gemmes på den interne server, og ligger derfor stadig på dmz serveren");
                return; // Do not delete auditlogs from dmz, if saving in masterrepo failed.
            }

            try
            {
                _dmzAuditlogRepo.Save();
            }
            catch (Exception e)
            {
                _logger.Error("Error when trying to delete auditlogs from dmz", e);
                _logger.LogForAdmin("Auditlogs fra DMZ serveren kunne ikke slettes på DMZ serveren efter de er blevet gemt på den interne server");
                return;
            }
        }

        public void SyncToDmz()
        {
            throw new NotImplementedException();
        }
    }
}
