angular.module("application").controller("ConfirmRejectApprovedReportModalController", [
   "$scope", "$modalInstance", "DriveReport", "$rootScope", function ($scope, $modalInstance, DriveReport, $rootScope) {


   $scope.confirmClicked = function(){
        $modalInstance.close($scope.emailText);
   }

   $scope.cancelClicked = function(){
        $modalInstance.dismiss();
   }

   }
]);


