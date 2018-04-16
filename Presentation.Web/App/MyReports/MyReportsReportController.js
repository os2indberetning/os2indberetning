angular.module('application').controller('MyReportsReportController', [
    "$scope", "$rootScope", "$window", "$state", "Person", "Autocomplete", "OrgUnit", "MkColumnFormatter", "RouteColumnFormatter", "PersonEmployments",
    function ($scope, $rootScope, $window, $state, Person, Autocomplete, OrgUnit, MkColumnFormatter, RouteColumnFormatter, PersonEmployments) {

        $scope.gridContainer = {};
        $scope.dateContainer = {};
        $scope.container = {};

        $scope.showReport = false;
        $scope.container.chosenPersonId = $rootScope.CurrentUser.Id;
        $scope.container.employeeFilter = $rootScope.CurrentUser.FullName;
        $scope.Employments = $rootScope.CurrentUser.Employments;

        angular.forEach($rootScope.CurrentUser.Employments, function (value, key) {
            value.PresentationString = value.Position + " - " + value.OrgUnit.LongDescription + " (" + value.EmploymentId + ")";
        });
            

        $scope.dateOptions = {
            format: "dd/MM/yyyy" 
        };

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

        $scope.getSelectedEmployment = function (selectedId) {
            var result;
            angular.forEach($scope.Employments, function(value) {
                if(result == undefined) {
                    if (value.Id == selectedId) {
                        result = value;
                    }
                }
            });   
            return result;   
        };

        // $scope.personAutoCompleteOptions = {
        //     filter: "contains",
        //     select: function (e) {
        //         $scope.container.chosenPersonId = this.dataItem(e.item.index()).Id;
        //     }
        // };

        $scope.orgunitAutoCompleteOptions = {
            filter: "contains",
            select: function (e) {
                $scope.container.chosenOrgunitId = this.dataItem(e.item.index()).Id;
            }
        };

        $scope.createReportClick = function () {
            var personId = $scope.container.chosenPersonId;
            var orgunitId = $scope.getSelectedEmployment($scope.container.SelectedEmployment).OrgUnit.Id;
            var fromUnix = $scope.getStartOfDayStamp($scope.dateContainer.fromDate);
            var toUnix = $scope.getEndOfDayStamp($scope.dateContainer.toDate);
            
            if ($scope.container.employeeFilter != undefined && $scope.container.reportFromDateString != undefined && $scope.container.reportToDateString != undefined) {
                $scope.gridContainer.reportsGrid.dataSource.transport.options.read.url = getDataUrl(fromUnix, toUnix, personId, orgunitId);
                $scope.gridContainer.reportsGrid.dataSource.read();
                $scope.showReport = true;                
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
            $scope.Name = $scope.container.employeeFilter;
            $scope.Municipality = $rootScope.HelpTexts.muniplicity.text; 
            $scope.DateInterval = $scope.container.reportFromDateString + " - " + $scope.container.reportToDateString;
            $scope.LicensePlates = $rootScope.CurrentUser.LicensePlates[0].Plate;

            var selectedEmpl = $scope.getSelectedEmployment($scope.container.SelectedEmployment);
                if(selectedEmpl != undefined && selectedEmpl != null) 
                    $scope.OrgUnit = selectedEmpl.OrgUnit.LongDescription;
                else 
                    $scope.OrgUnit = "Ikke angivet"; 

            $scope.HomeAddressStreet = "N/A";
            $scope.HomeAddressTown = "N/A"; 

            if(data.value[0] != undefined && data.value[0] != null) {
                result = data.value[0];                               
                
                var homeAddress = $scope.findHomeAddress(result.Person.PersonalAddresses);
                if(homeAddress != null && homeAddress != undefined) {
                    $scope.HomeAddressStreet = homeAddress.StreetName + " " + homeAddress.StreetNumber;
                    $scope.HomeAddressTown = homeAddress.ZipCode + " " + homeAddress.Town;
                }
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
            var url = "/odata/DriveReports?queryType=mine&$expand=DriveReportPoints,Employment($expand=OrgUnit),Person($expand=PersonalAddresses),ApprovedBy";
            var filters = "&$filter=DriveDateTimestamp ge " + startDate + " and DriveDateTimestamp le " + endDate;
            // if (personId != undefined && personId > 0) {
            //     filters += " and PersonId eq " + personId;
            // }
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
                    parse: function(data) {
                        $.each(data.value, function(idx, elem) {
                            var routeText = ""
                            angular.forEach(elem.DriveReportPoints, function (point, key) {
                                if (key != elem.DriveReportPoints.length - 1) {
                                    routeText += point.StreetName + " " + point.StreetNumber + ", " + point.ZipCode + " " + point.Town + " - ";
                                } else {
                                    routeText += point.StreetName + " " + point.StreetNumber + ", " + point.ZipCode + " " + point.Town;
                                }
                            });
                            elem.RoutePointsText = routeText;
                        });
                        return data;
                    },
                    model: {                        
                        fields: {
                            AmountToReimburse: { type: "number" },
                            RoutePointsText: {type: "string"}
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
            columnMenu: true,
            filterable: true,
            sortable: true,
            resizable: true,
            columns: [
                {
                    field: "DriveDateTimestamp",
                    title: "Dato for kørsel", 
                    filterable: false,
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
                    filterable: false,
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
                    field: "Person.FullName", 
                    title: "Medarbejder",
                    filterable: false,
                    width: 100 
                },
                { 
                    field: "Employment.EmploymentId", 
                    title: "MA.NR.", 
                    filterable: false,
                    width: 50 
                },
                { 
                    field: "Employment.OrgUnit.LongDescription", 
                    title: "Org. Enhed", 
                    filterable: false,
                    width: 100 
                },
                { 
                    field: "Purpose", 
                    title: "Formål",
                    filterable: false,
                    width: 150
                },
                {
                    title: "Rute",
                    field: "RoutePointsText",
                    width: 150
                },
                {
                    field: "IsRoundTrip", 
                    title: "Retur",
                    filterable: false,
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
                    filterable: false,
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
                    filterable: false,
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
                    filterable: false,
                    width: 50
                },
                { 
                    field: "DistanceFromHomeToBorder", 
                    title: "KM til kommunegrænse",
                    filterable: false,
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
                    filterable: false,
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
                    filterable: false,
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
                    filterable: false,
                    template: function (data) {
                        return data.AmountToReimburse.toFixed(2).toString().replace('.', ',') + " kr.";
                    }, 
                    footerTemplate: "Total: #= kendo.toString(sum, '0.00').replace('.',',') # Kr.",
                    width: 100,
                },
                { 
                    field: "KmRate", 
                    title: "Takst",
                    filterable: false,
                    template: 
                        function (data) {
                            return data.KmRate.toString() + " øre/km ";
                        },
                    width: 100
                },
                {
                    field: "Status",
                    title: "Status",
                    filterable: false,
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
                    filterable: false,
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
                    filterable: false,
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
                    filterable: false,
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
                    filterable: false,
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
                                { value: "Kørselsdato interval" },
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
                var IsRoundTripTemplate = kendo.template(this.columns[7].template);
                var IsExtraDistanceTemplate = kendo.template(this.columns[8].template);
                var FourKmRuleTemplate = kendo.template(this.columns[9].template);
                var DistanceFromBordersTemplate = kendo.template(this.columns[11].template);                
                var SixtyDaysRuleTemplate = kendo.template(this.columns[12].template);
                var DistanceTemplate = kendo.template(this.columns[13].template);
                var AmountTemplate = kendo.template(this.columns[14].template);
                var KmRateTemplate = kendo.template(this.columns[15].template);
                var StatusTemplate = kendo.template(this.columns[16].template);                
                var ClosedDateTemplate = kendo.template(this.columns[17].template);
                var ProcessedDateTemplate = kendo.template(this.columns[18].template);
                var ApprovedByTemplate = kendo.template(this.columns[19].template);



                for (var i = 1; i < sheet0.rows.length-1; i++) {
                    var row = sheet0.rows[i];
                    var IsDriveDatedataItem = {
                        DriveDateTimestamp: row.cells[0].value
                    };
                    var IsCreatedDatedataItem = {
                        CreatedDateTimestamp: row.cells[1].value
                    };                   
                    var IsRoundTripdataItem = {
                        IsRoundTrip: row.cells[7].value
                    };
                    var IsExtraDistancedataItem = {
                        IsExtraDistance: row.cells[8].value
                    };
                    var FourKmRuledataItem = {
                        FourKmRule: row.cells[9].value
                    };
                    var IsDistanceFromBordersdataItem = {
                        DistanceFromHomeToBorder: row.cells[11].value
                    };
                    var SixtyDaysRuledataItem = {
                        SixtyDaysRule: row.cells[12].value
                    };
                    var DistancedataItem = {
                        Distance: row.cells[13].value
                    };
                    var AmountdataItem = {
                        AmountToReimburse: row.cells[14].value
                    };
                    var KmRatedataItem = {
                        KmRate: row.cells[15].value
                    };
                    var StatusdataItem = {
                        Status: row.cells[16].value
                    };
                    var ClosedDatedataItem = {
                        ClosedDateTimestamp: row.cells[17].value
                    };
                    var ProcessedDatedataItem = {
                        ProcessedDateTimestamp: row.cells[18].value
                    };
                    var ApprovedBydataItem = {
                        ApprovedBy: {
                            FullName: row.cells[19].value
                        }
                            
                    };

                    row.cells[0].value = DriveDateTemplate(IsDriveDatedataItem);
                    row.cells[1].value = CreatedDateTemplate(IsCreatedDatedataItem);
                    row.cells[7].value = IsRoundTripTemplate(IsRoundTripdataItem);
                    row.cells[8].value = IsExtraDistanceTemplate(IsExtraDistancedataItem);
                    row.cells[9].value = FourKmRuleTemplate(FourKmRuledataItem);
                    row.cells[11].value = DistanceFromBordersTemplate(IsDistanceFromBordersdataItem);
                    row.cells[12].value = SixtyDaysRuleTemplate(SixtyDaysRuledataItem);
                    row.cells[13].value = DistanceTemplate(DistancedataItem);
                    row.cells[14].value = AmountTemplate(AmountdataItem);
                    row.cells[15].value = KmRateTemplate(KmRatedataItem);
                    row.cells[16].value = StatusTemplate(StatusdataItem);
                    row.cells[17].value = ClosedDateTemplate(ClosedDatedataItem);
                    row.cells[18].value = ProcessedDateTemplate(ProcessedDatedataItem);
                    row.cells[19].value = ApprovedByTemplate(ApprovedBydataItem);                    
                }
            }
        }
    }
]);
