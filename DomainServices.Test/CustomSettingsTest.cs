using Core.DomainServices;
using Core.DomainServices.Interfaces;
using NUnit.Framework;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace DomainServices.Test
{
    [TestFixture]
    public class CustomSettingsTest
    {
        private ICustomSettings _customSettings;

        [SetUp]
        public void Setup()
        {
            _customSettings = new CustomSettings();
        }

        [Test]
        public void GetSettings()
        {
            Assert.AreEqual(true, _customSettings.SdIsEnabled);
            Assert.AreEqual("usernametest", _customSettings.SdUsername);
            Assert.AreEqual("passwordtest", _customSettings.SdPassword);
            Assert.AreEqual("institutionnumbertest", _customSettings.SdInstitutionNumber);

            Assert.AreEqual("municipalitytest", _customSettings.Municipality);
        }
    }
}
