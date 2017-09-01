using Core.ApplicationServices.Logger;
using Core.DomainModel;
using Core.DomainServices;
using Core.DomainServices.Encryption;
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
                    Auditlog auditlogToInsert = new Auditlog();
                    auditlogToInsert.Id = auditlogEntry.Id;
                    auditlogToInsert.Date = auditlogEntry.Date;
                    try
                    {
                        auditlogToInsert.User = Encryptor.DecryptUser(auditlogEntry.User);
                    }
                    catch (Exception)
                    {
                        auditlogToInsert.User = auditlogEntry.User;
                    }
                    auditlogToInsert.Location = auditlogEntry.Location;
                    auditlogToInsert.Controller = auditlogEntry.Controller;
                    auditlogToInsert.Action = auditlogEntry.Action;
                    auditlogToInsert.Parameters = auditlogEntry.Parameters;
                    auditlogToInsert.WrittenToLogFile = false;

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
                _logger.Error($"{ this.GetType().Name},SyncFromDmz(), Error when trying to save auditlogs from dmz to masterdatabase", e);
                _logger.LogForAdmin("Auditlogs fra DMZ serveren kunne ikke gemmes på den interne server, og ligger derfor stadig på dmz serveren");
                return; // Do not delete auditlogs from dmz, if saving in masterrepo failed.
            }

            try
            {
                _dmzAuditlogRepo.Save();
            }
            catch (Exception e)
            {
                _logger.Error($"{ this.GetType().Name},SyncFromDmz(), Error when trying to delete auditlogs from dmz", e);
                _logger.LogForAdmin("Auditlogs fra DMZ serveren kunne ikke slettes på DMZ serveren efter de er blevet gemt på den interne server");
                return;
            }

            WriteAuditRecordsToLogfile();
        }

        private void WriteAuditRecordsToLogfile()
        {
            try
            {
                foreach (var record in _masterAuditlogRepo.AsQueryable().Where(x => !x.WrittenToLogFile).OrderBy(x => x.Id))
                {
                    _logger.AuditLogDMZ(record.Date, record.User, record.Location, record.Controller, record.Action, record.Parameters);
                    record.WrittenToLogFile = true;
                }
            }
            catch (Exception e)
            {
                _logger.Error($"{this.GetType().Name}, WriteAuditRecordsToLogfile(), Error when writing to dmz auditlog file", e);
                _logger.LogForAdmin("Systemet kunne ikke skrive auditlogs fra dmz serveren til logfilen");
            }
            finally
            {
                try
                {
                    _masterAuditlogRepo.Save();
                }
                catch (Exception ex)
                {
                    _logger.Error($"{this.GetType().Name}, WriteAuditRecordsToLogfile(), Error when updating dmz auditlog statuses", ex);
                }
            }
        }

        /// <summary>
        /// This method cleans up the backend table of auditlogs by deleting entries older than three months. It is assumed that any issues with writing to the log file will be fixed within three months.
        /// </summary>
        private void CleanUp()
        {
            foreach(var record in _masterAuditlogRepo.AsQueryable().Where(x => DateTime.Parse(x.Date) < DateTime.Now.AddMonths(-3) && x.WrittenToLogFile)){
                _masterAuditlogRepo.Delete(record);
            }

            try
            {
                _masterAuditlogRepo.Save();
            }
            catch (Exception e)
            {
                _logger.Error($"{this.GetType().Name}, CleanUp(), Error when deleting old dmz auditlog records", e);
                _logger.LogForAdmin("Systemet kunne ikke rydde op i gamle dmz auditlogs");
                throw;
            }
        }

        public void SyncToDmz()
        {
            throw new NotImplementedException();
        }
    }
}
