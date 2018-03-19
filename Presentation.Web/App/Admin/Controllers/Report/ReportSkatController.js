angular.module('application').controller('ReportSkatController', [
    "$scope", "$rootScope", "$window", "$state", "Person", "Autocomplete", "OrgUnit",
    function ($scope, $rootScope, $window, $state, Person, Autocomplete, OrgUnit) {

        $scope.container = {};
        $scope.persons = Autocomplete.allUsers();
        $scope.orgUnits = Autocomplete.orgUnits();

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

            $scope.container.chosenPersonId = "";
            $scope.container.chosenOrgunitId = "";

            var url = $state.href('document');

            $scope.$broadcast('createReportClicked');

            if ($scope.container.employeeFilter != undefined && $scope.container.reportFromDateString != undefined && $scope.container.reportToDateString != undefined) {

                $window.open(url + '?Employee=' + personId + '&from= ' + $scope.container.reportFromDateString + '&to=' + $scope.container.reportToDateString + "&orgUnit=" + orgunitId + "&reportType=0", '_blank');
            } else {

                alert('Du mangler at udfylde et felt med en *');
            }

        }


    }
]);
