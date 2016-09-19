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
        var manr = getParameterByName('manr');
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

        

        var reports;

        $.ajax({
            type: "GET",
            url: "odata/DriveReports/Service.Eksport?manr=" + manr + "&start=" + startDate + "&end=" + endDate + "&name=" + employee + "&orgUnit=" + orgUnit,

            contentType: "application/json;charset=utf-8",
            dataType: "json",
            success: function (result) {
                $scope.datoInterval = result.DateInterval;
                $('#MaNavn').text(result.name);
                $('#admin_der_har_trukket_rapporten').text(result.adminName);
                $('#Kommune').text(result.municipality);
               // $scope.wholeAmount = result.wholeAmount; 
               // $scope.wholeDistance = result.wholeDistance;
                $('#wholeDistance').text(result.wholeDistance);
                reports = result.driveReports;

                var str = "";
                var counter = 0;

                for (var i in result.MaNumbers) {
                    if (counter > 0) {
                        str = str + ", ";
                    }
                    str = str + result.MaNumbers[counter];
                    counter++;
                }
                counter = 0;
                $('#MaNr').text(str);
                str = "";
                for (var i in result.orgUnits) {
                    if (counter > 0) {
                        str = str + ", ";
                    }
                    str = str + result.orgUnits[counter];
                    counter++;
                }
                counter = 0;
                $('#Organisationsenheder').text(str);
                str = "";
            },
            error: function (response) {

                alert('Kunne ikke finde det du forespurgte');
            }, async: false
        });

        
          $scope.reports = {
              toolbar: ["excel", "pdf"],
              excel: {
                  fileName: "Rapport-"+today+".xlsx",
                  proxyURL: "//demos.telerik.com/kendo-ui/service/export",
                  filterable: true
              }, pdf: {
                  margin: { top: "1cm", left: "1cm", right: "1cm", bottom: "1cm" },
                  /*allPages: true,
                  avoidLinks: true,
                  paperSize: "A4",
                  margin: { top: "2cm", left: "1cm", right: "1cm", bottom: "1cm" },
                  landscape: true,
                  repeatHeaders: true,
                  template: $("#page-template").html(),
                  scale: 0.1*/
                  fileName: "Rapport-" + today + ".Pdf"
              },
              dataSource: {
                  data: reports,
                 aggregate: [{ field: "AmountToReimburse", aggregate: "sum" },{ field: "distance", aggregate: "sum" }],
                  schema: {
                      model: {
                          fields: {
                              AmountToReimburse: { type: "number" }
                          }
                      }
                  }/*
                    aggregate: [
               { field: "Distance", aggregate: "sum" },
               { field: "AmountToReimburse", aggregate: "sum" },
               ]*/
              },
              resizable: true,
              columns: [
                          {
                              field: "DriveDateTimestamp",
                               title: "Dato for kørsel", width: 100, /*footerTemplate: "Beløb:"+result.wholeAmount +  "<br/>Distance: " + result.wholeDistance*/
                          },
                          {
                              field: "CreatedDateTimestamp", 
                              title: "Dato for indberetning", width: 100
                          },
                          { field: "OrgUnit", title: "Org. Enhed", width: 100 },
                          { field: "Purpose", title: "Formål", width: 100 },
                          { field: "Route", title: "Rute", width: 100 },
                          { field: "isRoundTrip", title: "Retur", 
                          template: function (data) {
                            if(!data.isRoundTrip || data.isRoundTrip == null)
                                return "Nej.";
                            else
                                return "Ja.";
                          },
                            width: 50 },
                          { field: "IsExtraDistance", title: "MK", 
                          template: function (data) {
                            if(!data.IsExtraDistance)
                                return "Nej.";
                            else
                                return "Ja.";
                          },
                          width: 50 },
                          { field: "FourKmRule", title: "4-km", 
                          template: function (data) {
                            if(!data.FourKmRule || data.FourKmRule == null)
                                return "Nej.";
                            else
                                return "Ja.";
                          },
                          width: 50 },
                          { field: "distanceFromHomeToBorder", title: "KM til kommunegrænse", width: 100 },
                          
                          { field: "distance", title: "KM til udbetaling", 
                           template: function (data) {
                           return data.distance.toFixed(2).toString().replace('.', ',') + " km ";
                          },footerTemplate: "Total: #= kendo.toString(sum, '0.00').replace('.',',') # km.",
                          width: 100 ,
                           
                          //footerTemplate: "Total: #= kendo.toString(sum, '0.00').replace('.',',') # km"
                        },
                          
                          { field: "AmountToReimburse", title: "Beløb",
                           template: function (data) {
                            return data.AmountToReimburse.toFixed(2).toString().replace('.', ',') + " kr.";
                          },footerTemplate: "Total: #= kendo.toString(sum, '0.00').replace('.',',') # Kr.",
                           width: 100,
                  },
                          
                          { field: "approvedDate", title: "Godkendt dato" },
                          { field: "processedDate", title: "Sendt dato" },
                          { field: "ApprovedBy", title: "Godkendt af" },
                          { field: "kontering", title: "Kontering" }
                          
              ], 
              
               
               excelExport: function(e) {
                var sheet = e.workbook.sheets[0];
                
                var isRoundTripTemplate = kendo.template(this.columns[5].template);
                var IsExtraDistanceTemplate = kendo.template(this.columns[6].template);
                var FourKmRuleTemplate = kendo.template(this.columns[7].template);

                for (var i = 1; i < sheet.rows.length; i++) {
                var row = sheet.rows[i];

                var isRoundTripdataItem = {
                isRoundTrip: row.cells[5].value
                };
                var IsExtraDistancedataItem = {
                IsExtraDistance: row.cells[6].value
                };
                var FourKmRuledataItem = {
                FourKmRule: row.cells[7].value
                };
                row.cells[5].value = isRoundTripTemplate(isRoundTripdataItem);
                row.cells[6].value = IsExtraDistanceTemplate(IsExtraDistancedataItem);
                row.cells[7].value = FourKmRuleTemplate(FourKmRuledataItem);
            }
  }
          }

          
    }]);
