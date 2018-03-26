using Core.DomainServices.RoutingClasses;
using NUnit.Framework;
using Core.DomainModel;
using Infrastructure.AddressServices.Interfaces;
using Core.DomainServices.Interfaces;
using Core.DomainServices;

namespace Infrastructure.AddressServices.Tests
{
    [TestFixture]
    public class AddressLaunderingTests
    {
        private IAddressLaunderer uut;

        [SetUp]
        public void Setup()
        {
            ICustomSettings customSettings = new CustomSettings();
            IUrlDefinitions urlDefinitions = new UrlDefinitions(customSettings);
            uut = new AddressLaundering(urlDefinitions);
        }

        #region Laundering tests

        [Test]
        public void LaunderAddress_SplittetStreetName_Good()
        {
            //Arrange
            Address address = new Address { StreetName = "Ny Adelgade", StreetNumber = "10", ZipCode = 1104 };
            //Act
            Address result = uut.Launder(address);
            //Assert
            Assert.AreEqual(address, result);
        }

        #endregion

        #region Exception tests

        [Test]
        public void LaunderAddress_ThrowException_E700_BadStreetNr()
        {
            //Arrange
            Address address = new Address { StreetName = "Ny Adelgade", StreetNumber = "999999", ZipCode = 1104 };
            //Act

            //Assert
            Assert.Throws(typeof(AddressLaunderingException),
                () => uut.Launder(address), "Husnummer eksisterer ikke på vejen");
        }

        [Test]
        public void LaunderAddress_ThrowException_E800_BadStreet()
        {
            //Arrange
            Address address = new Address { StreetName = "Ny VejNavn Test Hans", StreetNumber = "10", ZipCode = 1104 };
            //Act

            //Assert
            Assert.Throws(typeof(AddressLaunderingException),
                () => uut.Launder(address), "Vejnavn findes ikke indenfor postdistriktet");
        }

        [Test]
        public void LaunderAddress_ThrowException_E900_BadZipCode()
        {
            //Arrange
            Address address = new Address { StreetName = "Ny Adelgade", StreetNumber = "10", ZipCode = 99999 };
            //Act

            //Assert
            Assert.Throws(typeof(AddressLaunderingException),
                () => uut.Launder(address), "Postnummer eksisterer ikke");
        }

        #endregion
    }
}
