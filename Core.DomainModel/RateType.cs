using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Core.DomainModel
{
    public class RateType
    {
        public int Id { get; set; }
        public string Description { get; set; }

        /// <summary>
        /// TF Code for KMD integration. When SD integration is used instead of KMD, this property is used for the "lønart" id, which is basically the same.
        /// </summary>
        public String TFCode { get; set; }
        public bool RequiresLicensePlate { get; set; }
        public bool IsBike { get; set; }
    }
}
