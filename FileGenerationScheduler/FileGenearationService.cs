﻿using System;
using System.Linq;
using System.Threading;
using Core.ApplicationServices.Logger;
using Core.ApplicationServices.MailerService.Interface;
using Core.DomainModel;
using Core.DomainServices;
using Core.ApplicationServices.Interfaces;

namespace FileGenerationScheduler
{
    public class FileGenerationService
    {
        private IMailService _mailService;
        private ITransferToPayrollService _transferToPayrollService;
        private IGenericRepository<MailNotificationSchedule> _mailRepo;
        private IGenericRepository<FileGenerationSchedule> _fileRepo;
        private ILogger _logger;

        public FileGenerationService(IMailService mailService, ITransferToPayrollService transferToPayrollService, IGenericRepository<MailNotificationSchedule> mailRepo, IGenericRepository<FileGenerationSchedule> fileRepo, ILogger logger)
        {
            _mailService = mailService;
            _transferToPayrollService = transferToPayrollService;
            _mailRepo = mailRepo;
            _fileRepo = fileRepo;
            _logger = logger;
        }

        /// <summary>
        /// Checks if it is time to generate file to the payroll system.
        /// Reschedule the file generation and the mail notification if Repeat is checked
        /// </summary>
        public void RunFileGenerationService()
        {
            var startOfDay = Utilities.ToUnixTime(new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, 00, 00, 00));
            var endOfDay = Utilities.ToUnixTime(new DateTime(DateTime.Now.Year, DateTime.Now.Month, DateTime.Now.Day, 23, 59, 59));

            // Filter the repository with the files that need to be generated today
            var filesToGenerate = _fileRepo.AsQueryable().Where(r => r.DateTimestamp >= startOfDay && r.DateTimestamp <= endOfDay && !r.Completed).ToList();

            if (filesToGenerate.Any())
            {
                // Try to generate the file using the Payroll service system
                Console.WriteLine("Forsøger at generere files.");
                foreach (var file in filesToGenerate)
                {
                    // TransferToPayrollService call here
                    AttemptGenerateFile();

                    // Set file generation complete
                    file.Completed = true;
                    _fileRepo.Save();

                    // Check if Repeat is true and schedule all mail notification and file gen jobs for the next month
                    if (file.Repeat)
                    {
                        var newDate = Utilities.ToUnixTime(Utilities.FromUnixTime(file.DateTimestamp).AddMonths(1));
                        try
                        {
                            var newFile = _fileRepo.Insert(new FileGenerationSchedule
                            {
                                DateTimestamp = newDate,
                                Repeat = true,
                                Completed = false,
                            });
                            _fileRepo.Save();

                            // Check all mail notifications
                            if (file.MailNotificationSchedules != null && file.MailNotificationSchedules.Any())
                            {
                                foreach (var mail in file.MailNotificationSchedules)
                                {
                                    var newDateTime = Utilities.ToUnixTime(Utilities.FromUnixTime(mail.DateTimestamp).AddMonths(1));
                                    var newNotification = _mailRepo.Insert(new MailNotificationSchedule()
                                    {
                                        FileGenerationScheduleId = newFile.Id, // ?
                                        DateTimestamp = newDateTime,
                                        CustomText = mail.CustomText
                                    });
                                }
                                _mailRepo.Save();
                            }                            
                        }
                        catch (Exception e)
                        {
                            Console.WriteLine("Could not reschedule the file generation");
                            _logger.Debug($"{this.GetType().Name}, RunFileGenerationService(), File generation was not rescheduled");
                            _logger.Error($"{GetType().Name}, AttemptGenerateFiles(), File generation was not rescheduled", e);
                        }                        
                    }
                }
                _logger.Debug($"{this.GetType().Name}, RunFileGenerationService(), File generation finished");
            }
            else
            {
                _logger.Debug($"{this.GetType().Name}, RunFileGenerationService(): No files to generate for payroll service found for today");
                Console.WriteLine("Ingen filer fundet! Programmet lukker om 3 sekunder.");
                Console.WriteLine(Environment.CurrentDirectory);
                Thread.Sleep(3000);
            }
        }

        /// <summary>
        /// Generated and transfers the files to the payroll system.
        /// Sends notification about the event to the admins
        /// </summary>
        private void AttemptGenerateFile()
        {
            try
            {
                _transferToPayrollService.TransferReportsToPayroll();

                // Send mail to admins
                var mailSubject = "Filen fra OS2 Indberetning er genereret";
                var mailText = "Filen fra OS2 Indberetning med godkendte indberetning er genereret.";
                _mailService.SendMailToAdmins(mailSubject, mailText);
            }
            catch (Exception e)
            {
                var mailSubject = "Generering af filen fra OS2 Indberetning fejlede";
                var mailText = "Generering af filen fra OS2 Indberetning med godkendte indberetninger fejlede. Filen er ikke blevet genereret.";
                _mailService.SendMailToAdmins(mailSubject, mailText);

                //TODO: Handle issues when trying to send payroll
                Console.WriteLine("Kunne ikke send files til payroll");
                _logger.LogForAdmin("Fejl ved generering af IND01 fil til KMD med godkendte indberetninger. Filen er ikke genereret, og indberetninger dermed ikke overført.");
                _logger.Error($"{GetType().Name}, AttemptGenerateFiles(), Could not generet file for KMD", e);
            }
        }
    }
}
