angular.module("application").controller("ReportController", [
    "$scope", "$rootScope","$window","$state",
    function ($scope, $rootScope,$window,$state) {

        $scope.container = {};


        $scope.createReportClick = function () {
            //$window.open('/eksport/index?Employee=' + $scope.container.employeeFilter + '&manr=' + $scope.container.MANrFilter + '&from= ' + $scope.container.reportFromDateString + '&to=' + $scope.container.reportToDateString, '_blank');
            //$window.open('app/admin/html/report/DocumentView.html');

            var url = $state.href('document', { "Employee": 'parametre' });

$scope.$broadcast('createReportClicked');

            $window.open(url + '?Employee=' + $scope.container.employeeFilter + '&manr=' + $scope.container.MANrFilter + '&from= ' + $scope.container.reportFromDateString + '&to=' + $scope.container.reportToDateString, '_blank');

            //$window.open('/Document');
        }


    }
]);
