﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Web.OData;
using Core.DomainModel;

namespace Core.ApplicationServices.Interfaces
{
    public interface IDriveReportService
    {
        DriveReport Create(DriveReport report);
        void SendMailForRejectedReport(int key, Delta<DriveReport> delta);

        void CalculateFourKmRuleForOtherReports(DriveReport report);

        Person GetResponsibleLeaderForReport(DriveReport driveReport);
        Person GetActualLeaderForReport(DriveReport driveReport);
        bool Validate(DriveReport report);

        void SendMailToUserAndApproverOfEditedReport(DriveReport report, string emailText, Person admin, string action);

        double GetNDKWorkRouteCalculation(int employmentId, DriveReportTransportType transportType, bool startsHome, bool endsHome, Address[] adresses);
    }
}
