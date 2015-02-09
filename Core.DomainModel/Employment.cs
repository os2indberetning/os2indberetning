﻿using System;
using System.Collections.Generic;

namespace Core.DomainModel
{
    public class Employment
    {
        public int Id { get; set; }
        public int EmploymentId { get; set; }
        public string Position { get; set; }
        public bool IsLeader { get; set; }
        public long StartDateTimestamp { get; set; }
        public long EndDateTimestamp { get; set; }

        public virtual Person Person { get; set; }
        public virtual ICollection<Report> Reports { get; set; }
        public virtual OrgUnit OrgUnit { get; set; }
    }
}