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
            var result2 = Utilities.FromUnixTime(result);

            // Assert
            Assert.AreEqual(expectedUnixTimestamp, result);
            Assert.AreEqual(datetime, result2);
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

        [Test]
        public void TestToUnix_FromUnix_DateTimeNow()
        {
            // Arranage
            var dateTimeNow = new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, DateTime.Now.Hour, DateTime.Now.Minute, DateTime.Now.Second, 0);           

            // Act
            var resultUnix = Utilities.ToUnixTime(dateTimeNow);
            var resultDate = Utilities.FromUnixTime(resultUnix);

            // Assert
            Assert.AreEqual(dateTimeNow, resultDate);
        }
    }
}
