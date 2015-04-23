﻿using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Entity.Migrations.Model;
using System.Data.SqlClient;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using DBUpdater.Models;
using Infrastructure.DataAccess;

namespace DBUpdater
{
    static class Program
    {
        static void Main(string[] args)
        {

            using (SqlConnection sqlConnection1 = new SqlConnection("data source=706sofd01.intern.syddjurs.dk;initial catalog=MDM;persist security info=True;user id=sofdeindberetning;password=soa2ieCh>e"))
            {
                SqlCommand cmd = new SqlCommand();
                SqlDataReader reader;

                cmd.CommandText = "SELECT * FROM eindberetning.medarbejder";
                cmd.CommandType = CommandType.Text;
                cmd.Connection = sqlConnection1;

                sqlConnection1.Open();

                reader = cmd.ExecuteReader();

                while (reader.Read())
                {
                    var currentRow = new Employee();
                    currentRow.MaNr = reader.GetInt32(0);
                    currentRow.AnsaettelsesDato = reader.SafeGetDate(1);
                    currentRow.OphoersDato = reader.SafeGetDate(2);
                    currentRow.Fornavn = reader.SafeGetString(3);
                    currentRow.Efternavn = reader.SafeGetString(4);
                    currentRow.ADBrugerNavn = reader.SafeGetString(5);
                    currentRow.Adresse = reader.SafeGetString(6);
                    currentRow.Stednavn = reader.SafeGetString(7);
                    currentRow.PostNr = int.Parse(reader.SafeGetString(8));
                    currentRow.By = reader.SafeGetString(9);
                    currentRow.Land = reader.SafeGetString(10);
                    currentRow.Email = reader.SafeGetString(11);
                    currentRow.CPR = reader.SafeGetString(12);
                    currentRow.LOSOrgId = reader.GetInt32(13);
                    currentRow.Leder = reader.GetBoolean(14);
                    currentRow.Stillingsbetegnelse = reader.SafeGetString(15);
                    currentRow.Omkostningssted = reader.GetInt32(16);
                    currentRow.AnsatForhold = reader.SafeGetString(17);
                    currentRow.EkstraCiffer = reader.GetInt32(18);
                }
            }
        }

        public static string SafeGetString(this SqlDataReader reader, int colIndex)
        {
            if (!reader.IsDBNull(colIndex))
            {
                return reader.GetString(colIndex);
            }
            return string.Empty;
        }

        public static DateTime SafeGetDate(this SqlDataReader reader, int colIndex)
        {
            if (!reader.IsDBNull(colIndex))
            {
                return reader.GetDateTime(colIndex);
            }
            return new DateTime();
        }

    }
}