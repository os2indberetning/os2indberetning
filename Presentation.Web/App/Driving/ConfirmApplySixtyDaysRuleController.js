angular.module("application").controller("ConfirmApplySixtyDaysRuleController", [
    "$scope", "$modalInstance", "sixtyDaysRuleHelptext",
    function ($scope, $modalInstance, sixtyDaysRuleHelptext) {
        
        $scope.sixtyDaysRuleHelptext = sixtyDaysRuleHelptext;
 
        $scope.confirmApplySixtyDaysRule = function () {
            $modalInstance.close();
        }

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        }
 
    }
 ]);