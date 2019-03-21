using System;
using System.Collections.Generic;
using System.Linq;
using Core.DomainModel;
using Core.DomainServices;
using Core.ApplicationServices.Logger;
using Core.DomainServices.Interfaces;

namespace Core.ApplicationServices.FileGenerator
{
    public class ReportGenerator : IReportGenerator
    {
        private readonly IGenericRepository<DriveReport> _reportRepo;
        private readonly IReportFileWriter _fileWriter;
        private ILogger _logger;
        private ICustomSettings _customSettings;
        
        public ReportGenerator(IGenericRepository<DriveReport> reportRepo, IReportFileWriter fileWriter, ILogger logger, ICustomSettings customSettings)
        {
            _reportRepo = reportRepo;
            _fileWriter = fileWriter;
            _logger = logger;
            _customSettings = customSettings;
        }

        public void WriteRecordsToFileAndAlterReportStatus()
        {
            var usersToReimburse = GetUsersAndReportsToReimburse();
            var records = RecordListBuilder(usersToReimburse);

            bool success;
            try
            {
                success = _fileWriter.WriteRecordsToFile(records);
            }
            catch (Exception e)
            {
                _logger.Error($"{GetType().Name}, WriteRecordsToFileAndAlterReportStatus(), Error when writing records to IND01-file", e);
                throw;
            }

            if (!success)
            {
                //There was an error writing the file so the reports should not be marked as invoiced
                _logger.Error($"{GetType().Name}, WriteRecordsToFileAndAlterReportStatus(), Error when writing records to IND01-file, reports status not updated to processed.");
                return;
            }

            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);

            foreach (var reports in usersToReimburse.Values)
            {
                foreach (var report in reports)
                {
                    report.Status = ReportStatus.Invoiced;
                    var deltaTime = DateTime.Now.ToUniversalTime() - epoch;
                    report.ProcessedDateTimestamp = (long)deltaTime.TotalSeconds;
                }
            }
            _reportRepo.Save();
        }

        private Dictionary<string, List<DriveReport>> GetUsersAndReportsToReimburse()
        {
            var reportsToReimburse = GetDriveReportsToReimburse();
            var reportsPerUser = new Dictionary<string, List<DriveReport>>();

            foreach (var report in reportsToReimburse)
            {
                var cpr = report.Person.CprNumber;
                if (!reportsPerUser.ContainsKey(cpr))
                {
                    reportsPerUser.Add(cpr, new List<DriveReport>());
                }
                reportsPerUser[cpr].Add(report);                
            }

            return reportsPerUser;
        }

        private IEnumerable<DriveReport> GetDriveReportsToReimburse()
        {
            return _reportRepo.AsQueryable().Where(r => r.Status == ReportStatus.Accepted).ToList();
        }

        private List<FileRecord> RecordListBuilder(Dictionary<string, List<DriveReport>> usersToReimburse)
        {
            var fileRecords = new List<FileRecord>();
            foreach (var cpr in usersToReimburse.Keys)
            {
                var userDriveReports = usersToReimburse[cpr];

                // If a Drive report has an TFCodeOptional, we want to make two records. 
                // One using the real TFCode and one using the TFCodeOptional
                var driveReportsWithOptionalTFCode = userDriveReports.Where(r => r.TFCodeOptional != null);
                userDriveReports.AddRange(driveReportsWithOptionalTFCode.Select(r =>
                {
                    var reportUsingOptionalTFCode = new DriveReport
                    {
                        TFCode = r.TFCodeOptional,
                        AccountNumber = r.AccountNumber,
                        Employment = r.Employment,
                        EmploymentId = r.EmploymentId,
                        Distance = r.Distance,
                        DriveDateTimestamp = r.DriveDateTimestamp
                    };
                    return reportUsingOptionalTFCode;
                }).ToList());

                var driveReports = userDriveReports.OrderBy(x => x.EmploymentId).OrderBy(x => x.TFCode).ThenBy(x => x.AccountNumber).ThenBy(x => x.DriveDateTimestamp);
                DriveReport currentDriveReport = null;
                var currentTfCode = "";
                var currentMonth = -1;
                String currentAccountNumber = null;
                foreach (var driveReport in driveReports)
                {
                    var driveDate = TimestampToDate(driveReport.DriveDateTimestamp);
                    if (!driveReport.TFCode.Equals(currentTfCode) //We make one file record for each employment and each tf code
                            || driveDate.Month != currentMonth
                            || currentDriveReport == null
                            || !driveReport.EmploymentId.Equals(currentDriveReport.EmploymentId)
                            || !(driveReport.AccountNumber == currentAccountNumber))
                    {
                        if (currentDriveReport != null)
                        {
                            fileRecords.Add(new FileRecord(currentDriveReport, cpr, _customSettings));
                        }
                        currentMonth = driveDate.Month;
                        currentTfCode = driveReport.TFCode;
                        currentAccountNumber = driveReport.AccountNumber;
                        currentDriveReport = new DriveReport
                        {
                            TFCode = driveReport.TFCode,
                            AccountNumber = currentAccountNumber ?? String.Empty,
                            Employment = driveReport.Employment,
                            EmploymentId = driveReport.EmploymentId,
                            Distance = 0,
                            DriveDateTimestamp = TimetsmpOfLastDayInMonth(driveDate)
                        };

                    }
                    currentDriveReport.Distance += driveReport.Distance;
                }
                if (currentDriveReport != null)
                {
                    fileRecords.Add(new FileRecord(currentDriveReport, cpr, _customSettings));
                }
            }

            return fileRecords;
        }

        private DateTime TimestampToDate(long timestamp)
        {
            var dtDateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
            dtDateTime = dtDateTime.AddSeconds(timestamp).ToLocalTime();
            return dtDateTime;
        }

        private long TimetsmpOfLastDayInMonth(DateTime date)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

            var lastDay = new DateTime(date.Year, date.Month, DateTime.DaysInMonth(date.Year, date.Month));
            return (long)(lastDay - epoch).TotalSeconds;
        }
    }
}
