using System;
using System.Collections.Generic;
using System.Linq;
using Core.ApplicationServices;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.DomainServices.RoutingClasses;
using Core.DomainServices.Ínterfaces;
using Ninject;
using NSubstitute;
using NUnit.Framework;

namespace ApplicationServices.Test.ReimbursementCalculatorTest
{
    [TestFixture]
    public class ReimbursementCalculatorTest : ReimbursementCalculatorBaseTest
    {
        /// <summary>
        /// Read
        /// </summary>
        [Test]
        public void Calculate_ReportMethodIsRead_WithoutFourKmRule()
        {
            var report = GetDriveReport();
            report.FourKmRule = false;
            report.StartsAtHome = false;
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.EndsAtHome = false;
            report.Distance = 42;
            report.KilometerAllowance = KilometerAllowance.Read;

            var distance = report.Distance;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            }
            });

            var result = calculator.Calculate(new RouteInformation(){Length = 100}, report);

            Assert.That(distance, Is.EqualTo(result.Distance));
            Assert.That(distance * report.KmRate / 100, Is.EqualTo(result.AmountToReimburse));
        }

        [Test]
        public void Calculate_WithRoundTrip_ShouldDoubleDistance()
        {
            var report = GetDriveReport();
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.Distance = 42;
            report.KilometerAllowance = KilometerAllowance.Read;
            report.IsRoundTrip = true;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            }
            });

            var result = calculator.Calculate(new RouteInformation() { Length = 100 }, report);

            Assert.AreEqual(84, report.Distance);

        }

        [Test]
        public void Calculate_WithoutRoundTrip_ShouldNotDoubleDistance()
        {
            var report = GetDriveReport();
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.Distance = 42;
            report.KilometerAllowance = KilometerAllowance.Read;
            report.IsRoundTrip = false;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            }
            });

            var result = calculator.Calculate(new RouteInformation() { Length = 100 }, report);

            Assert.AreEqual(42, report.Distance);

        }

        [Test]
        public void Calculate_ReportMethodIsRead_WithFourKmRule()
        {
            var report = GetDriveReport();
            report.FourKmRule = true;
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.StartsAtHome = false;
            report.EndsAtHome = false;
            report.Distance = 42;
            report.KilometerAllowance = KilometerAllowance.Read;

            var distance = report.Distance;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            }
            });

            var result = calculator.Calculate(new RouteInformation(), report);

            Assert.That(distance - 4, Is.EqualTo(result.Distance));
            Assert.AreEqual((distance - 4) * report.KmRate / 100, result.AmountToReimburse, 0.001);
        }


        /// <summary>
        /// Calculated without allowance
        /// </summary>
        [Test]
        public void Calculate_ReportMethodIsCalculatedWithoutAllowance_WithoutFourKmRule()
        {
            var report = GetDriveReport();
            report.FourKmRule = false;
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.StartsAtHome = true;
            report.EndsAtHome = true;
            report.Distance = 42;
            report.KilometerAllowance = KilometerAllowance.CalculatedWithoutExtraDistance;

            var distance = report.Distance;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            }
            });

            var result = calculator.Calculate(new RouteInformation(){Length = 42}, report);

            Assert.That(distance * report.KmRate / 100, Is.EqualTo(result.AmountToReimburse));
        }

        [Test]
        public void Calculate_ReportMethodIsCalculatedWithoutAllowance_WithFourKmRule()
        {
            var report = GetDriveReport();
            report.FourKmRule = true;
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.StartsAtHome = true;
            report.EndsAtHome = true;
            report.Distance = 42;
            report.KilometerAllowance = KilometerAllowance.CalculatedWithoutExtraDistance;
            var distance = report.Distance;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            }
            });

            var result = calculator.Calculate(new RouteInformation(){Length = 42}, report);

            Assert.AreEqual(((distance - 4) * report.KmRate / 100), result.AmountToReimburse, 0.001);
        }

        [Test]
        public void ReportWhereAddressHistoryExistsForPeriod_ShouldUseAddressHistory_AndSetTrueRouteBeginsAtHome()
        {
            var historyMockData = new List<AddressHistory>()
            {
                new AddressHistory
            {
                StartTimestamp = (Int32) (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds,
                EndTimestamp = (Int32) (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(-1))).TotalSeconds,
                WorkAddress = new WorkAddress
                {
                    StreetName = "TestWorkAddress",
                    StreetNumber = "123",
                    ZipCode = 1234,
                    Town = "TestTown",
                    Latitude = "5555",
                    Longitude = "5555"
                },
                HomeAddress = new PersonalAddress
                {
                    StreetName = "TestHomeAddress",
                    StreetNumber = "123",
                    ZipCode = 1234,
                    Town = "TestTown",
                    Latitude = "1234",
                    Longitude = "1234",
                    Type = PersonalAddressType.OldHome
                },
                EmploymentId = 1,
                Id = 1
            }
            };

            var emplMockData = new List<Employment>
            {
                new Employment
                {
                    Person = new Person()
                    {
                        Id = 1,
                        FullName = "TestPerson"
                    },
                    OrgUnit = new OrgUnit()
                    {
                        Address = new WorkAddress()
                        {
                             StreetName = "RealTestWork",
                             StreetNumber = "123",
                             ZipCode = 1234,
                             Town = "TestTown",
                             Latitude = "9999",
                             Longitude = "9999"
                        }
                    },
                    PersonId = 1,
                    Id = 1,
                }
            };

            var calculator = GetCalculator(emplMockData, historyMockData);

            var driveReport = new DriveReport()
            {
                Id = 1,
                PersonId = 1,
                EmploymentId = 1,
                DriveDateTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds,
                DriveReportPoints = new List<DriveReportPoint>
                {
                    new DriveReportPoint()
                    {
                        Latitude = "1234",
                        Longitude = "1234"
                    },
                    new DriveReportPoint()
                    {
                        Latitude = "5555",
                        Longitude = "5555"
                    }
                }
            };

            calculator.Calculate(new RouteInformation(){Length = 123}, driveReport);
            Assert.AreEqual(true, driveReport.StartsAtHome);
            Assert.AreEqual(false, driveReport.EndsAtHome);
        }

        [Test]
        public void ReportWhereAddressHistoryExistsForPeriod_ShouldUseAddressHistory_AndSetTrueRouteEndsAtHome()
        {
            var historyMockData = new List<AddressHistory>()
            {
                new AddressHistory
            {
                StartTimestamp = (Int32) (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(1))).TotalSeconds,
                EndTimestamp = (Int32) (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1).AddDays(-1))).TotalSeconds,
                WorkAddress = new WorkAddress
                {
                    StreetName = "TestWorkAddress",
                    StreetNumber = "123",
                    ZipCode = 1234,
                    Town = "TestTown",
                    Latitude = "5555",
                    Longitude = "5555"
                },
                HomeAddress = new PersonalAddress
                {
                    StreetName = "TestHomeAddress",
                    StreetNumber = "123",
                    ZipCode = 1234,
                    Town = "TestTown",
                    Latitude = "1234",
                    Longitude = "1234",
                    Type = PersonalAddressType.OldHome
                },
                EmploymentId = 1,
                Id = 1
            }
            };

            var emplMockData = new List<Employment>
            {
                new Employment
                {
                    Person = new Person()
                    {
                        Id = 1,
                        FullName = "TestPerson"
                    },
                    OrgUnit = new OrgUnit()
                    {
                        Address = new WorkAddress()
                        {
                             StreetName = "RealTestWork",
                             StreetNumber = "123",
                             ZipCode = 1234,
                             Town = "TestTown",
                             Latitude = "9999",
                             Longitude = "9999"
                        }
                    },
                    PersonId = 1,
                    Id = 1,
                }
            };

            var calculator = GetCalculator(emplMockData, historyMockData);

            var driveReport = new DriveReport()
            {
                Id = 1,
                PersonId = 1,
                EmploymentId = 1,
                DriveDateTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds,
                DriveReportPoints = new List<DriveReportPoint>
                {
                    new DriveReportPoint()
                    {
                        Latitude = "5555",
                        Longitude = "5555"
                    },
                     new DriveReportPoint()
                    {
                        Latitude = "1234",
                        Longitude = "1234"
                    }
                }
            };

            calculator.Calculate(new RouteInformation() { Length = 123 }, driveReport);
            Assert.AreEqual(true, driveReport.EndsAtHome);
            Assert.AreEqual(false, driveReport.StartsAtHome);
        }

        [Test]
        public void ReportWhereNoAddressHistoryExistsForPeriod_ShouldNotUseAddressHistory_AndNotSetTrueRouteBeginsOrEndsAtHome()
        {
            var historyMockData = new List<AddressHistory>()
            {
            };

            var emplMockData = new List<Employment>
            {
                new Employment
                {
                    Person = new Person()
                    {
                        Id = 1,
                        FullName = "TestPerson"
                    },
                    OrgUnit = new OrgUnit()
                    {
                        Address = new WorkAddress()
                        {
                             StreetName = "RealTestWork",
                             StreetNumber = "123",
                             ZipCode = 1234,
                             Town = "TestTown",
                             Latitude = "9999",
                             Longitude = "9999"
                        }
                    },
                    PersonId = 1,
                    Id = 1,
                }
            };

            var calculator = GetCalculator(emplMockData, historyMockData);

            var driveReport = new DriveReport()
            {
                Id = 1,
                PersonId = 1,
                EmploymentId = 1,
                DriveDateTimestamp = (Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds,
                DriveReportPoints = new List<DriveReportPoint>
                {
                    new DriveReportPoint()
                    {
                        Latitude = "5555",
                        Longitude = "5555"
                    },
                     new DriveReportPoint()
                    {
                        Latitude = "1234",
                        Longitude = "1234"
                    }
                }
            };

            calculator.Calculate(new RouteInformation() { Length = 123 }, driveReport);
            Assert.AreEqual(false, driveReport.EndsAtHome);
            Assert.AreEqual(false, driveReport.StartsAtHome);
        }


        #region KilometerAllowance = Calculated
        [Test]
        public void CalculateNewReport_StartsAtHome()
        {
            var report = GetDriveReport();
            report.FourKmRule = false;
            report.KilometerAllowance = KilometerAllowance.Calculated;
            report.DriveReportPoints = new List<DriveReportPoint>()
            {
                new DriveReportPoint() // Same as home adress to trigger StartsAtHome
                {
                    Longitude = "12341234",
                    Latitude = "12341234"
                },
                new DriveReportPoint()
                {
                    Longitude = "11111111",
                    Latitude = "11111111"
                }
            };

            var distance = report.Distance;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
                {
                    PersonId = 1,
                    OrgUnit = new OrgUnit()
                    {
                        Address = new WorkAddress()
                        {
                            StreetName = "Katrinebjergvej",
                            StreetNumber = "93B",
                            ZipCode = 8200,
                            Town = "Aarhus N"
                        }
                    },
                    WorkDistanceOverride = 10 // Distance from Home to Work is set to 10
                }
            });

            var route = new RouteInformation() { Length = 42 };

            var result = calculator.Calculate(route, report);

            // The driven distance should be substracted the persons distance from home to work
            Assert.AreEqual(distance - 10, report.Distance);
        }

        [Test]
        public void CalculateNewReport_StartsAtHome_IsRoundTrip()
        {
            var report = GetDriveReport();
            report.FourKmRule = false;
            report.IsRoundTrip = true;
            report.KilometerAllowance = KilometerAllowance.Calculated;
            report.DriveReportPoints = new List<DriveReportPoint>()
            {
                new DriveReportPoint() // Same as home adress to trigger StartsAtHome
                {
                    Longitude = "12341234",
                    Latitude = "12341234"
                },
                new DriveReportPoint()
                {
                    Longitude = "11111111",
                    Latitude = "11111111"
                }
            };

            var distance = report.Distance;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
                {
                    PersonId = 1,
                    OrgUnit = new OrgUnit()
                    {
                        Address = new WorkAddress()
                        {
                            StreetName = "Katrinebjergvej",
                            StreetNumber = "93B",
                            ZipCode = 8200,
                            Town = "Aarhus N"
                        }
                    },
                    WorkDistanceOverride = 10 // Distance from Home to Work is set to 10
                }
            });

            var route = new RouteInformation() { Length = 42 };

            var result = calculator.Calculate(route, report);

            // The driven distance should be substracted the persons distance from home to work
            Assert.AreEqual((distance - 10) * 2, report.Distance);
        }

        [Test]
        public void CalculateNewReport_StartsAndEndsAtHome()
        {
            var report = GetDriveReport();
            report.FourKmRule = false;
            report.KilometerAllowance = KilometerAllowance.Calculated;
            report.DriveReportPoints = new List<DriveReportPoint>()
            {
                new DriveReportPoint() // Same as home adress to trigger StartsAtHome
                {
                    Longitude = "12341234",
                    Latitude = "12341234"
                },
                new DriveReportPoint()
                {
                    Longitude = "12341234",
                    Latitude = "12341234"
                }
            };

            var distance = report.Distance;

            var calculator = GetCalculator(new List<Employment>()
            {
                new Employment()
                {
                    PersonId = 1,
                    OrgUnit = new OrgUnit()
                    {
                        Address = new WorkAddress()
                        {
                            StreetName = "Katrinebjergvej",
                            StreetNumber = "93B",
                            ZipCode = 8200,
                            Town = "Aarhus N"
                        }
                    },
                    WorkDistanceOverride = 10 // Distance from Home to Work is set to 10
                }
            });

            var route = new RouteInformation() { Length = 42 };

            var result = calculator.Calculate(route, report);

            // The driven distance should be substracted the persons distance from home to work
            Assert.AreEqual(distance - (10 * 2), report.Distance);
        }

        [Test]
        public void CalculateNewReport_StartsAtHome_FirstReportOfTheDay_WithFourKmRule()
        {
            var driveDateTimestampToday = ToUnixTime(DateTime.Now);
            var driveDateTimestampYesterday = ToUnixTime(DateTime.Now.AddDays(-1));
            var report = GetDriveReport();
            report.FourKmRule = true;
            report.DriveDateTimestamp = driveDateTimestampToday;
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.DriveReportPoints = new List<DriveReportPoint>()
            {
                new DriveReportPoint() // Same as home adress to trigger StartsAtHome
                {
                    Longitude = "12341234",
                    Latitude = "12341234"
                },
                new DriveReportPoint()
                {
                    Longitude = "11111111",
                    Latitude = "11111111"
                }
            };
            report.KilometerAllowance = KilometerAllowance.Calculated;

            var distance = report.Distance;

            var firstDriveReportOfTheDay = GetDriveReport();
            firstDriveReportOfTheDay.FourKmRule = true;
            firstDriveReportOfTheDay.FourKmRuleDeducted = 4;
            firstDriveReportOfTheDay.DriveDateTimestamp = driveDateTimestampYesterday;

            var calculator = GetCalculator(
                new List<Employment>()
                {
                    new Employment()
                    {
                        OrgUnit = new OrgUnit()
                        {
                            Address = new WorkAddress()
                            {
                                StreetName = "Katrinebjergvej",
                                StreetNumber = "93B",
                                ZipCode = 8200,
                                Town = "Aarhus N"
                            }
                        }
                    }
                },
                new List<DriveReport>()
                {
                    firstDriveReportOfTheDay
                }
            );

            var route = new RouteInformation() { Length = 42 };

            var result = calculator.Calculate(route, report);

            // The driven distance should be deducted the 4 km and the persons distance from hoome to border. Note that the report from yesterday should not affect the report.
            Assert.AreEqual(distance - (4 + 2), report.Distance);
            Assert.AreEqual(4, report.FourKmRuleDeducted);
        }

        [Test]
        public void CalculateNewReport_StartsAtHome_SecondReportOfTheDay_WithFourKmRule()
        {
            var driveDateTimestamp = ToUnixTime(DateTime.Now);
            var report = GetDriveReport();
            report.FourKmRule = true;
            report.DriveDateTimestamp = driveDateTimestamp;
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.DriveReportPoints = new List<DriveReportPoint>()
            {
                new DriveReportPoint() // Same as home adress to trigger StartsAtHome
                {
                    Longitude = "12341234",
                    Latitude = "12341234"
                },
                new DriveReportPoint()
                {
                    Longitude = "11111111",
                    Latitude = "11111111"
                }
            };
            report.KilometerAllowance = KilometerAllowance.Calculated;

            var distance = report.Distance;

            var firstDriveReportOfTheDay = GetDriveReport();
            firstDriveReportOfTheDay.FourKmRule = true;
            firstDriveReportOfTheDay.FourKmRuleDeducted = 4;
            firstDriveReportOfTheDay.DriveDateTimestamp = driveDateTimestamp;

            var calculator = GetCalculator(
                new List<Employment>()
                {
                    new Employment()
                    {
                        OrgUnit = new OrgUnit()
                        {
                            Address = new WorkAddress()
                            {
                                StreetName = "Katrinebjergvej",
                                StreetNumber = "93B",
                                ZipCode = 8200,
                                Town = "Aarhus N"
                            }
                        }
                    }
                },
                new List<DriveReport>()
                {
                    firstDriveReportOfTheDay
                }
            );

            var route = new RouteInformation() { Length = 42 };

            var result = calculator.Calculate(route, report);

            // The driven distance should only be deducted the persons DistanceFromHomeToBorder since 4 km has allready been deducted from an other report from the same day.
            Assert.AreEqual(distance - (2), report.Distance);
            Assert.AreEqual(0, report.FourKmRuleDeducted);
        }

        [Test]
        public void CalculateNewReport_StartsAtHome_SecondReportOfTheDay_WithFourKmRule_OnlySomeOfTheFourKmHasBeenDeducted()
        {
            var driveDateTimestamp = ToUnixTime(DateTime.Now);
            var report = GetDriveReport();
            report.FourKmRule = true;
            report.DriveDateTimestamp = driveDateTimestamp;
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.DriveReportPoints = new List<DriveReportPoint>()
            {
                new DriveReportPoint() // Same as home adress to trigger StartsAtHome
                {
                    Longitude = "12341234",
                    Latitude = "12341234"
                },
                new DriveReportPoint()
                {
                    Longitude = "11111111",
                    Latitude = "11111111"
                }
            };
            report.KilometerAllowance = KilometerAllowance.Calculated;

            var distance = report.Distance;

            var firstDriveReportOfTheDay = GetDriveReport();
            firstDriveReportOfTheDay.FourKmRule = true;
            firstDriveReportOfTheDay.FourKmRuleDeducted = 2.5;
            firstDriveReportOfTheDay.DriveDateTimestamp = driveDateTimestamp;

            var calculator = GetCalculator(
                new List<Employment>()
                {
                    new Employment()
                    {
                        OrgUnit = new OrgUnit()
                        {
                            Address = new WorkAddress()
                            {
                                StreetName = "Katrinebjergvej",
                                StreetNumber = "93B",
                                ZipCode = 8200,
                                Town = "Aarhus N"
                            }
                        }
                    }
                },
                new List<DriveReport>()
                {
                    firstDriveReportOfTheDay
                }
            );

            var route = new RouteInformation() { Length = 42 };

            var result = calculator.Calculate(route, report);

            // The driven distance should only be deducted the persons DistanceFromHomeToBorder and 4 km - the part of the four km that has allready been deducted from the prvious report of the day
            Assert.AreEqual(distance - (2 + (4 - 2.5)), report.Distance);
            Assert.AreEqual(4 - 2.5, report.FourKmRuleDeducted);
        }

        [Test]
        public void CalculateNewReport_StartsAtHome_FirstReportOfTheDay_WithFourKmRule_IsRoundTrip()
        {
            var driveDateTimestampToday = ToUnixTime(DateTime.Now);
            var driveDateTimestampYesterday = ToUnixTime(DateTime.Now.AddDays(-1));
            var report = GetDriveReport();
            report.FourKmRule = true;
            report.IsRoundTrip = true;
            report.DriveDateTimestamp = driveDateTimestampToday;
            report.Employment = new Employment()
            {
                OrgUnit = new OrgUnit()
                {
                    Address = new WorkAddress()
                    {
                        StreetName = "Katrinebjergvej",
                        StreetNumber = "93B",
                        ZipCode = 8200,
                        Town = "Aarhus N"
                    }
                }
            };
            report.DriveReportPoints = new List<DriveReportPoint>()
            {
                new DriveReportPoint() // Same as home adress to trigger StartsAtHome
                {
                    Longitude = "12341234",
                    Latitude = "12341234"
                },
                new DriveReportPoint()
                {
                    Longitude = "11111111",
                    Latitude = "11111111"
                }
            };
            report.KilometerAllowance = KilometerAllowance.Calculated;

            var firstDriveReportOfTheDay = GetDriveReport();
            firstDriveReportOfTheDay.FourKmRule = true;
            firstDriveReportOfTheDay.FourKmRuleDeducted = 4;
            firstDriveReportOfTheDay.DriveDateTimestamp = driveDateTimestampYesterday;

            var calculator = GetCalculator(
                new List<Employment>()
                {
                    new Employment()
                    {
                        OrgUnit = new OrgUnit()
                        {
                            Address = new WorkAddress()
                            {
                                StreetName = "Katrinebjergvej",
                                StreetNumber = "93B",
                                ZipCode = 8200,
                                Town = "Aarhus N"
                            }
                        }
                    }
                },
                new List<DriveReport>()
                {
                    firstDriveReportOfTheDay
                }
            );

            var route = new RouteInformation() { Length = 42 };

            var distance = report.Distance;

            var result = calculator.Calculate(route, report);

            // The driven distance should be deducted the 4 km and two times the persons distance from home to border because of round trip. Note that the report from yesterday should not affect the report.
            Assert.AreEqual((distance * 2) - (4 + (2 * 2)), report.Distance);
            Assert.AreEqual(4, report.FourKmRuleDeducted);
        }

        #endregion

        public long ToUnixTime(DateTime date)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            return Convert.ToInt64((date - epoch).TotalSeconds);
        }
    }
}