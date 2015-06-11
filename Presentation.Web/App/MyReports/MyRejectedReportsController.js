
angular.module("application").controller("MyRejectedReportsController", [
   "$scope", "$modal", "$rootScope", "Report", "$timeout", function ($scope, $modal, $rootScope, Report, $timeout) {

       // Set personId. The value on $rootScope is set in resolve in application.js
       var personId = $rootScope.CurrentUser.Id;

       $scope.tableSortHelp = $rootScope.HelpTexts.TableSortHelp.text;

       $scope.getEndOfDayStamp = function (d) {
           var m = moment(d);
           return m.endOf('day').unix();
       }

       $scope.getStartOfDayStamp = function (d) {
           var m = moment(d);
           return m.startOf('day').unix();
       }

       // dates for kendo filter.
       var fromDateFilter = new Date();
       fromDateFilter.setDate(fromDateFilter.getDate() - 30);
       fromDateFilter = $scope.getStartOfDayStamp(fromDateFilter);
       var toDateFilter = $scope.getEndOfDayStamp(new Date());

       $scope.loadReports = function () {
           /// <summary>
           /// Loads current users rejected reports from backend to kendo grid datasource.
           /// </summary>
           $scope.Reports = {
               dataSource: {
                   type: "odata",
                   transport: {
                       read: {
                           beforeSend: function (req) {
                               req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                           },



                           url: "/odata/DriveReports?status=Rejected &$expand=DriveReportPoints,ApprovedBy($select=FullName)",
                           dataType: "json",
                           cache: false
                       },
                       parameterMap: function (options, type) {
                           var d = kendo.data.transports.odata.parameterMap(options);

                           delete d.$inlinecount; // <-- remove inlinecount parameter                                                        

                           d.$count = true;

                           return d;
                       }
                   },
                   schema: {
                       data: function (data) {
                           return data.value; // <-- The result is just the data, it doesn't need to be unpacked.
                       },
                       total: function (data) {
                           return data['@odata.count']; // <-- The total items count is the data length, there is no .Count to unpack.
                       }
                   },
                   pageSize: 20,
                   serverPaging: false,
                   serverSorting: true,
                   filter: [{ field: "PersonId", operator: "eq", value: personId }, { field: "DriveDateTimestamp", operator: "gte", value: fromDateFilter }, { field: "DriveDateTimestamp", operator: "lte", value: toDateFilter }],
                   sort: { field: "DriveDateTimestamp", dir: "desc" },
                   aggregate: [
                   { field: "Distance", aggregate: "sum" },
                   { field: "AmountToReimburse", aggregate: "sum" },
                   ]
               },
               sortable: true,
               pageable: {
                   messages: {
                       display: "{0} - {1} af {2} indberetninger", //{0} is the index of the first record on the page, {1} - index of the last record on the page, {2} is the total amount of records
                       empty: "Ingen indberetninger at vise",
                       page: "Side",
                       of: "af {0}", //{0} is total amount of pages
                       itemsPerPage: "indberetninger pr. side",
                       first: "Gå til første side",
                       previous: "Gå til forrige side",
                       next: "Gå til næste side",
                       last: "Gå til sidste side",
                       refresh: "Genopfrisk"
                   },
                   pageSizes: [5, 10, 20, 30, 40, 50]
               },
               dataBound: function () {
                   this.expandRow(this.tbody.find("tr.k-master-row").first());
               },
               columns: [
                  {
                      field: "DriveDateTimestamp",
                      template: function (data) {
                          var m = moment.unix(data.DriveDateTimestamp);
                          return m._d.getDate() + "/" +
                              (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                              m._d.getFullYear();
                      },
                      title: "Kørselsdato"
                  }, {
                      field: "Purpose",
                      title: "Formål"
                  }, {
                      title: "Rute",
                      field: "DriveReportPoints",
                      template: function (data) {
                          var tooltipContent = "";
                          var gridContent = "";
                          angular.forEach(data.DriveReportPoints, function (point, key) {
                              if (key != data.DriveReportPoints.length - 1) {
                                  tooltipContent += point.StreetName + " " + point.StreetNumber + ", " + point.ZipCode + " " + point.Town + "<br/>";
                                  gridContent += point.Town + "<br/>";
                              } else {
                                  tooltipContent += point.StreetName + " " + point.StreetNumber + ", " + point.ZipCode + " " + point.Town;
                                  gridContent += point.Town;
                              }
                          });
                          var result = "<div kendo-tooltip k-content=\"'" + tooltipContent + "'\">" + gridContent + "</div> <a ng-click='showRouteModal(" + data.Id + ")'>Se rute på kort</a>";

                          if (data.KilometerAllowance != "Read") {
                              return result;
                          } else {
                              if (data.IsFromApp) {
                                  return "<div kendo-tooltip k-content=\"'" + data.UserComment + "'\">Indberettet fra mobil app</div> <a ng-click='showRouteModal(" + data.Id + ")'>Se rute på kort</a>";
                              } else {
                                  return "<div kendo-tooltip k-content=\"'" + data.UserComment + "'\">Aflæst manuelt</div>";
                              }

                          }
                      }
                  }, {
                      field: "Distance",
                      title: "Afstand",
                      template: function (data) {
                          return data.Distance.toFixed(2).toString().replace('.', ',') + " Km.";
                      },
                      footerTemplate: "Total: #= kendo.toString(sum, '0.00').replace('.',',') # Km"
                  }, {
                      field: "AmountToReimburse",
                      title: "Beløb",
                      template: function (data) {
                          return data.AmountToReimburse.toFixed(2).toString().replace('.', ',') + " Dkk.";
                      },
                      footerTemplate: "Total: #= kendo.toString(sum, '0.00').replace('.',',') # Dkk"
                  }, {
                      field: "CreationDate",
                      template: function (data) {
                          var m = moment.unix(data.CreatedDateTimestamp);
                          return m._d.getDate() + "/" +
                                (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                 m._d.getFullYear();
                      },
                      title: "Indberetningsdato"
                  }, {
                      field: "ClosedDateTimestamp",
                      title: "Afvist dato",
                      template: function (data) {
                          var m = moment.unix(data.ClosedDateTimestamp);
                          var date = m._d.getDate() + "/" +
                                (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                 m._d.getFullYear();
                          return date;

                      },
                  }, {
                      field: "ApprovedBy.FullName",
                      title: "Afvist af",
                      template: function (data) {
                          return data.ApprovedBy.FullName + "<div kendo-tooltip k-content=\"'" + data.Comment + "'\"><i class='fa fa-comment-o'></i></div>";
                      }
                  }
               ],
               scrollable: false
           };
       }

       $scope.loadInitialDates = function () {
           /// <summary>
           /// Sets initial date filters.
           /// </summary>
           // Set initial values for kendo datepickers.

           initialLoad = 2;

           var from = new Date();
           from.setDate(from.getDate() - 30);

           $scope.dateContainer.toDate = new Date();
           $scope.dateContainer.fromDate = from;
       }



       // Event handlers


       var initialLoad = 2;
       $scope.dateChanged = function () {
           // $timeout is a bit of a hack, but it is needed to get the current input value because ng-change is called before ng-model updates.
           $timeout(function () {

               var from = $scope.getStartOfDayStamp($scope.dateContainer.fromDate);
               var to = $scope.getEndOfDayStamp($scope.dateContainer.toDate);

               // Initial load is also a bit of a hack.
               // dateChanged is called twice when the default values for the datepickers are set.
               // This leads to sorting the grid content on load, which is not what we want.
               // Therefore the sorting is not done the first 2 times the dates change - Which are the 2 times we set the default values.
               if (initialLoad <= 0) {

                   $scope.applyDateFilter(from, to);
               }
               initialLoad--;
           }, 0);
       }

       $scope.clearClicked = function () {
           /// <summary>
           /// Clears filters.
           /// </summary>
           $scope.gridContainer.grid.dataSource.filter([{ field: "PersonId", operator: "eq", value: personId }]);
           $scope.loadInitialDates();
       }

       $scope.applyDateFilter = function (fromDateStamp, toDateStamp) {
           /// <summary>
           /// Applies date filters.
           /// </summary>
           /// <param name="fromDateStamp"></param>
           /// <param name="toDateStamp"></param>
           var newFilters = [];
           newFilters.push({ field: "PersonId", operator: "eq", value: personId });
           newFilters.push({ field: "DriveDateTimestamp", operator: "gte", value: fromDateStamp });
           newFilters.push({ field: "DriveDateTimestamp", operator: "lte", value: toDateStamp });
           $scope.gridContainer.grid.dataSource.filter(newFilters);
       }

       $scope.refreshGrid = function () {
           /// <summary>
           /// Refreshes kendo grid datasource.
           /// </summary>
           $scope.gridContainer.grid.dataSource.read();
       }

       // Load up the grids.
       $scope.loadReports();




       // Contains references to kendo ui grids.
       $scope.gridContainer = {};
       $scope.dateContainer = {};

       $scope.loadInitialDates();

       // Format for datepickers.
       $scope.dateOptions = {
           format: "dd/MM/yyyy",
       };

       $scope.showRouteModal = function (routeId) {
           /// <summary>
           /// Opens show route modal.
           /// </summary>
           /// <param name="routeId"></param>
           var modalInstance = $modal.open({
               templateUrl: '/App/Admin/HTML/Reports/Modal/ShowRouteModalTemplate.html',
               controller: 'ShowRouteModalController',
               backdrop: "static",
               resolve: {
                   routeId: function () {
                       return routeId;
                   }
               }
           });
       }
   }
]);