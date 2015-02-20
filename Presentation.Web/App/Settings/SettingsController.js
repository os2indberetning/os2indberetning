﻿angular.module("application").controller("SettingController", [
    "$scope", "$modal", "Person", "LicensePlate", "Personalroute", "Point", "RouteContainer", "$http", "NotificationService", function ($scope, $modal, Person, LicensePlate, Personalroute, Point, RouteContainer, $http, NotificationService) {
        $scope.isCollapsed = true;
        $scope.mailAdvice = '';
        $scope.licenseplates = [];
        $scope.tokens = [];
        $scope.newLicensePlate = "";
        $scope.newLicensePlateDescription = "";
        $scope.workDistanceOverride = 0;
        $scope.recieveMail = false;
        $scope.alternativeHomeAddress = "";
        $scope.alternativeWorkAddress = "";

        LicensePlate.get({ id: 1 }, function (data) {
            $scope.licenseplates = data.value;
        });

        $scope.saveNewLicensePlate = function () {
            var newPlate = new LicensePlate({
                Plate: $scope.newLicensePlate,
                Description: $scope.newLicensePlateDescription,
                PersonId: 1
            });

            newPlate.$save(function (data) {
                $scope.licenseplates.push(data.value[0]);
                $scope.licenseplates.sort(function (a, b) {
                    return a.Id > b.Id;
                });
                $scope.newLicensePlate = "";
                $scope.newLicensePlateDescription = "";

                NotificationService.AutoFadeNotification("success", "Success", "Ny nummerplade blev gemt");
            }, function () {
                NotificationService.AutoFadeNotification("danger", "Fejl", "Nummerplade blev ikke gemt");
            });
        };

        $scope.deleteLicensePlate = function (plate) {
            var objIndex = $scope.licenseplates.indexOf(plate);
            $scope.licenseplates.splice(objIndex, 1);

            LicensePlate.delete({ id: plate.Id }, function (data) {                
                NotificationService.AutoFadeNotification("success", "Success", "Nummerplade blev slettet");
            }), function () {
                $scope.licenseplates.push(plate);
                $scope.licenseplates.sort(function (a, b) {
                    return a.Id > b.Id;
                });
                NotificationService.AutoFadeNotification("danger", "Fejl", "Nummerplade blev ikke slettet");
            };
        }

        $scope.saveNewToken = function () {
            NotificationService.AutoFadeNotification("danger", "Fejl", "Jeg er ikke implementeret :(");
        }

        $scope.deleteToken = function () {
            NotificationService.AutoFadeNotification("danger", "Fejl", "Jeg er ikke implementeret :(");
        }

        $scope.invertRecieveMail = function () {
            $scope.recieveMail = !$scope.recieveMail;

            var newPerson = new Person({
                RecieveMail: $scope.recieveMail
            });

            newPerson.$patch({ id: 1 }, function () {
                NotificationService.AutoFadeNotification("success", "Success", "Valg om modtagelse af mails blev gemt");
            }), function () {
                $scope.recieveMail = !$scope.recieveMail;
                NotificationService.AutoFadeNotification("danger", "Fejl", "Valg om modtagelse af mails blev ikke gemt");
            };
        }

        $scope.saveAlternativeHomeAddress = function () {
            NotificationService.AutoFadeNotification("danger", "Fejl", "Jeg er ikke implementeret :(");
        }

        $scope.saveAlternativeWorkAddress = function () {
            NotificationService.AutoFadeNotification("danger", "Fejl", "Jeg er ikke implementeret :(");
        }

        // Contains references to kendo ui grids.
        $scope.gridContainer = {};

        $scope.setHomeWorkOverride = function () {
            var newPerson = new Person({
                WorkDistanceOverride: $scope.workDistanceOverride
            });

            newPerson.$patch({ id: 1 }, function (data) {
                NotificationService.AutoFadeNotification("success", "Success", "Afstand mellem hjemme- og arbejdsadresse blev gemt");
            }), function () {
                if ($scope.mailAdvice == 'No') {
                    $scope.mailAdvice = 'Yes';
                } else {
                    $scope.mailAdvice = 'No';
                }
                NotificationService.AutoFadeNotification("danger", "Fejl", "Afstand mellem hjemme- og arbejdsadresse blev ikke gemt");
            };
        };

        $scope.loadGrids = function (id) {
            $scope.personalRoutes = {
                dataSource: {
                    type: "odata",
                    transport: {
                        read: {
                            beforeSend: function (req) {
                                req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                            },
                            url: "odata/PersonalRoutes(" + id + ")?$expand=Points",
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
                    pageSize: 5,
                    serverPaging: true,
                    serverSorting: true
                },
                sortable: true,
                pageable: true,
                dataBound: function () {
                    this.expandRow(this.tbody.find("tr.k-master-row").first());
                },
                columns: [
                    {
                        field: "Description",
                        title: "Beskrivelse"
                    }, {
                        field: "Points",
                        template: function (data) {
                            var temp = [];

                            angular.forEach(data.Points, function (value, key) {
                                if (value.PreviousPointId == undefined) {
                                    this.push(value.StreetName + " " + value.StreetNumber + ", " + value.ZipCode + " " + value.Town);
                                }

                            }, temp);

                            return temp;
                        },
                        title: "Adresse 1"
                    }, {
                        field: "Id",
                        template: function (data) {
                            var temp = [];

                            angular.forEach(data.Points, function (value, key) {
                                if (value.NextPointId == undefined) {
                                    this.push(value.StreetName + " " + value.StreetNumber + ", " + value.ZipCode + " " + value.Town);
                                }

                            }, temp);

                            return temp;
                        },
                        title: "Adresse 2"
                    },
                    {
                        field: "Id",
                        title: "Muligheder",
                        template: "<a ng-controller='RouteEditModalController' ng-click='openRouteEditModal(${Id})'>Rediger</a>"
                    }
                ]
            };

            $scope.personalAddresses = {
                dataSource: {
                    type: "odata",
                    transport: {
                        read: {
                            beforeSend: function (req) {
                                req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                            },
                            url: "odata/PersonalAddresses(" + id + ")",
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
                    pageSize: 5,
                    serverPaging: true,
                    serverSorting: true
                },
                sortable: true,
                pageable: true,
                dataBound: function () {
                    this.expandRow(this.tbody.find("tr.k-master-row").first());
                },
                columns: [
                    {
                        field: "Description",
                        title: "Beskrivelse"
                    }, {
                        field: "Id",
                        template: function (data) {
                            return (data.StreetName + " " + data.StreetNumber + ", " + data.ZipCode + " " + data.Town);
                        },
                        title: "Indberettet den"
                    }, {
                        field: "Id",
                        title: "Muligheder",
                        template: "<a ng-controller='AddressEditModalController' ng-click='openAddressEditModal(${Id})'>Mine tokens</a>"
                    }
                ]
            };
        }

        $scope.GetPerson = Person.get({ id: 1 }, function (data) {
            $scope.currentPerson = data.value[0];
            $scope.workDistanceOverride = $scope.currentPerson.WorkDistanceOverride;
            $scope.recieveMail = data.value[0].RecieveMail;
            if ($scope.recieveMail == true) {
                $scope.mailAdvice = 'Yes';
            } else {
                $scope.mailAdvice = 'No';
            }
            NotificationService.AutoFadeNotification("success", "Success", "Person fundet");
        },
        function () {
            NotificationService.AutoFadeNotification("danger", "Fejl", "Person ikke fundet");
        });

        $scope.loadGrids(1);

        $scope.openTokenModal = function (size) {

            var modalInstance = $modal.open({
                scope: $scope,
                templateUrl: '/App/Settings/tokenModal.html',
                controller: 'TokenInstanceController',
                backdrop: 'static',
                size: size,
                resolve: {
                    items: function () {
                        return $scope.tokens;
                    },
                    personId: function() {
                        return $scope.currentPerson.Id;
                    } 
                }
            });

            modalInstance.result.then(function (tokens) {
                $scope.tokens = tokens;
            });
        };
    }
]);