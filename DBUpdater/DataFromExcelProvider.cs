using System;
using System.Collections.Generic;
using System.Configuration;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using DBUpdater.Models;
using LinqToExcel;

namespace DBUpdater
{
    class DataFromExcelProvider 
    {
        public IQueryable<EmployeeIDM> GetEmployeesAsQueryable()
        {
            var result = new List<EmployeeIDM>();

            var path = ConfigurationManager.AppSettings["PersonExcelPath"];
            if (!File.Exists(path))
                throw new FileNotFoundException();

            var queryFactory = new ExcelQueryFactory(path);
            var worksheetName = queryFactory.GetWorksheetNames();
            foreach (var row in queryFactory.Worksheet(worksheetName.First()))
            {
                var currentRow = new EmployeeIDM();

                currentRow.Institutionskode = row[0];
                currentRow.Tjenestenummer = row[1].Cast<int>();
                currentRow.AnsættelseFra = row[2].Cast<DateTime>();
                currentRow.AnsættelseTil = row[3].Cast<DateTime>();
                currentRow.Fornavn = row[4];
                currentRow.Efternavn = row[5];
                currentRow.APOSUID = row[6];
                currentRow.BrugerID = row[7];
                currentRow.OrgEnhed = row[8];
                currentRow.OrgEnhedOUID = row[9];
                currentRow.Email = row[10];
                currentRow.Stillingsbetegnelse = row[11];
                currentRow.CPRNummer = row[13];
                currentRow.Medarbejeradresse = row[14];
                
                result.Add(currentRow);
            }
            return result.AsQueryable();
        }

        public IQueryable<OrganisationIDM> GetOrganisationsAsQueryable()
        {
            var result = new List<OrganisationIDM>();

            var path = ConfigurationManager.AppSettings["OrganisationExcelPath"];
            if (!File.Exists(path))
                throw new FileNotFoundException();

            var queryFactory = new ExcelQueryFactory(path);
            var worksheetName = queryFactory.GetWorksheetNames();
            foreach (var row in queryFactory.Worksheet(worksheetName.First()))
            {
                //var address = row[5].Cast<string>().Split(',');
                //var street = address[0];
                //var zipCode = Convert.ToInt32(address[1].Split(new char[0], 2)[0]);
                //var city = address[1].Split(new char[0], 2)[1];
                var currentRow = new OrganisationIDM
                {
                    Adresse = row[5],
                    Leder = row[4],
                    Navn = row[1],
                    OUID = row[0],
                    OverliggendeOUID = row[2],
                    OverliggendeOrg = row[3]
                };
                result.Add(currentRow);
            }
            return result.AsQueryable();
        }
    }
}
