﻿using System;
using System.Collections.Generic;
using System.Linq;
using Core.ApplicationServices.FileGenerator;
using Core.DomainModel;
using NUnit.Framework;
using Core.ApplicationServices.Logger;
using Core.DomainServices;

namespace ApplicationServices.Test.FileGenerator
{
    [TestFixture]
    public class ReportGeneratorTest
    {
        [Test]
        public void WriteRecordsShouldAlterReportStatusToInvoiced()
        {
            var repoMock = new ReportRepositoryMock();
            var reportGenerator = new ReportGenerator(repoMock, new FileWriterMock(), new Logger(), new CustomSettings());

            Assert.AreEqual(ReportStatus.Accepted, repoMock.Report1Accepted.Status, "Status should be accepted before being passed to file generator");
            Assert.AreEqual(ReportStatus.Accepted, repoMock.Report2Accepted.Status, "Status should be accepted before being passed to file generator");
            Assert.AreEqual(ReportStatus.Rejected, repoMock.Report3Rejected.Status, "Status should be rejected before being passed to file generator");
            Assert.AreEqual(ReportStatus.Invoiced, repoMock.Report4Invoiced.Status, "Status should be invoiced before being passed to file generator"); 
            Assert.AreEqual(ReportStatus.Accepted, repoMock.Report5Accepted.Status, "Status should be accepted before being passed to file generator");
            Assert.AreEqual(ReportStatus.Accepted, repoMock.Report6Accepted.Status, "Status should be accepted before being passed to file generator");
            Assert.AreEqual(ReportStatus.Accepted, repoMock.Report7Accepted.Status, "Status should be accepted before being passed to file generator");
            Assert.AreEqual(ReportStatus.Accepted, repoMock.Report8Accepted.Status, "Status should be accepted before being passed to file generator");

            reportGenerator.WriteRecordsToFileAndAlterReportStatus();

            Assert.AreEqual(ReportStatus.Invoiced, repoMock.Report1Accepted.Status, "Status should be changed to invoiced after being passed to file generator");
            Assert.AreEqual(ReportStatus.Invoiced, repoMock.Report2Accepted.Status, "Status should be changed to invoiced after being passed to file generator");
            Assert.AreEqual(ReportStatus.Rejected, repoMock.Report3Rejected.Status, "Status should not be changed by being passed to file generator");
            Assert.AreEqual(ReportStatus.Invoiced, repoMock.Report4Invoiced.Status, "Status should not be changed by being passed to file generator");
            Assert.AreEqual(ReportStatus.Invoiced, repoMock.Report5Accepted.Status, "Status should be changed to invoiced after by being passed to file generator");
            Assert.AreEqual(ReportStatus.Invoiced, repoMock.Report6Accepted.Status, "Status should be changed to invoiced after by being passed to file generator");
            Assert.AreEqual(ReportStatus.Invoiced, repoMock.Report7Accepted.Status, "Status should be changed to invoiced after by being passed to file generator");
            Assert.AreEqual(ReportStatus.Invoiced, repoMock.Report8Accepted.Status, "Status should be changed to invoiced after by being passed to file generator");
        }

        [Test]
        public void WriteRecordsShouldPass4RecordsToTheWriter()
        {
            var repoMock = new ReportRepositoryMock();
            var writerMock = new FileWriterMock();
            var reportGenerator = new ReportGenerator(repoMock, writerMock, new Logger(), new CustomSettings());

            Assert.AreEqual(0, writerMock.RecordList.Count, "The writer should have an empty list before the generator is called");
            reportGenerator.WriteRecordsToFileAndAlterReportStatus();

            Assert.AreEqual(5, writerMock.RecordList.Count,
                "The writer should receive three elements when it is called by the generator.");
        }

        [Test]
        public void WriteRecordShouldGroupReportsWithSameTFCodeAndSameMonth()
        {
            var repoMock = new ReportRepositoryMock();
            var writerMock = new FileWriterMock();
            var reportGenerator = new ReportGenerator(repoMock, writerMock, new Logger(), new CustomSettings());

            Assert.AreEqual(0, writerMock.RecordList.Count, "The writer should have an empty list before the generator is called");
            reportGenerator.WriteRecordsToFileAndAlterReportStatus();

            Assert.AreEqual(600, writerMock.RecordList.ElementAt(0).ReimbursementDistance, "Total distance of person 1s reports should be 600");
            Assert.AreEqual(600, writerMock.RecordList.ElementAt(1).ReimbursementDistance, "Total distance of person 2s reports from march with tf code 310-3 should be 600");
            Assert.AreEqual(1400, writerMock.RecordList.ElementAt(2).ReimbursementDistance, "Total distance of person 2s reports from march with tf code 310-4 should be 1400");
            Assert.AreEqual(600, writerMock.RecordList.ElementAt(3).ReimbursementDistance, "Total distance of person 2s reports from april with tf code 310-4 should be 600");
            Assert.AreEqual(600, writerMock.RecordList.ElementAt(4).ReimbursementDistance, "Total distance of person 2s reports from april with tf code 310-9 should be 600");
        }

        [Test]
        public void WriteRecordShouldSetTheDateToBeTheLastInTheMonth()
        {
            var repoMock = new ReportRepositoryMock();
            var writerMock = new FileWriterMock();
            var reportGenerator = new ReportGenerator(repoMock, writerMock, new Logger(), new CustomSettings());

            Assert.AreEqual(0, writerMock.RecordList.Count, "The writer should have an empty list before the generator is called");
            reportGenerator.WriteRecordsToFileAndAlterReportStatus();

            Assert.AreEqual(new DateTime(2015, 3, 31).Year, writerMock.RecordList.ElementAt(0).Date.Year, "The date of the first record should be March 31 2015");
            Assert.AreEqual(new DateTime(2015, 3, 31).Month, writerMock.RecordList.ElementAt(0).Date.Month, "The date of the first record should be March 31 2015");
            Assert.AreEqual(new DateTime(2015, 3, 31).Date, writerMock.RecordList.ElementAt(0).Date.Date, "The date of the first record should be March 31 2015");
            Assert.AreEqual(new DateTime(2015, 4, 30).Year, writerMock.RecordList.ElementAt(3).Date.Year, "The date of the first record should be April 30 2015");
            Assert.AreEqual(new DateTime(2015, 4, 30).Month, writerMock.RecordList.ElementAt(3).Date.Month, "The date of the first record should be April 30 2015");
            Assert.AreEqual(new DateTime(2015, 4, 30).Date, writerMock.RecordList.ElementAt(3).Date.Date, "The date of the first record should be April 30 2015");
        }

        [Test]
        public void WriteRecordShouldTwoReportsIfTFCodeOptionalIsSet()
        {
            var repoMock = new ReportRepositoryMock();
            var writerMock = new FileWriterMock();
            var reportGenerator = new ReportGenerator(repoMock, writerMock, new Logger(), new CustomSettings());

            var preRepoMockCount = repoMock.AsQueryable().Count();

            Assert.AreEqual(0, writerMock.RecordList.Count, "The writer should have an empty list before the generator is called");
            reportGenerator.WriteRecordsToFileAndAlterReportStatus();

            Assert.AreEqual(repoMock.Report9Accepted.TFCodeOptional, writerMock.RecordList.ElementAt(2).TFCode, "The TFCodeOptional from Report9 should be the same as TFCode on record 3");
            Assert.AreEqual(new DateTime(2015, 3, 31).Date, writerMock.RecordList.ElementAt(2).Date.Date, "The date of the first record should be March 31 2015");
            Assert.AreEqual(1400, writerMock.RecordList.ElementAt(2).ReimbursementDistance, "The distance of the third record should be 1400");

            Assert.AreEqual(repoMock.Report9Accepted.TFCode, writerMock.RecordList.ElementAt(4).TFCode, "The TFCode from Report9 should be the same as TFCode on record 5");
            Assert.AreEqual(new DateTime(2015, 3, 31).Date, writerMock.RecordList.ElementAt(4).Date.Date, "The date of the fifth record should be March 31 2015");
            Assert.AreEqual(600, writerMock.RecordList.ElementAt(4).ReimbursementDistance, "The distance of the fifth record should be 600");

            Assert.AreEqual(preRepoMockCount, repoMock.AsQueryable().Count(), "Reports created with Optional TFCode should not be saved");
        }

        [Test]
        public void TestReceiveReportsToInvoiceSD()
        {
            var repoMock = new ReportRepositoryMock();
            var writerMock = new FileWriterMock();
            var reportGenerator = new ReportGenerator(repoMock, writerMock, new Logger(), new CustomSettings());

            var acceptedReports = repoMock.AsQueryable().Where(x => x.Status == ReportStatus.Accepted && x.Distance > 0).ToList();
            var preRepoMockCount = acceptedReports.Count();

            var reportToInvoice = reportGenerator.ReceiveReportsToInvoiceSD();

            Assert.AreNotEqual(preRepoMockCount, reportToInvoice.Count, "The number of reports before receiving reports to invoice should contain less reports");
            Assert.AreEqual(8, reportToInvoice.Count, "The number of reports to invoice should be 7");
            
            var reportsWithTFCode = acceptedReports.Where(x => x.TFCode == "310-4").ToList();
            var reportsToInvoiceWithTFCode = reportToInvoice.Where(x => x.TFCode == "310-4").ToList();
            Assert.AreEqual(5, reportsWithTFCode.Count, "The number of accepted reports before, should be 5");
            Assert.AreEqual(6, reportsToInvoiceWithTFCode.Count, "The number of accepted reports after, should be 6");
        }
    }

    class FileWriterMock : IReportFileWriter
    {
        public ICollection<FileRecord> RecordList = new List<FileRecord>(); 

        public bool WriteRecordsToFile(ICollection<FileRecord> recordList)
        {
            RecordList = recordList;
            return true;
        }
    }
}
