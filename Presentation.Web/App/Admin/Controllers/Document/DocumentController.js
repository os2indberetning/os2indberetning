angular.module("application").controller("DocumentController", [
    "$scope", "$rootScope", "$window", "Rate", "NotificationService","rateType" //"$routeParams",
    function ($scope, $rootScope, $window, $routeParams, $Rate, $NotificationService,$rateType) {
        
        $scope.container = {};


        function getParameterByName(name, url) {
            if (!url) url = window.location.href;
            name = name.replace(/[\[\]]/g, "\\$&");
            var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
                results = regex.exec(url);
            if (!results) return null;
            if (!results[2]) return '';
            return decodeURIComponent(results[2].replace(/\+/g, " "));
        }

        var employee = getParameterByName('Employee');
        var manr = getParameterByName('manr');
        var startDate = getParameterByName('from');
        var endDate = getParameterByName('to');


        document.getElementById('employeeHeader').innerText = employee;
        document.getElementById('MAnrHeader').innerText = manr;
        document.getElementById('startDateHeader').innerText = startDate;
        document.getElementById('endDateHeader').innerText = endDate;
        
        var allReports = [];
        
        /// <summary>
        /// Loads existing rates from backend to kendo grid datasource.
        /// </summary>
        $scope.rates = {
            autoBind: false,
            dataSource: {
                type: "odata",
                transport: {
                    read: {
                        beforeSend: function (req) {
                            req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                        },
                        url: "/odata/Rates?$expand=Type&$filter=Active eq true",
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
                serverSorting: true,
            },
            sortable: true,
            pageable: {
                messages: {
                    display: "{0} - {1} af {2} takster", //{0} is the index of the first record on the page, {1} - index of the last record on the page, {2} is the total amount of records
                    empty: "Ingen takster at vise",
                    page: "Side",
                    of: "af {0}", //{0} is total amount of pages
                    itemsPerPage: "takster pr. side",
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
                    field: "Year",
                    title: "År"
                }, {
                    field: "KmRate",
                    title: "Takst",
                    template: "${KmRate} ører pr/km"
                }, {
                    field: "Type.TFCode",
                    title: "TF kode",
                },
                {
                    field: "Type",
                    title: "Type",
                    template: function (data) {
                        return data.Type.Description;
                    }
                }
            ],
        };

        $scope.updateRatesGrid = function () {
            /// <summary>
            /// Refreshes kendo grid datasource.
            /// </summary>
            $scope.container.rateGrid.dataSource.read();
        }

        $scope.$on("kendoWidgetCreated", function (event, widget) {
            if (widget === $scope.container.rateDropDown) {
                $scope.rateTypes = RateType.get(function () {
                    angular.forEach($scope.rateTypes, function (rateType, key) {
                        rateType.Description += " (" + rateType.TFCode + ")"
                    });
                    $scope.container.rateDropDown.dataSource.read();
                    $scope.container.rateDropDown.select(0);
                });
            }
        });



        $scope.downloadReportClick = function () {
            //$window.open('/eksport/index?Employee=' + $scope.container.employeeFilter + '&manr=' + $scope.container.MANrFilter + '&from= ' + $scope.container.reportFromDateString + '&to=' + $scope.container.reportToDateString, '_blank');
            //$window.open('app/admin/html/report/DocumentView.html');
            var text = "";
            for (var t in $scope.Reports.dataSource) {
                text = text + " - " + t


            }
            text = text + "\n" + $scope.Reports.length;

            alert(getParameterByName('bog') + 'list: '+ text);
        }
    }]);
