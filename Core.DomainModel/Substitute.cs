using System;
using System.Collections.Generic;

namespace Core.DomainModel
{
    public class Substitute
    {
        public int Id { get; set; }
        public long StartDateTimestamp { get; set; }
        public long EndDateTimestamp { get; set; }

        /// <summary>
        /// If this substitute object is modelling a substitute scenario, LeaderId will be the same as PersonId.
        /// If this substitute object is modelling a personal approver scenario, LeaderId will be the Id of the Substitutes Leader.
        /// </summary>
        public int LeaderId { get; set; }
        public virtual Person Leader { get; set; }
        /// <summary>
        /// The person who is substitute for someone else, and whos going to be approving reports he/she would otherwise not approve.
        /// </summary>
        public int SubId { get; set; }
        public virtual Person Sub { get; set; }
        /// <summary>
        /// The person that the substitute is replacing (= being a substitute for).
        /// </summary>
        public int PersonId { get; set; }
        public virtual Person Person { get; set; }
        public int OrgUnitId { get; set; }
        public virtual OrgUnit OrgUnit { get; set; }
        public int? CreatedById { get; set; }
        public virtual Person CreatedBy { get; set; }
        public bool TakesOverOriginalLeaderReports { get; set; }
    }
}
