angular.module("application").controller("DeletFileGenerationScheduleController", [
    "$scope", "$modalInstance", "itemId", "NotificationService",
    function ($scope, $modalInstance, itemId, NotificationService) {

        $scope.confirmDelete = function () {
            /// <summary>
            /// Confirms deletion of FileGenerationSchedule
            /// </summary>
            $modalInstance.close($scope.itemId);
            NotificationService.AutoFadeNotification("success", "", "Lønkørslen blev slettet.");
        }
        


        $scope.cancel = function () {
            /// <summary>
            /// Cancels deletion of FilegenerationSchedule
            /// </summary>
            $modalInstance.dismiss('cancel');
            NotificationService.AutoFadeNotification("warning", "", "Sletning af lønkørslen blev annulleret.");
        }
    }
]);