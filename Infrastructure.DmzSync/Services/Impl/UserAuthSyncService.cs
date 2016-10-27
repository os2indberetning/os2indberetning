using System;
using System.Linq;
using Core.DmzModel;
using Core.DomainModel;
using Core.DomainServices;
using Infrastructure.DmzDataAccess;
using Core.DomainServices.Encryption;
using Infrastructure.DmzSync.Services.Interface;
using Core.ApplicationServices.Logger;

namespace Infrastructure.DmzSync.Services.Impl
{
    public class UserAuthSyncService : ISyncService
    {
        private readonly IGenericRepository<AppLogin> _appLoginRepo;
        private readonly IGenericRepository<UserAuth> _dmzAuthRepo;
        private readonly ILogger _logger;

        public UserAuthSyncService(IGenericRepository<AppLogin> appLoginRepo, IGenericRepository<UserAuth> dmzAuthRepo,  ILogger logger)
        {
            _appLoginRepo = appLoginRepo;
            _dmzAuthRepo = dmzAuthRepo;
            _logger = logger;
        }

        /// <summary>
        /// Syncs all AppLogin from DMZ database to OS2 database.
        /// </summary>
        public void SyncFromDmz()
        {
            throw new NotImplementedException();
        }


        // Dont run this method before syncing people.
        /// <summary>
        /// Syncs all MobileTokens from OS2 database to DMZ database.
        /// Do not run this before having synced people.
        /// </summary>
        public void SyncToDmz()
        {
            var i = 0;
            var logins = _appLoginRepo.AsQueryable().ToList();
            var max = logins.Count;
            _logger.Log($"{this.GetType().Name}. SyncEmployments(). Amount of logins= {max}", "dmz", 3);

            foreach (var login in logins)
            {
                i++;
                if (i % 10 == 0)
                {
                    Console.WriteLine("Syncing UserAuth " + i + " of " + max);
                }

                try
                {
                    var encryptedLogin = Encryptor.EncryptAppLogin(login);

                    var dmzUserAuth = _dmzAuthRepo.AsQueryable().FirstOrDefault(x => x.ProfileId == login.PersonId);

                    var dmzLogin = new UserAuth
                    {
                        UserName = encryptedLogin.UserName,
                        GuId = encryptedLogin.GuId,
                        Password = encryptedLogin.Password,
                        ProfileId = encryptedLogin.PersonId,
                        Salt = encryptedLogin.Salt
                    };

                    if (dmzUserAuth == null)
                    {
                        _dmzAuthRepo.Insert(dmzLogin);
                    }
                    else
                    {
                        dmzUserAuth.UserName = dmzLogin.UserName;
                        dmzUserAuth.GuId = dmzLogin.GuId;
                        dmzUserAuth.Password = dmzLogin.Password;
                        dmzUserAuth.ProfileId = dmzLogin.ProfileId;
                        dmzUserAuth.Salt = dmzLogin.Salt;
                    }
                }catch(Exception ex)
                {
                    _logger.Log($"{this.GetType().Name}. SyncToDmz(). Exception during sync to DMZ for mobileTokens from OS2 database to DMZ database. Login= {login}, LoginID= {login.Id}. Exception: {ex.Message}", "dmz", 1);
                }
            }

            _dmzAuthRepo.Save();
        }

        /// <summary>
        /// Clears all AppLogins in DMZ database.
        /// </summary>
        public void ClearDmz()
        {
            throw new NotImplementedException();
        }

    }

}
