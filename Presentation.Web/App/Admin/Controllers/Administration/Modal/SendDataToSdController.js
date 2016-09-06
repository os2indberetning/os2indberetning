angular.module("application").controller("SendDataToSdController", [
   "$scope", "$modalInstance", function ($scope, $modalInstance) {

 
       $scope.confirmSendData = function () {
           /// <summary>
           /// Confirm Generate KMD file
           /// </summary>
           $modalInstance.close();
       }

       $scope.cancelSendData = function () {
           /// <summary>
           /// Cancel generate KMD file.
           /// </summary>
           $modalInstance.dismiss('cancel');
       }

   }
]);