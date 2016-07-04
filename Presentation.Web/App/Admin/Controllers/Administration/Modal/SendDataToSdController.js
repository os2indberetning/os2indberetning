angular.module("application").controller("SendDataToSdController", [
   "$scope", "$modalInstance","$http", function ($scope, $modalInstance, $http) {


       $scope.confirmGenerateFile = function () {
           /// <summary>
           /// Send data to SD
           /// </summary>

           $http.get("/odata/DriveReports/Service.sendDataToSd").success(function (data) {
               
           }).error(function (error) {
               
           });

           alert('confirmed')
       }

       $scope.cancelGenerateFile = function () {
           /// <summary>
           /// Cancel generate KMD file.
           /// </summary>
           $modalInstance.dismiss('cancel');
       }

   }
]);