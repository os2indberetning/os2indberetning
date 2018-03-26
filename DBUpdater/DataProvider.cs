﻿using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Data.SqlClient;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using DBUpdater.Models;
using Core.ApplicationServices;
using Ninject;
using Core.ApplicationServices.Logger;
using MySql.Data.MySqlClient;
using System.IO;
using Core.DomainServices.Interfaces;

namespace DBUpdater
{
    public class DataProvider : IDbUpdaterDataProvider
    {
        private readonly string _connectionString = ConfigurationManager.ConnectionStrings["DBUpdaterConnection"].ConnectionString;

        private ILogger _logger = NinjectWebKernel.GetKernel().Get<ILogger>();
        private ICustomSettings _customSettings;

        public DataProvider(ILogger logger, ICustomSettings customSettings)
        {
            _logger = logger;
            _customSettings = customSettings;
        }

        /// <summary>
        /// Reads employees from Kommune database and returns them asqueryable.
        /// </summary>
        /// <returns></returns>
        public IQueryable<Employee> GetEmployeesAsQueryable()
        {
            _logger.Debug($"{GetType().Name}, GetEmployeesAsQueryable(), Employee import from views started");

            var result = new List<Employee>();

            using (var sqlConnection1 = new SqlConnection(_connectionString))
            {
                string medarbejderView = _customSettings.DbViewMedarbejder;

                if(medarbejderView == null)
                {
                    _logger.Error($"{this.GetType().Name}, GetEmployeesAsQueryable(), DATABASE_VIEW_MEDARBEJDER is null");
                }

                var cmd = new SqlCommand
                {
                    CommandText = "SELECT * FROM " + medarbejderView,
                    CommandType = CommandType.Text,
                    Connection = sqlConnection1
                };

                string manr = "0";
                try
                {
                    sqlConnection1.Open();
                    var reader = cmd.ExecuteReader();

                    while (reader.Read())
                    {
                        manr = SafeGetInt32(reader, 0).ToString();
                        var currentRow = new Employee
                        {
                            //Fix to see the actual row in log when an exception happens. Look in exception catch.
                            //MaNr = SafeGetInt32(reader, 0),
                            MaNr = manr,
                            AnsaettelsesDato = SafeGetDate(reader, 1),
                            OphoersDato = SafeGetDate(reader, 2),
                            Fornavn = SafeGetString(reader, 3),
                            Efternavn = SafeGetString(reader, 4),
                            ADBrugerNavn = SafeGetString(reader, 5),
                            Adresse = SafeGetString(reader, 6),
                            Stednavn = SafeGetString(reader, 7),
                            PostNr = SafeGetString(reader, 8) == null ? 0 : int.Parse(SafeGetString(reader, 8)),
                            By = SafeGetString(reader, 9),
                            Land = SafeGetString(reader, 10),
                            Email = SafeGetString(reader, 11),
                            CPR = SafeGetString(reader, 12),
                            LOSOrgId = SafeGetInt32(reader, 13),
                            Leder = reader.GetBoolean(14),
                            Stillingsbetegnelse = SafeGetString(reader, 15),
                            Omkostningssted = SafeGetInt64(reader, 16),
                            AnsatForhold = SafeGetString(reader, 17),
                            EkstraCiffer = SafeGetInt16(reader, 18)
                        };
                        result.Add(currentRow);
                    }
                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().Name}, GetEmployeesAsQueryable(), Error when importing employees, DATABASE_VIEW_MEDARBEJDER={medarbejderView}, MaNr={manr}", e);
                    throw;
                }
            }
            return result.AsQueryable();
        }

        /// <summary>
        /// Read Organisations from Kommune database and returns them asQueryably.
        /// </summary>
        /// <returns></returns>
        public IQueryable<Organisation> GetOrganisationsAsQueryable()
        {
            string organisationView = _customSettings.DbViewOrganisation;

            if (organisationView == null)
            {
                _logger.Error($"{this.GetType().Name}, GetOrganisationsAsQueryable(): DATABASE_VIEW_ORGANISATION is null");
            }

            var result = new List<Organisation>();
            using (var sqlConnection = new SqlConnection(_connectionString))
            {
                var cmd = new SqlCommand
                {
                    CommandText = "SELECT * FROM " + organisationView,
                    CommandType = CommandType.Text,
                    Connection = sqlConnection
                };

                try
                {
                    sqlConnection.Open();
                    var reader = cmd.ExecuteReader();
                    while (reader.Read())
                    {
                        var currentRow = new Organisation
                        {
                            LOSOrgId = reader.GetInt32(0),
                            ParentLosOrgId = SafeGetInt32(reader, 1),
                            KortNavn = SafeGetString(reader, 2),
                            Navn = SafeGetString(reader, 3),
                            Gade = SafeGetString(reader, 4),
                            Stednavn = SafeGetString(reader, 5),
                            Postnr = SafeGetInt16(reader, 6),
                            By = SafeGetString(reader, 7),
                            Omkostningssted = SafeGetInt64(reader, 8),
                            Level = reader.GetInt32(9)
                        };
                        result.Add(currentRow);
                    }
                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().Name}, GetOrganisationsAsQueryable(), Error when importing orgunits, DATABASE_VIEW_ORGANISATION={organisationView}", e);
                    throw;
                }
            }
            return result.AsQueryable();
        }

        private DateTime? SafeGetDate(IDataReader reader, int colIndex)
        {
            if (!reader.IsDBNull(colIndex))
            {
                return reader.GetDateTime(colIndex);
            }
            return null;
        }

        private string SafeGetString(IDataReader reader, int colIndex)
        {
            if (!reader.IsDBNull(colIndex))
            {
                return reader.GetString(colIndex);
            }
            return null;
        }

        private int? SafeGetInt16(IDataReader reader, int colIndex)
        {
            if (!reader.IsDBNull(colIndex))
            {
                // This if statement was added because Syddjurs changed their datatype on a row from smallint to tinyint, while Favrskov did not.
                // A tinyint is a byte, which is handled by the first check.
                // A smallint will be handled by the else statement.
                if (reader.GetFieldType(colIndex) == typeof(byte))
                {
                    var b = reader.GetByte(colIndex);
                    return Convert.ToInt32(b);
                }
                else
                {
                    return reader.GetInt16(colIndex);
                }
            }
            return null;
        }

        public IQueryable<IDMOrganisation> GetOrganisationsAsQueryableIDM()
        {
            string organisationView = _customSettings.DbViewOrganisation;

            if (organisationView == null)
            {
                _logger.Error($"{this.GetType().Name}, GetOrganisationsAsQueryable(): DATABASE_VIEW_ORGANISATION is null");
            }

            var result = new List<IDMOrganisation>();
            using (var sqlConnection1 = new MySqlConnection(_connectionString))
            {
                var cmd = new MySqlCommand()
                {
                    CommandText = $"SELECT * FROM {organisationView}",
                    CommandType = CommandType.Text,
                    Connection = sqlConnection1
                };
                try
                {
                    sqlConnection1.Open();
                    var reader = cmd.ExecuteReader();

                    while (reader.Read())
                    {
                        var currentRow = new IDMOrganisation()
                        {
                            OUID = SafeGetString(reader, 0),
                            Navn = SafeGetString(reader, 1),
                            OverliggendeOUID = SafeGetString(reader, 2),
                            OverliggendeOrg = SafeGetString(reader, 3),
                            Leder = SafeGetString(reader, 4),
                            Vejnavn = SafeGetString(reader, 5),
                            PostNr = SafeGetString(reader, 6),
                            PostDistrikt = SafeGetString(reader, 7)
                        };
                        result.Add(currentRow);
                    }
                }
                catch (EndOfStreamException eose)
                {
                    _logger.Error($"{this.GetType().Name}, GetOrganisationsAsQueryableIDM(): Connection timed out", eose);
                    throw;
                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().Name}, GetOrganisationsAsQueryableIDM(): Error when reading organisations from IDM view", e);
                    throw;
                }
            }
            return result.AsQueryable();
        }

        public IQueryable<IDMEmployee> GetEmployeesAsQueryableIDM()
        {
            string medarbejderView = _customSettings.DbViewMedarbejder;

            if (medarbejderView == null)
            {
                _logger.Error($"{this.GetType().Name}, GetEmployeesAsQueryable(): DATABASE_VIEW_MEDARBEJDER is null");
            }

            var result = new List<IDMEmployee>();

            using (var sqlConnection1 = new MySqlConnection(_connectionString))
            {
                var cmd = new MySqlCommand()
                {
                    CommandText = $"SELECT * FROM {medarbejderView}",
                    CommandType = CommandType.Text,
                    Connection = sqlConnection1
                };

                try
                {
                    sqlConnection1.Open();

                    var reader = cmd.ExecuteReader();

                    while (reader.Read())
                    {
                        var temp1 = SafeGetString(reader, 3);
                        var temp2 = SafeGetString(reader, 4);

                        var currentRow = new IDMEmployee
                        {
                            Institutionskode = SafeGetString(reader, 0),
                            Tjenestenummer = SafeGetString(reader, 1),
                            CPRNummer = SafeGetString(reader, 2),
                            AnsættelseFra = SafeGetString(reader, 3) == "" ? DateTime.Now : Convert.ToDateTime(SafeGetString(reader, 3)),
                            AnsættelseTil = SafeGetString(reader, 4) == "" ? DateTime.Parse("31-12-9999") : Convert.ToDateTime(SafeGetString(reader, 4)),
                            Fornavn = SafeGetString(reader, 5),
                            Efternavn = SafeGetString(reader, 6),
                            APOSUID = SafeGetString(reader, 7),
                            BrugerID = SafeGetString(reader, 8),
                            OrgEnhed = SafeGetString(reader, 9),
                            OrgEnhedOUID = SafeGetString(reader, 10),
                            Email = SafeGetString(reader, 11),
                            Stillingsbetegnelse = SafeGetString(reader, 12),
                            Vejnavn = SafeGetString(reader, 13),
                            PostNr = SafeGetString(reader, 15),
                            PostDistrikt = SafeGetString(reader, 16)
                        };
                        result.Add(currentRow);
                    }
                }
                catch (Exception e)
                {
                    _logger.Error($"{this.GetType().Name}, GetOrganisationsAsQueryableIDM(): Error when reading employees from IDM view", e);
                    throw;
                }
            }
            return result.AsQueryable();
        }

        private int? SafeGetInt32(IDataReader reader, int colIndex)
        {
            if (!reader.IsDBNull(colIndex))
            {
                return reader.GetInt32(colIndex);
            }
            return null;
        }

        private long? SafeGetInt64(IDataReader reader, int colIndex)
        {
            if (!reader.IsDBNull(colIndex))
            {
                return reader.GetInt64(colIndex);
            }
            return null;
        }

    }
}
