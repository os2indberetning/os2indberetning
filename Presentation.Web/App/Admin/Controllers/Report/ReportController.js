angular.module('application').controller('ReportController', [
    "$scope", "$rootScope", "$window", "$state", "Person", "Autocomplete", "OrgUnit", "MkColumnFormatter", "RouteColumnFormatter",
    function ($scope, $rootScope, $window, $state, Person, Autocomplete, OrgUnit, MkColumnFormatter, RouteColumnFormatter) {

        $scope.gridContainer = {};
        $scope.dateContainer = {};

        $scope.container = {};
        $scope.persons = Autocomplete.allUsers();
        $scope.orgUnits = Autocomplete.orgUnits();
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
                $scope.showReport = true;                
                result = data.value[0];
                $scope.Name = result.Person.FullName;
                $scope.LicensePlates = result.LicensePlate;
                if($scope.container.orgUnitFilter != undefined && $scope.container.orgUnitFilter != "") 
                    $scope.OrgUnit = $scope.container.orgUnitFilter;
                else 
                    $scope.OrgUnit = "Ikke angivet";
                
                $scope.Municipality = $rootScope.HelpTexts.muniplicity.text; 
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
                $scope.showReport = false;                
                alert('Kunne ikke finde det du forespurgte');
            }
            reports = data;
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
                filterable: false,
                allPages: true
            },
            pdf: {
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
            dataSource: {
                type: "odata-v4",
                transport: {
                    read: {
                        beforeSend: function (req) {
                            req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                        },

                        url: "",
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
                    model: {
                        fields: {
                            AmountToReimburse: { type: "number" }
                        }
                    },
                    data: function (data) {
                        $scope.updateData(data);
                        return data.value; // <-- The result is just the data, it doesn't need to be unpacked.
                    }
                },
                pageSize: 20,        
                sort: { field: "DriveDateTimestamp", dir: "desc" },
                aggregate: [
                    { field: "Distance", aggregate: "sum" },
                    { field: "AmountToReimburse", aggregate: "sum" },
                ],
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
                pageSizes: [5, 10, 20, 30, 40, 50, 100, 150, 200]
            },
            groupable: false,
            filterable: true,
            columnMenu: true,
            reorderable: true,
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
                                routeText += point.StreetName + " " + point.StreetNumber + ", " + point.ZipCode + " " + point.Town + " - ";
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
                        // {
                        //     cells: [
                        //         { value: "Admin" }, 
                        //         { value: $scope.AdminName} 
                        //     ]
                        // },
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
                var DriveDateTemplate = kendo.template(this.columns[0].template);
                var CreatedDateTemplate = kendo.template(this.columns[1].template);
                var RuteTemplate = kendo.template(this.columns[4].template);                
                var IsRoundTripTemplate = kendo.template(this.columns[5].template);
                var IsExtraDistanceTemplate = kendo.template(this.columns[6].template);
                var FourKmRuleTemplate = kendo.template(this.columns[7].template);
                var DistanceFromBordersTemplate = kendo.template(this.columns[9].template);                
                var SixtyDaysRuleTemplate = kendo.template(this.columns[10].template);
                var DistanceTemplate = kendo.template(this.columns[11].template);
                var AmountTemplate = kendo.template(this.columns[12].template);
                var KmRateTemplate = kendo.template(this.columns[13].template);
                var StatusTemplate = kendo.template(this.columns[14].template);                
                var ClosedDateTemplate = kendo.template(this.columns[15].template);
                var ProcessedDateTemplate = kendo.template(this.columns[16].template);
                var ApprovedByTemplate = kendo.template(this.columns[17].template);



                for (var i = 1; i < sheet0.rows.length-1; i++) {
                    var row = sheet0.rows[i];
                    var IsDriveDatedataItem = {
                        DriveDateTimestamp: row.cells[0].value
                    };
                    var IsCreatedDatedataItem = {
                        CreatedDateTimestamp: row.cells[1].value
                    };
                    var IsRutedataItem = {
                        DriveReportPoints: row.cells[4].value
                    };
                    var IsRoundTripdataItem = {
                        IsRoundTrip: row.cells[5].value
                    };
                    var IsExtraDistancedataItem = {
                        IsExtraDistance: row.cells[6].value
                    };
                    var FourKmRuledataItem = {
                        FourKmRule: row.cells[7].value
                    };
                    var IsDistanceFromBordersdataItem = {
                        DistanceFromHomeToBorder: row.cells[9].value
                    };
                    var SixtyDaysRuledataItem = {
                        SixtyDaysRule: row.cells[10].value
                    };
                    var DistancedataItem = {
                        Distance: row.cells[11].value
                    };
                    var AmountdataItem = {
                        AmountToReimburse: row.cells[12].value
                    };
                    var KmRatedataItem = {
                        KmRate: row.cells[13].value
                    };
                    var StatusdataItem = {
                        Status: row.cells[14].value
                    };
                    var ClosedDatedataItem = {
                        ClosedDateTimestamp: row.cells[15].value
                    };
                    var ProcessedDatedataItem = {
                        ProcessedDateTimestamp: row.cells[16].value
                    };
                    var ApprovedBydataItem = {
                        ApprovedBy: {
                            FullName: row.cells[17].value
                        }
                            
                    };

                    row.cells[0].value = DriveDateTemplate(IsDriveDatedataItem);
                    row.cells[1].value = CreatedDateTemplate(IsCreatedDatedataItem);
                    row.cells[4].value = RuteTemplate(IsRutedataItem);
                    row.cells[5].value = IsRoundTripTemplate(IsRoundTripdataItem);
                    row.cells[6].value = IsExtraDistanceTemplate(IsExtraDistancedataItem);
                    row.cells[7].value = FourKmRuleTemplate(FourKmRuledataItem);
                    row.cells[9].value = DistanceFromBordersTemplate(IsDistanceFromBordersdataItem);
                    row.cells[10].value = SixtyDaysRuleTemplate(SixtyDaysRuledataItem);
                    row.cells[11].value = DistanceTemplate(DistancedataItem);
                    row.cells[12].value = AmountTemplate(AmountdataItem);
                    row.cells[13].value = KmRateTemplate(KmRatedataItem);
                    row.cells[14].value = StatusTemplate(StatusdataItem);
                    row.cells[15].value = ClosedDateTemplate(ClosedDatedataItem);
                    row.cells[16].value = ProcessedDateTemplate(ProcessedDatedataItem);
                    row.cells[17].value = ApprovedByTemplate(ApprovedBydataItem);                    
                }
            }
        }
    }
]);
