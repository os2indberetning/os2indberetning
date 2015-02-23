﻿using System;
using System.Collections.Generic;
using System.Linq;
using Core.DomainModel;
using Core.DomainServices;
using Infrastructure.AddressServices;
using Infrastructure.AddressServices.Interfaces;
using Infrastructure.AddressServices.Routing;
using Infrastructure.DataAccess;


namespace Core.ApplicationServices
{
    public class DriveReportService
    {
        private readonly IRoute _route;
        private readonly IAddressCoordinates _coordinates;
        private readonly IGenericRepository<DriveReportPoint> _driveReportPointRepository;
        private readonly IGenericRepository<DriveReport> _driveReportRepository;
        private readonly IGenericRepository<Person> _personRepository;
        private readonly ReimbursementCalculator _calculator;

        public DriveReportService()
        {
            _route = new BestRoute();
            _coordinates = new AddressCoordinates();
            _driveReportPointRepository = new GenericRepository<DriveReportPoint>(new DataContext());
            _driveReportRepository = new GenericRepository<DriveReport>(new DataContext());
            _personRepository = new GenericRepository<Person>(new DataContext());
            _calculator = new ReimbursementCalculator();
        }

        public DriveReportService(IRoute route, IAddressCoordinates coordinates, IGenericRepository<DriveReportPoint> driveReportPointRepository, IGenericRepository<DriveReport> driveReportRepository, IGenericRepository<Person> personRepository, ReimbursementCalculator calculator)
        {
            _route = route;
            _coordinates = coordinates;
            _driveReportPointRepository = driveReportPointRepository;
            _driveReportRepository = driveReportRepository;
            _personRepository = personRepository;
            _calculator = calculator;
        }

        public IQueryable<DriveReport> AddFullName(IQueryable<DriveReport> repo)
        {
            var set = repo.ToList();

            // Add fullname to the resultset
            foreach (var driveReport in set)
            {
                AddFullName(driveReport);
            }
            return set.AsQueryable();
        }

        public void AddFullName(DriveReport driveReport)
        {
            if (driveReport == null)
            {
                return;
            }
            driveReport.Fullname = driveReport.Person.FirstName;

            if (!string.IsNullOrEmpty(driveReport.Person.MiddleName))
            {
                driveReport.Fullname += " " + driveReport.Person.MiddleName;
            }

            driveReport.Fullname += " " + driveReport.Person.LastName;
        }

        public DriveReport Create(DriveReport report)
        {
            var pointsWithCoordinates = report.DriveReportPoints.Select((t, i) => report.DriveReportPoints.ElementAt(i)).Select(currentPoint => (DriveReportPoint) _coordinates.GetAddressCoordinates(currentPoint)).ToList();

            report.DriveReportPoints = pointsWithCoordinates;

            var drivenRoute = _route.GetRoute(report.DriveReportPoints);

            report.Distance = (double)drivenRoute.Length / 1000;


            if (report.PersonId == 0)
            {
                throw new Exception("No person provided");
            }


            report = _calculator.Calculate(report);

            var createdReport = _driveReportRepository.Insert(report);
            _driveReportRepository.Save();


            for (var i = 0; i < createdReport.DriveReportPoints.Count; i++)
            {
                var currentPoint = createdReport.DriveReportPoints.ElementAt(i);

                if (i == report.DriveReportPoints.Count - 1)
                {
                    // last element   
                    currentPoint.PreviousPointId = createdReport.DriveReportPoints.ElementAt(i - 1).Id;
                }
                else if (i == 0)
                {
                    // first element
                    currentPoint.NextPointId = createdReport.DriveReportPoints.ElementAt(i + 1).Id;
                }
                else
                {
                    // between first and last
                    currentPoint.NextPointId = createdReport.DriveReportPoints.ElementAt(i + 1).Id;
                    currentPoint.PreviousPointId = createdReport.DriveReportPoints.ElementAt(i - 1).Id;

                }

            }

            _driveReportRepository.Save();

            return report;
        }
    }
}
