angular.module("application").controller("DocumentController", [
    "$scope", "$rootScope", "$window", //"$routeParams",
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


        /*document.getElementById('employeeHeader').innerText = employee;
        document.getElementById('MAnrHeader').innerText = manr;
        document.getElementById('startDateHeader').innerText = startDate;
        document.getElementById('endDateHeader').innerText = endDate;*/



        $scope.reports = {
            toolbar: ["excel","pdf","Print"],
            excel: {
                fileName: "Kørsels udtræk.xlsx",
                proxyURL: "//demos.telerik.com/kendo-ui/service/export",
                filterable: true,
                display: false
            },pdf: {
                allPages: true,
                avoidLinks: true,
                landscape: false,
                repeatHeaders: true

           
            },
             dataSource: {
                 transport: {
                     read: {
                         beforeSend: function (req) {
                             req.setRequestHeader('Accept', 'application/json;odata=fullmetadata');
                         },
                         url: "odata/DriveReports/Service.Eksport?manr=" + manr + "&start=" + startDate + "&end=" + endDate + "&name=" + employee + "&orgUnit=" + orgUnit,
                         //url: "odata/DriveReports/Service.Eksport?manr=5&start=start&end=end&name=rasmus&orgUnit=org",
                         dataType: "json",
                         cache: false
                     }
                 },
                 aggregate: [{ field: "distanceFromHomeToBorder", aggregate: "sum" }],
                 schema: {
                     model: {
                         fields: {
                             distanceFromHomeToBorder: { type: "number" }
                         }
                     }
                 }
             }, 
             resizable: true,
         

             columns: [
                         {
                             field: "DriveDateTimestamp", template: function (data) {
                                 var m = moment.unix(data.DriveDateTimestamp);
                                 return m._d.getDate() + "/" +
                                     (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                     m._d.getFullYear();
                             }, title: "Dato for kørsel", width:100
                         },
                         {
                             field: "CreatedDateTimestamp", template: function (data) {
                                 var m = moment.unix(data.CreatedDateTimestamp);
                                 return m._d.getDate() + "/" +
                                     (m._d.getMonth() + 1) + "/" + // +1 because getMonth is zero indexed.
                                     m._d.getFullYear();
                             }, title: "Dato for indberetning",
                         },
                         {field: "OrgUnit", title: "Org. Enhed", width: '100'},
                         { field: "Purpose", title: "Formål", },
                         { field: "Route", title: "Rute", },
                         { field: "IsExtraDistance", title: "Merkørselsangivelse", },
                         { field: "FourKmRule", title: "4-km", },
                         { field: "distanceFromHomeToBorder", title: "km til kommunegrænse", footerTemplate: "Samlet: #= sum # "},
                         { field: "AmountToReimburse", title: "km til udbetaling", },
                         { field: "ApprovedBy", title: "Godkendt af", }
             ], scrollable:false
         };



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
             /*optional stylesheet*/ //mywindow.document.write('<link rel="stylesheet" href="main.css" type="text/css" />');
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
         };
         $scope.saveAsPdf = function () {
             alert('saveAsPdf Clicked :-)')
         };
         $scope.downloadAsExcel = function () {
             alert('downloadAsExcel Clicked :-)')
         };
    }]);
