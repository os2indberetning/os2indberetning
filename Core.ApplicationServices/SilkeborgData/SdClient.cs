using Core.ApplicationServices.SilkeborgData;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Core.ApplicationServices.SdKoersel;
using Core.ApplicationServices.Logger;
using Core.DomainServices.Interfaces;

namespace Core.ApplicationServices
{
    public class SdClient : ISdClient
    {
        private AnsaettelseKoerselOpret20170501PortTypeClient _portTypeClient;
        private ILogger _logger;
        private ICustomSettings _customSettings;

        public SdClient (ILogger logger, ICustomSettings customSettings)
        {
            _logger = logger;
            _customSettings = customSettings;

            try
            {
                _portTypeClient = new AnsaettelseKoerselOpret20170501PortTypeClient();
                _portTypeClient.ClientCredentials.UserName.UserName = _customSettings.SdUsername;
                _portTypeClient.ClientCredentials.UserName.Password = _customSettings.SdPassword;
            }
            catch (Exception e)
            {
                _logger.Error($"{this.GetType().ToString()}, sendDataToSd(), Error when initiating SD client", e);
                throw e;
            }
        }

        public AnsaettelseKoerselOpret20170501Type SendRequest(AnsaettelseKoerselOpretInputType requestData)
        {
            try
            {
                return _portTypeClient.AnsaettelseKoerselOpret20170501Operation(requestData);
            }
            catch (Exception e)
            {
                _logger.Error($"{this.GetType().ToString()}, SendRequest(), Error when sending data to SD", e);
                throw;
            }
        }
    }
}
