
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Core.DomainModel
{
    public class FileGenerationSchedule
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int Id { get; set; }
        public long DateTimestamp { get; set; }
        public bool Repeat { get; set; }
        public bool Completed { get; set; }
        public virtual ICollection<MailNotificationSchedule> MailNotificationSchedules { get; set; }
    }
}
