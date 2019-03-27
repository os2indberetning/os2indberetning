﻿angular.module("application").controller("FileGenerationScheduleController", [
    "$scope", "$modal", "FileGenerationSchedule", "EmailNotification", "$rootScope", function ($scope, $modal, FileGenerationSchedule, EmailNotification, $rootScope) {


        $scope.$on('emailClicked', function (event, mass) {
            $scope.gridContainer.notificationGrid.dataSource.read();
        });

        $scope.EmailHelpText = $rootScope.HelpTexts.EmailHelpText.text;

        $scope.gridContainer = {};


        /// <summary>
        /// Loads existing FileGenerationSchedules from backend to kendo grid datasource.
        /// </summary>
        $scope.notifications = {
            autoBind : false,
            dataSource: {
                type: "odata",
                transport: {
                    read: {
                        beforeSend: function (req) {
                            req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                        },
                        url: "/odata/FileGenerationSchedule?$expand=MailNotificationSchedules",
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
                serverPaging: false,
                serverSorting: true
            },
            sortable: true,
            pageable: {
                messages: {
                    display: "{0} - {1} af {2} adviseringer", //{0} is the index of the first record on the page, {1} - index of the last record on the page, {2} is the total amount of records
                    empty: "Ingen adviseringer at vise",
                    page: "Side",
                    of: "af {0}", //{0} is total amount of pages
                    itemsPerPage: "adviseringer pr. side",
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
                    field: "DateTimestamp",
                    title: "Lønkørsel",
                    template: function (data) {
                        var m = moment.unix(data.DateTimestamp);
                        return m._d.getDate() + "/" +
                            (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                            m._d.getFullYear();
                    }
                }, {
                    field: "Repeat",
                    title: "Gentag månedligt",
                    template: function (data) {
                        if (data.Repeat) {
                            return "Ja";
                        }
                        return "Nej";
                    }
                }, {
                    //field: "Notified",
                    title: "Er kørt",
                    template: function (data) {
                        if (data.Completed) {
                            return "<i class='fa fa-check'></i>";
                        }
                        return "";
                    }
                },
                {
                    //field: "MailNotificationSchedules",
                    title: "Emailadvis sendt",
                    template: function (data) {
                        if (data.MailNotificationSchedules.length > 0){
                            if(data.MailNotificationSchedules[0].DateTimestamp < moment().unix()) {
                                return "<i class='fa fa-check'></i>";
                            }
                        }
                        return "";
                    }
                },
                {
                    field: "Id",
                    template: "<a ng-click=editClick(${Id})>Redigér</a> | <a ng-click=deleteClick(${Id})>Slet</a>",
                    title: "Muligheder"
                }
            ]
        };

        $scope.updateNotificationGrid = function () {
            /// <summary>
            /// Refreshes kendo grid datasource.
            /// </summary>
            $scope.gridContainer.notificationGrid.dataSource.read();
        }

        $scope.editClick = function (id) {
            /// <summary>
            /// Opens Edit MailNotification modal
            /// </summary>
            /// <param name="id">Id of MailNotification to edit</param>
            var modalInstance = $modal.open({
                templateUrl: '/App/Admin/HTML/Email/AddEditFileGenerationScheduleTemplate.html',
                controller: 'AddEditFileGenerationScheduleController',
                backdrop: "static",
                resolve: {
                    itemId: function () {
                        return id;
                    }
                }
            });

            modalInstance.result.then(function (result) {
                FileGenerationSchedule.patch({ id: id }, {
                    "DateTimestamp": result.FileGenerationSchedule.DateTimestamp,
                    "Repeat": result.FileGenerationSchedule.Repeat
                }, function() {
                    $scope.updateNotificationGrid();
                });

                angular.forEach(result.DeletedMailsIds, function(deleteId) {
                    if(deleteId > 0) {
                        EmailNotification.delete({ id: deleteId} );
                    }
                });

                angular.forEach(result.FileGenerationSchedule.MailNotificationSchedules, function(mailnotif){
                    if(mailnotif.Id > 0) {
                        EmailNotification.patch({id: mailnotif.Id}, {
                            "DateTimestamp": mailnotif.DateTimestamp,
                            "CustomText": mailnotif.CustomText
                        });                        
                    }
                    else {
                        mailnotif.FileGenerationScheduleId = id;
                        EmailNotification.post(mailnotif);
                    }                    
                });
            });
        }

        //$scope.pageSizeChanged = function () {
        //    $scope.gridContainer.notificationGrid.dataSource.pageSize(Number($scope.gridContainer.gridPageSize));
        //}

        $scope.deleteClick = function (id) {
            /// <summary>
            /// Opens delete MailNotification modal
            /// </summary>
            /// <param name="id">Id of MailNotification to delete</param>
            var modalInstance = $modal.open({
                templateUrl: '/App/Admin/HTML/Email/ConfirmDeleteFileGenerationScheduleTemplate.html',
                controller: 'DeleteFileGenerationScheduleController',
                backdrop: "static",
                resolve: {
                    itemId: function () {
                        return id;
                    }
                }
            });

            modalInstance.result.then(function () {
                FileGenerationSchedule.delete({ id: id }, function () {
                    $scope.updateNotificationGrid();
                });
            });
        }

        $scope.addNewClick = function () {
            /// <summary>
            /// Opens add new MailNotification modal
            /// </summary>
            var modalInstance = $modal.open({
                templateUrl: '/App/Admin/HTML/Email/AddEditFileGenerationScheduleTemplate.html',
                controller: 'AddEditFileGenerationScheduleController',
                backdrop: "static",
                resolve: {
                    itemId: function () {
                        return -1;
                    }
                }
            });

            modalInstance.result.then(function (result) {
                FileGenerationSchedule.post(result.FileGenerationSchedule, function () {
                    $scope.updateNotificationGrid();
                });
            });
        }


    }
]);
