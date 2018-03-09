
using System;

namespace Core.DomainModel
{
    public class MailNotificationSchedule
    {
        public int Id { get; set; }
        public long DateTimestamp { get; set; }
        public int FileGenerationScheduleId{ get; set; }
        public virtual FileGenerationSchedule FileGenerationSchedule { get; set; }
        public string CustomText { get; set; }
    }
}
