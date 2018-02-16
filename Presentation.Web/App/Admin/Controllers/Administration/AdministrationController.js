﻿angular.module("application").controller("AdministrationController", [
   "$scope", "$q", "HelpText", "Person", "$modal", "NotificationService", "sendDataToSd", "File", "Autocomplete",
   function ($scope, $q, HelpText, Person, $modal, NotificationService, sendDataToSd, File, Autocomplete) {

       HelpText.getAll().$promise.then(function (res) {
           $scope.isSD = res.UseSD;
           $scope.isKmd = res.UseKMD;
       });

       $scope.autoCompleteOptions = {
           filter: "contains"
       };

       $scope.nonAdmins = Autocomplete.nonAdmins();

       // Called from AdminMenuController
       // Prevents loading data before it is needed.
       $scope.$on('administrationClicked', function (event, mass) {
           $scope.gridContainer.grid.dataSource.read();
       });
       $scope.gridContainer = {};
       $scope.person = {};

       /// <summary>
       /// Loads existing admins from backend.
       /// </summary>
       $scope.admins = {
           autoBind: false,
           dataSource: {
               type: "odata",
               transport: {
                   read: {
                       beforeSend: function (req) {
                           req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                       },
                       url: "/odata/Person?$filter=IsAdmin",
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
                       return data.value;
                   },
                   total: function (data) {
                       return data['@odata.count']; // <-- The total items count is the data length, there is no .Count to unpack.
                   }
               },
               pageSize: 20,
               serverPaging: true,
               serverSorting: true,
           },
           sortable: true,
           pageable: {
               messages: {
                   display: "{0} - {1} af {2} administratorer", //{0} is the index of the first record on the page, {1} - index of the last record on the page, {2} is the total amount of records
                   empty: "Ingen administratorer at vise",
                   page: "Side",
                   of: "af {0}", //{0} is total amount of pages
                   itemsPerPage: "administratorer pr. side",
                   first: "Gå til første side",
                   previous: "Gå til forrige side",
                   next: "Gå til næste side",
                   last: "Gå til sidste side",
                   refresh: "Genopfrisk"
               },
               pageSizes: [5, 10, 20, 30, 40, 50, 100, 150, 200]
           },
           scrollable: false,
           columns: [
               {
                   field: "FullName",
                   title: "Medarbejder"
               }, {
                   field: "Mail",
                   title: "Email"
               }, {
                   title: "Muligheder",
                   template: function (data) {
                       return "<a ng-click='removeAdmin(" + data.Id + ",\"" + data.FullName + "\")'>Slet</a>";
                   }
               }, {
                   field: "AdminReceiveMail",
                   title: "Modtag emails",
                   template: function (data) {
                       if (data.AdminRecieveMail) {
                           return "<input type='checkbox' ng-click='adminRecieveMailChecked(" + data.Id + ", false)' checked></input>";
                       } else {
                           return "<input type='checkbox' ng-click='adminRecieveMailChecked(" + data.Id + ", true)'></input>";
                       }
                   }
               }
           ],
       };

       $scope.adminRecieveMailChecked = function (id, newValue) {
        /// <summary>
        /// Is called when the user checks an orgunit in the grid.
        /// Patches HasAccessToFourKmRule on the backend.
        /// </summary>
        /// <param name="id"></param>

            Person.patch({ id: id }, { "AdminRecieveMail": newValue }).$promise.then(function () {
                    if (newValue) {
                        NotificationService.AutoFadeNotification("success", "", "Admin modtager nu emails");
                    } else {
                        NotificationService.AutoFadeNotification("success", "", "Admin modtager ikke længere emails");
                    }

                    $scope.gridContainer.grid.dataSource.read();
            });
        }

       $scope.removeAdmin = function (Id, FullName) {
           /// <summary>
           /// Opens remove admin modal
           /// </summary>
           /// <param name="Id">Id of person</param>
           /// <param name="FullName">FullName of person</param>
           var modalInstance = $modal.open({
               templateUrl: 'App/Admin/HTML/Administration/Modal/RemoveAdminModalTemplate.html',
               controller: 'RemoveAdminModalController',
               backdrop: 'static',
               size: 'lg',
               resolve: {
                   Id: function () {
                       return Id;
                   },
                   FullName: function () {
                       return FullName;
                   }
               }
           });

           modalInstance.result.then(function (resPerson) {
               Person.patch({ id: resPerson.Id }, { "IsAdmin": false }, function () {
                   NotificationService.AutoFadeNotification("success", "", resPerson.FullName + " blev slettet som administrator.");
                   $scope.gridContainer.grid.dataSource.read();
               }, function () {
                   NotificationService.AutoFadeNotification("danger", "", resPerson.FullName + " blev ikke slettet som administrator.");
               });
           });
       }

       $scope.addAdminClicked = function () {
           /// <summary>
           /// Opens add admin modal
           /// </summary>
           if ($scope.person.chosenAdmin == undefined || $scope.person.chosenAdmin[0] == undefined) {
               return;
           }

           var modalInstance = $modal.open({
               templateUrl: 'App/Admin/HTML/Administration/Modal/AddAdminModalTemplate.html',
               controller: 'AddAdminModalController',
               backdrop: 'static',
               size: 'lg',
               resolve: {
                   chosenPerson: function () {
                       return $scope.person.chosenAdmin[0];
                   }
               }
           });

           modalInstance.result.then(function (person) {
               Person.patch({ id: person.Id }, { "IsAdmin": true }, function () {
                   NotificationService.AutoFadeNotification("success", "", person.FullName + " blev gjort til administrator.");
                   $scope.gridContainer.grid.dataSource.read();
                   $scope.person.chosenAdmin = "";
               }, function () {
                   NotificationService.AutoFadeNotification("danger", "", person.FullName + " blev ikke gjort til administrator.");
               });
           });
       }

       $scope.generateFileReportClicked = function () {
           /// <summary>
           /// Opens confirm generate file report modal
           /// </summary>
           var modalInstance = $modal.open({
               templateUrl: 'App/Admin/HTML/Administration/Modal/ConfirmGenerateFileModalTemplate.html',
               controller: 'GenerateFileModalController',
               backdrop: 'static',
               size: 'lg',
           });

           modalInstance.result.then(function (person) {
               File.generateFileReport(function () {
                   NotificationService.AutoFadeNotification("success", "", "Indberetninger blev overført til lønsystem");
               }, function () {
                   NotificationService.AutoFadeNotification("danger", "", "Indberetninger blev IKKE overført til lønsystem");
               });
           });
       }


       /*$scope.sendDataToSDClicked = function () {
           /// <summary>
           /// Opens confirm generate kmd file modal
           /// </summary>
           //alert(JSON.stringify(Configuration.getConfiguration({key: 'asdasda'})));

           var modalInstance = $modal.open({
               templateUrl: 'App/Admin/HTML/Administration/Modal/ConfirmDataSendSDModalTemplate.html',
               controller: 'SendDataToSdController',
               backdrop: 'static',
               size: 'lg',
           });

           modalInstance.result.then(function (person) {
               sendDataToSd.sendDataToSd(function () {
                   NotificationService.AutoFadeNotification("success", "", "Data blev sendt til SD.");
               }, function () {
                   NotificationService.AutoFadeNotification("danger", "", "Data blev ikke sendt!");
               });
           });
       }*/


   }
]);