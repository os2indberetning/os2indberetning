angular.module("application").controller("DocumentController", [
    "$scope", "$rootScope", "$window",  //"$routeParams",
    function ($scope, $rootScope, $window) {

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
        var startDate = getParameterByName('from');
        var endDate = getParameterByName('to');
        var orgUnit = getParameterByName('orgUnit');


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

        today = dd + '-' + mm + '-' + yyyy;
        $('#dateCreated').text(today);
        $scope.Today = today;



        var reports;

        $.ajax({
            type: "GET",
            url: "odata/DriveReports/Service.Eksport?start=" + startDate + "&end=" + endDate + "&name=" + employee + "&orgUnit=" + orgUnit,
            contentType: "application/json;charset=utf-8",
            dataType: "json",
            success: function (result) {
                $scope.Name = result.Name;
                $scope.LicensePlates = result.LicensePlates;
                $scope.OrgUnit = result.OrgUnit;
                $scope.Municipality = result.Municipality;
                $scope.DateInterval = result.DateInterval;
                $scope.AdminName = result.AdminName;
                $scope.HomeAddressStreet = result.HomeAddressStreetAndNumber;
                $scope.HomeAddressTown = result.HomeAddressZipCodeAndTown;

                reports = result.DriveReports;
            },
            error: function (response) {
                alert('Kunne ikke finde det du forespurgte');
            }, async: false
        });


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
            dataSource: {
                data: reports,
                aggregate: [{ field: "AmountToReimburse", aggregate: "sum" }, { field: "Distance", aggregate: "sum" }],
                schema: {
                    model: {
                        fields: {
                            AmountToReimburse: { type: "number" }
                        }
                    }
                },
                pageSize: 10
            },
            resizable: true,
            columns: [
                {
                    field: "DriveDateTimestamp",
                    title: "Dato for kørsel", 
                    width: 100, /*footerTemplate: "Beløb:"+result.wholeAmount +  "<br/>Distance: " + result.wholeDistance*/
                },
                {
                    field: "CreatedDateTimestamp",
                    title: "Dato for indberetning", 
                    width: 100
                },
                { 
                    field: "OrgUnit", 
                    title: "Org. Enhed", 
                    width: 100 
                },
                { 
                    field: "Purpose", 
                    title: "Formål",
                    width: 150
                },
                { 
                    field: "Route", 
                    title: "Rute",
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
                    field: "DistanceFromHomeToBorder", 
                    title: "KM til kommunegrænse", 
                    width: 110 
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
                    field: "Rate", 
                    title: "Takst",
                    template: 
                        function (data) {
                            return data.Rate.toFixed(2).toString().replace('.', ',') + " øre/km ";
                        },
                    width: 100
                },
                { 
                    field: "ApprovedDate", 
                    title: "Godkendt dato",
                    width: 100 
                },
                { 
                    field: "ProcessedDate", 
                    title: "Sendt til løn",
                    width: 100 
                },
                { 
                    field: "ApprovedBy", 
                    title: "Godkendt af" ,
                    width: 150
                },
                { 
                    field: "Accounting", 
                    title: "Kontering",
                    width: 100 
                }

            ],
            excelExport: function (e) {
                var sheet = e.workbook.sheets[0];
                // Apply grid header to excel sheet.
                sheet.rows.unshift(
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
                )
                
                // Add roundtrip, extra distance and fourkmrule templates to the excel cheet columns.
                var IsRoundTripTemplate = kendo.template(this.columns[5].template);
                var IsExtraDistanceTemplate = kendo.template(this.columns[6].template);
                var FourKmRuleTemplate = kendo.template(this.columns[7].template);

                for (var i = 10; i < sheet.rows.length-1; i++) {
                    var row = sheet.rows[i];

                    var IsRoundTripdataItem = {
                        IsRoundTrip: row.cells[5].value
                    };
                    var IsExtraDistancedataItem = {
                        IsExtraDistance: row.cells[6].value
                    };
                    var FourKmRuledataItem = {
                        FourKmRule: row.cells[7].value
                    };
                    row.cells[5].value = IsRoundTripTemplate(IsRoundTripdataItem);
                    row.cells[6].value = IsExtraDistanceTemplate(IsExtraDistancedataItem);
                    row.cells[7].value = FourKmRuleTemplate(FourKmRuledataItem);
                }
            }
        }


    }]);
