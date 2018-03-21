angular.module('application').controller('ReportController', [
    "$scope", "$rootScope", "$window", "$state", "Person", "Autocomplete", "OrgUnit", "MkColumnFormatter", "RouteColumnFormatter",
    function ($scope, $rootScope, $window, $state, Person, Autocomplete, OrgUnit, MkColumnFormatter, RouteColumnFormatter) {

        $scope.gridContainer = {};
        $scope.dateContainer = {};

        $scope.container = {};
        $scope.persons = Autocomplete.allUsers();
        $scope.orgUnits = Autocomplete.orgUnits();
        $scope.gridContainer.TESTING = "TESTING";
        $scope.showReport = false;

        $scope.dateOptions = {
            format: "dd/MM/yyyy",
 
        };

        $scope.container = {};


        var today = new Date();
        var dd = today.getDate();
        var mm = today.getMonth() + 1; //January is 0!
        var yyyy = today.getFullYear();

        if (dd < 10) {
            dd = '0' + dd
        }

        if (mm < 10) {
            mm = '0' + mm
        }

        today = dd + '/' + mm + '/' + yyyy;
        $('#dateCreated').text(today);
        $scope.Today = today;

        $scope.personAutoCompleteOptions = {
            filter: "contains",
            select: function (e) {
                $scope.container.chosenPersonId = this.dataItem(e.item.index()).Id;
            }
        };

        $scope.orgunitAutoCompleteOptions = {
            filter: "contains",
            select: function (e) {
                $scope.container.chosenOrgunitId = this.dataItem(e.item.index()).Id;
            }
        };

        $scope.createReportClick = function () {
            var personId = $scope.container.chosenPersonId;
            var orgunitId = $scope.container.chosenOrgunitId;
            var fromUnix = $scope.getStartOfDayStamp($scope.dateContainer.fromDate);
            var toUnix = $scope.getEndOfDayStamp($scope.dateContainer.toDate);

            // $scope.container.chosenPersonId = "";
            // $scope.container.chosenOrgunitId = "";

             if ($scope.container.employeeFilter != undefined && $scope.container.reportFromDateString != undefined && $scope.container.reportToDateString != undefined) {
                $scope.gridContainer.reportsGrid.dataSource.transport.options.read.url = getDataUrl(fromUnix, toUnix, personId, orgunitId);
                $scope.gridContainer.reportsGrid.dataSource.read();                
                $scope.showReport = true;

                // if(result != null && result != undefined) {
                //     $scope.Name = result.Name;
                //     $scope.LicensePlates = result.LicensePlates;
                //     $scope.OrgUnit = result.OrgUnit;
                //     $scope.Municipality = result.Municipality;
                //     $scope.DateInterval = result.DateInterval;
                //     $scope.AdminName = result.AdminName;
                //     $scope.HomeAddressStreet = result.HomeAddressStreetAndNumber;
                //     $scope.HomeAddressTown = result.HomeAddressZipCodeAndTown;
        
                //     reports = result.DriveReports;               
                // }

            }else {
                alert('Du mangler at udfylde et felt med en *');
            }       
        }

        $scope.getEndOfDayStamp = function (d) {
            var m = moment(d);
            return m.endOf('day').unix();
        }
 
        $scope.getStartOfDayStamp = function (d) {
            var m = moment(d);
            return m.startOf('day').unix();
        }

        $scope.updateData = function (data) {
            if(data.value[0] != undefined && data.value[0] != null) {
                result = data.value[0];
                $scope.Name = result.Person.FullName;
                $scope.LicensePlates = result.LicensePlate;
                if($scope.container.orgUnitFilter != undefined && $scope.container.orgUnitFilter != "") 
                    $scope.OrgUnit = $scope.container.orgUnitFilter;
                else 
                    $scope.OrgUnit = "Ikke angivet";
                
                $scope.Municipality = ""; //result.Municipality; from customSettings???
                $scope.DateInterval = $scope.container.reportFromDateString + " - " + $scope.container.reportToDateString;
                var homeAddress = $scope.findHomeAddress(result.Person.PersonalAddresses);
                //$scope.AdminName = result.AdminName;
                if(homeAddress != null && homeAddress != undefined) {
                    $scope.HomeAddressStreet = homeAddress.StreetName + " " + homeAddress.StreetNumber;
                    $scope.HomeAddressTown = homeAddress.ZipCode + " " + homeAddress.Town;
                }
                else {
                    $scope.HomeAddressStreet = "N/A";
                    $scope.HomeAddressTown = "N/A"; 
                }
            }
            else {
                // Report that the search returns no values
            }
        }

        $scope.findHomeAddress = function(addresses) {
            var result;
            angular.forEach(addresses, function(value) {
                if(result == undefined) {
                    if (value.Type == "Home" && result == undefined) {
                        result = value;
                    }
                }
            });
            return result;
        }
 
        var getDataUrl = function (startDate, endDate, personId, orgUnit) {
            var url = "/odata/DriveReports? getReportsWhereSubExists=true &$expand=DriveReportPoints,ResponsibleLeader,Employment($expand=OrgUnit),Person($expand=PersonalAddresses),ApprovedBy";
            var filters = "&$filter=DriveDateTimestamp ge " + startDate + " and DriveDateTimestamp le " + endDate;
            if (personId != undefined && personId > 0) {
                filters += " and PersonId eq " + personId;
            }
            if (orgUnit != undefined && orgUnit != "") {
                filters += " and Employment/OrgUnitId eq " + orgUnit;
            }
            var result = url + filters;
            return result;
        }

        $scope.reports = {
            toolbar: ["excel", "pdf"],
            excel: {
                fileName: "Rapport-" + today + ".xlsx",
                proxyURL: "//demos.telerik.com/kendo-ui/service/export",
                filterable: false
            }, pdf: {
                margin: { top: "1cm", left: "1cm", right: "1cm", bottom: "1cm" },
                landscape: true,
                allPages: true,
                /*paperSize: "A4",
                avoidLinks: true,
                margin: { top: "2cm", left: "1cm", right: "1cm", bottom: "1cm" },
                repeatHeaders: true,
                template: $("#page-template").html(),
                scale: 0.1*/
                
                fileName: "Rapport-" + today + ".Pdf"
            },
            autoBind: false,
            dataSource: {
                type: "odata-v4",
                transport: {
                    read: {
                        beforeSend: function (req) {
                            req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                        },

                        url: "/odata/DriveReports?status=Pending &getReportsWhereSubExists=true &$expand=DriveReportPoints,ResponsibleLeader,Employment($expand=OrgUnit)",
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
                        var tmp = data;
                        $scope.updateData(data);
                        return data.value; // <-- The result is just the data, it doesn't need to be unpacked.
                    },
                    total: function (data) {
                        return data['@odata.count']; // <-- The total items count is the data length, there is no .Count to unpack.
                    }
                },
                pageSize: 20,
                serverPaging: true,
                serverAggregates: false,
                serverSorting: true,
                serverFiltering: true,
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
                pageSizes: [5, 10, 20, 30, 40, 50, 100, 150, 200],
            },
            dataBound: function () {
                this.expandRow(this.tbody.find("tr.k-master-row").first());
            },
            resizable: true,
            columns: [
                {
                    field: "DriveDateTimestamp",
                    title: "Dato for kørsel", 
                    template: function (data) {
                        if (data.DriveDateTimestamp > 0) {
                            var m = moment.unix(data.DriveDateTimestamp);
                            return m._d.getDate() + "/" +
                                (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                m._d.getFullYear();
                        }
                        else {
                            return "";
                        }
                    },
                    width: 100, /*footerTemplate: "Beløb:"+result.wholeAmount +  "<br/>Distance: " + result.wholeDistance*/
                },
                {
                    field: "CreatedDateTimestamp",
                    title: "Dato for indberetning", 
                    template: function (data) {
                        if (data.CreatedDateTimestamp > 0) {
                            var m = moment.unix(data.CreatedDateTimestamp);
                            return m._d.getDate() + "/" +
                                (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                m._d.getFullYear();
                        }
                        else {
                            return "";
                        }
                    },
                    width: 100
                },
                { 
                    field: "Employment.OrgUnit.LongDescription", 
                    title: "Org. Enhed", 
                    width: 100 
                },
                { 
                    field: "Purpose", 
                    title: "Formål",
                    width: 150
                },
                {
                    title: "Rute",
                    field: "DriveReportPoints",
                    template: function (data) {
                        var routeText = ""
                        angular.forEach(data.DriveReportPoints, function (point, key) {
                            if (key != data.DriveReportPoints.length - 1) {
                                routeText += point.StreetName + " " + point.StreetNumber + ", " + point.ZipCode + " " + point.Town + " - <br/>";
                            } else {
                                routeText += point.StreetName + " " + point.StreetNumber + ", " + point.ZipCode + " " + point.Town;
                            }
                        });
                        return routeText;
                    },
                    width: 150
                },
                {
                    field: "IsRoundTrip", 
                    title: "Retur",
                    template: function (data) {
                        if (!data.IsRoundTrip || data.IsRoundTrip == null)
                            return "Nej";
                        else
                            return "Ja";
                    },
                    width: 50
                },
                {
                    field: "IsExtraDistance", 
                    title: "MK",
                    template: function (data) {
                        if (!data.IsExtraDistance || data.IsExtraDistance == null)
                            return "Nej";
                        else
                            return "Ja";
                    },
                    width: 40
                },
                {
                    field: "FourKmRule", 
                    title: "4-km",
                    template: function (data) {
                        if (!data.FourKmRule || data.FourKmRule == null)
                            return "Nej";
                        else
                            return "Ja";
                    },
                    width: 50
                },
                {
                    field: "FourKmRuleDeducted", 
                    title: "4-km fratrukket",
                    width: 50
                },
                { 
                    field: "DistanceFromHomeToBorder", 
                    title: "KM til kommunegrænse",
                    template: function (data) {
                        if (data.FourKmRule) {
                            if(data.IsRoundTrip) {
                                return data.Person.DistanceFromHomeToBorder * 2;
                            }
                            else {
                                return data.Person.DistanceFromHomeToBorder;
                            }
                        }
                        else {
                            return 0;
                        }
                    }, 
                    width: 110 
                },
                {
                    field: "SixtyDaysRule", 
                    title: "60-dage",
                    template: function (data) {
                        if (!data.SixtyDaysRule || data.SixtyDaysRule == null)
                            return "Nej";
                        else
                            return "Ja";
                    },
                    width: 50
                },
                {
                    field: "Distance", 
                    title: "KM til udbetaling",
                    template: 
                        function (data) {
                            return data.Distance.toFixed(2).toString().replace('.', ',') + " km ";
                        }
                    , 
                    footerTemplate: "Total: #= kendo.toString(sum, '0.00').replace('.',',') # km.",
                    width: 100,
                },
                {
                    field: "AmountToReimburse", 
                    title: "Beløb",
                    template: function (data) {
                        return data.AmountToReimburse.toFixed(2).toString().replace('.', ',') + " kr.";
                    }, 
                    footerTemplate: "Total: #= kendo.toString(sum, '0.00').replace('.',',') # Kr.",
                    width: 100,
                },
                { 
                    field: "KmRate", 
                    title: "Takst",
                    template: 
                        function (data) {
                            return data.KmRate.toFixed(2).toString().replace('.', ',') + " øre/km ";
                        },
                    width: 100
                },
                {
                    field: "Status",
                    title: "Status",
                    template: function (data) {
                        if (data.Status == "Pending")
                            return "Afventer";
                        else if (data.Status == "Accepted")
                            return "Godkendt";
                        else if (data.Status == "Rejected")
                            return "Afvist";
                        else 
                            return "Overført til løn";
                    },
                    width: 100
                },
                { 
                    field: "ClosedDateTimestamp", 
                    title: "Godkendt/Afvist dato",
                    template: function (data) {
                        if (data.ClosedDateTimestamp > 0) {
                            var m = moment.unix(data.ClosedDateTimestamp);
                            return m._d.getDate() + "/" +
                                (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                m._d.getFullYear();
                        }
                        else {
                            return "";
                        }
                    },
                    width: 100 
                },
                { 
                    field: "ProcessedDateTimestamp", 
                    title: "Sendt til løn",
                    template: function (data) {
                        if (data.ProcessedDateTimestamp > 0) {
                            var m = moment.unix(data.ProcessedDateTimestamp);
                            return m._d.getDate() + "/" +
                                (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                m._d.getFullYear();
                        }
                        else {
                            return "";
                        }
                    },
                    width: 100 
                },
                { 
                    field: "ApprovedBy.FullName", 
                    title: "Godkendt/Afvist af" ,
                    template: function (data) {
                        if (data.ApprovedBy == null || data.ApprovedBy == undefined)
                            return "";
                        else
                            return data.ApprovedBy.FullName;
                    },
                    width: 150
                },
                { 
                    field: "UserComment", 
                    title: "Bemærkning",
                    width: 100 
                }

            ],
            excelExport: function (e) {
                // e.workbook.sheets[1] will contain employee data from grid header.
                e.workbook.sheets[1] = {
                    rows:[
                        {
                            cells: [ // this is a row
                                { value: "Navn" }, // this is column 1
                                { value: $scope.Name } // this is column 2
                            ]
                        },
                        {
                            cells: [ 
                                { value: "Nummerplade" },
                                { value: $scope.LicensePlates }
                            ]
                        },
                        {
                            cells: [
                                { value: "Adresse" }, 
                                { value: $scope.HomeAddressStreet + " " + $scope.HomeAddressTown} 
                            ]
                        },
                        {
                            cells: [
                                { value: "Afdeling" }, 
                                { value: $scope.OrgUnit} 
                            ]
                        },
                        {
                            cells: [
                                { value: "Kommune" },
                                { value: $scope.Municipality}
                            ]
                        },
                        {
                            cells: [ 
                                { value: "Dato interval for udbetaling" },
                                { value: $scope.DateInterval}
                            ]
                        },
                        {
                            cells: [
                                { value: "Admin" }, 
                                { value: $scope.AdminName} 
                            ]
                        },
                        {
                            cells: [
                                { value: "Dato for rapportdannelse" },
                                { value: $scope.Today}
                            ]
                        }
                    ]
                }

                // e.workbook.sheets[0] contains reports
                var sheet0 = e.workbook.sheets[0];
                
                // Add roundtrip, extra distance and fourkmrule templates to the excel cheet columns.
                var IsRoundTripTemplate = kendo.template(this.columns[5].template);
                var IsExtraDistanceTemplate = kendo.template(this.columns[6].template);
                var FourKmRuleTemplate = kendo.template(this.columns[7].template);
                var SixtyDaysRuleTemplate = kendo.template(this.columns[10].template);

                for (var i = 1; i < sheet0.rows.length-1; i++) {
                    var row = sheet0.rows[i];

                    var IsRoundTripdataItem = {
                        IsRoundTrip: row.cells[5].value
                    };
                    var IsExtraDistancedataItem = {
                        IsExtraDistance: row.cells[6].value
                    };
                    var FourKmRuledataItem = {
                        FourKmRule: row.cells[7].value
                    };
                    var SixtyDaysRuledataItem = {
                        SixtyDaysRule: row.cells[10].value
                    };
                    row.cells[5].value = IsRoundTripTemplate(IsRoundTripdataItem);
                    row.cells[6].value = IsExtraDistanceTemplate(IsExtraDistancedataItem);
                    row.cells[7].value = FourKmRuleTemplate(FourKmRuledataItem);
                    row.cells[10].value = SixtyDaysRuleTemplate(SixtyDaysRuledataItem);
                }
            }
        }
    }
]);
