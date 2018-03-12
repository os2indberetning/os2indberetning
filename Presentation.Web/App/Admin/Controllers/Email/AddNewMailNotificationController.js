angular.module("application").controller("AddNewMailNotificationController", [
    "$scope", "$modalInstance", "NotificationService", "StandardAddress", "AddressFormatter", "SmartAdresseSource",
    function ($scope, $modalInstance, NotificationService, StandardAddress, AddressFormatter, SmartAdresseSource) {

        $scope.FileGenerationSchedule = {};
        $scope.FileGenerationSchedule.MailNotificationSchedules = [];
        $scope.Repeat = "";

        $scope.dateOptions = {
            format: "dd/MM/yyyy"
        };

        $scope.payrollDate = new Date();

        $scope.confirmSave = function () {
            /// <summary>
            /// Saves new MailNotification if fields are properly filled.
            /// </summary>
            var error = false;

            $scope.repeatErrorMessage = "";
            if ($scope.Repeat == "") {
                error = true;
                $scope.repeatErrorMessage = "* Du skal udfylde 'Gentag månedligt'.";
            }

            $scope.payDateErrorMessage = "";
            if ($scope.payrollDate == undefined) {
                error = true;
                $scope.payDateErrorMessage = "* Du skal vælge en gyldig lønkørselsdato.";
            }

            var result = {};
            if ($scope.Repeat == "true") {
                $scope.FileGenerationSchedule.Repeat = true;
            } else {
                $scope.FileGenerationSchedule.Repeat = false;
            }

            angular.forEach($scope.FileGenerationSchedule.MailNotificationSchedules, function(mailnotif, key){
                mailnotif.DateTimestamp = moment(mailnotif.DateTimestamp).unix();
            })

            $scope.FileGenerationSchedule.DateTimestamp = moment($scope.payrollDate).unix();
            if (!error) {
                $modalInstance.close($scope.FileGenerationSchedule);
                NotificationService.AutoFadeNotification("success", "", "Lønkørslen blev oprettet.");
            }

        }

        $scope.cancel = function () {
            /// <summary>
            /// Cancels creation of new MailNotification.
            /// </summary>
            $modalInstance.dismiss('cancel');
            NotificationService.AutoFadeNotification("warning", "", "Oprettelse af lønkørslen blev annulleret.");
        }

        $scope.AddMailNotificationSchedule = function(){
            $scope.FileGenerationSchedule.MailNotificationSchedules.push({DateTimestamp:"", CustomText:""})
        }

        $scope.RemoveMailNotificationSchedule = function(index){
            if(index > -1){
                $scope.FileGenerationSchedule.MailNotificationSchedules.splice(index, 1);
            }
        }
    }
]);