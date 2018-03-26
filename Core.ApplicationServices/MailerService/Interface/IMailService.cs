using System;
using System.Collections.Generic;

namespace Core.ApplicationServices.MailerService.Interface
{
    public interface IMailService
    {
        void SendMails(DateTime payRoleDateTime, string customText);
        IEnumerable<string> GetLeadersWithPendingReportsMails();
        void SendMailToAdmins(string subject, string text);
        void SendMail(string toAddress, string subject, string text);
    }
}
