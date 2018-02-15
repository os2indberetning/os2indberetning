using Core.DomainServices;
using NUnit.Framework;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace DomainServices.Test
{
    [TestFixture]
    public class UtilititesTest
    {
        [Test]
        public void TestToUnixTime_NoDaylightSavings()
        {
            // Arrange
            var datetime = new DateTime(2018, 2, 15, 12, 00, 00);
            var expectedUnixTimestamp = 1518692400;

            // Act
            var result = Utilities.ToUnixTime(datetime);

            // Assert
            Assert.AreEqual(expectedUnixTimestamp, result);
        }

        [Test]
        public void TestToUnixTime_DaylightSavings()
        {
            // Arrange
            var datetime = new DateTime(2018, 7, 15, 12, 00, 00);
            var expectedUnixTimestamp = 1531648800;

            // Act
            var unixtimestamp = Utilities.ToUnixTime(datetime);

            // Assert
            Assert.AreEqual(expectedUnixTimestamp, unixtimestamp);
        }

        [Test]
        public void TestFromUnixTime_NoDaylightSavings()
        {
            // Arrange
            var unixTimestamp = 1518692400;
            var expectedDatetime = new DateTime(2018, 2, 15, 12, 00, 00);

            // Act
            var result = Utilities.FromUnixTime(unixTimestamp);

            // Assert
            Assert.AreEqual(expectedDatetime, result);
        }

        [Test]
        public void TestFromUnixTime_DaylightSavings()
        {
            // Arrange
            var unixTimestamp = 1531648800;
            var expectedDatetime = new DateTime(2018, 7, 15, 12, 00, 00);

            // Act
            var result = Utilities.FromUnixTime(unixTimestamp);

            // Assert
            Assert.AreEqual(expectedDatetime, result);
        }
    }
}
