
using System;
using System.Collections.Generic;
using System.Linq;

namespace Core.DomainModel
{
    public enum ReportStatus
    {
        Pending,
        Accepted,
        Rejected,
        Invoiced
    }

    public class Report
    {
        public int Id { get; set; }
        public ReportStatus Status { get; set; }
        public long CreatedDateTimestamp { get; set; }
        public long EditedDateTimestamp { get; set; }
        public String Comment { get; set; }
        public long ClosedDateTimestamp { get; set; }
        public long ProcessedDateTimestamp { get; set; }
        public virtual Person ApprovedBy { get; set; }
        public int? ApprovedById { get; set; }

        public int PersonId { get; set; }
        public virtual Person Person { get; set; }
        public int EmploymentId { get; set; }
        public virtual Employment Employment { get; set; }

        public virtual ICollection<Person> ResponsibleLeaders
        {
            get;
            set;
        }
        public int? ActualLeaderId { get; set; }
        public virtual Person ActualLeader { get; set; }

        public bool IsPersonResponsible(Person person)
        {
            return ResponsibleLeaders.Contains(person);
        }

        public bool IsPersonResponsible(int personId)
        {
            return ResponsibleLeaders.Select(p => p.Id).Contains(personId);
        }

        public void UpdateResponsibleLeaders(ICollection<Person> newlist)
        {
            foreach (var person in ResponsibleLeaders.ToList())
            {
                if (!newlist.Any(p => p.Id == person.Id))
                {
                    ResponsibleLeaders.Remove(person);
                }
            }

            foreach (var person in newlist)
            {
                if (!ResponsibleLeaders.Any(p => p.Id == person.Id))
                {
                    ResponsibleLeaders.Add(person);
                }
            }
        }
    }
}
