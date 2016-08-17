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
                $('#DatoInterval').text(result.DateInterval);
                $('#MaNavn').text(result.name);
                $('#admin_der_har_trukket_rapporten').text(result.adminName);
                $('#Kommune').text(result.municipality);
                $('#wholeAmount').text(result.wholeAmount);
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
                  /*allPages: true,
                  avoidLinks: true,
                  paperSize: "A4",
                 // margin: { top: "2cm", left: "1cm", right: "1cm", bottom: "1cm" },
                  landscape: true,
                  repeatHeaders: true,
                  template: $("#page-template").html(),
                  scale: 0.1*/
                  fileName: "Rapport-" + today + ".Pdf"
              },
              dataSource: {
                  data: reports,
                /*  aggregate: [{ field: "AmountToReimburse", aggregate: "sum" }, { field: "AmountToReimburse", aggregate: "sum" }],
                  schema: {
                      model: {
                          fields: {
                              AmountToReimburse: { type: "number" }
                          }
                      }
                  }*/
              },
              resizable: true,
              columns: [
                          {
                              field: "DriveDateTimestamp", template: function (data) {
                                  var m = moment.unix(data.DriveDateTimestamp);
                                  return m._d.getDate() + "/" +
                                      (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                      m._d.getFullYear();
                              }, title: "Dato for kørsel", width: 100, /*footerTemplate: "Beløb:"+result.wholeAmount +  "<br/>Distance: " + result.wholeDistance*/
                          },
                          {
                              field: "CreatedDateTimestamp", template: function (data) {
                                  var m = moment.unix(data.CreatedDateTimestamp);
                                  return m._d.getDate() + "/" +
                                      (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                      m._d.getFullYear();
                              }, title: "Dato for indberetning", width: 100
                          },
                          { field: "OrgUnit", title: "Org. Enhed", width: 100 },
                          { field: "Purpose", title: "Formål", width: 100 },
                          { field: "Route", title: "Rute", width: 100 },
                          { field: "IsExtraDistance", title: "Merkørselsangivelse", width: 100 },
                          { field: "FourKmRule", title: "4-km", width: 100 },
                          { field: "distanceFromHomeToBorder", title: "km til kommunegrænse", width: 100 },
                           { field: "distanceFromHomeToBorder", title: "Km til udbetaling", width: 100 },
                          { field: "AmountToReimburse", title: "Beløb", /*footerTemplate: "Samlet: #= sum # ",*/ width: 100 },
                          
                          { field: "approvedDate", title: "Godkendt dato" },
                          { field: "processedDate", title: "Sendt dato" },
                          { field: "ApprovedBy", title: "Godkendt af" },
                          { field: "Account", title: "Kontering" }
                          
              ], scrollable: false
          }
     /*     alert(reports);
    
            function PrintElem(elem) {
             Popup($(elem).html());
         }

         function Popup(data) {

             var today = new Date();
             var dd = today.getDate();
             var mm = today.getMonth()+1; //January is 0!
             var yyyy = today.getFullYear();
             
             if(dd<10) {
                 dd='0'+dd
             } 

             if(mm<10) {
                 mm='0'+mm
             } 

             today = mm+'/'+dd+'/'+yyyy;


             var mywindow = window.open('', today, 'height=400,width=600');
             mywindow.document.write('<html><head><title>' + today + '</title>');
             /*optional stylesheet //mywindow.document.write('<link rel="stylesheet" href="main.css" type="text/css" />');
             mywindow.document.write('</head><body >');
             mywindow.document.write(data);
             mywindow.document.write('</body></html>');

             mywindow.document.close(); // necessary for IE >= 10
             mywindow.focus(); // necessary for IE >= 10

             mywindow.print();
             mywindow.close();

             return true;
         }

         function print(){
         
             PrintElem("#printThis");
         
         }


         $scope.print = function () {
             PrintElem("#printThis");
         };*/
         $scope.saveAsPdf = function () {
             alert('saveAsPdf Clicked :-)')
         };
         $scope.downloadAsExcel = function () {
             alert('downloadAsExcel Clicked :-)')
         };
    }]);
