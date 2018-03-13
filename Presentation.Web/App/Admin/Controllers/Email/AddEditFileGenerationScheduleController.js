angular.module("application").controller("AddEditFileGenerationScheduleController", [
    "$scope", "$modalInstance", "NotificationService", "StandardAddress", "AddressFormatter", "SmartAdresseSource", "FileGenerationSchedule", "itemId",
    function ($scope, $modalInstance, NotificationService, StandardAddress, AddressFormatter, SmartAdresseSource, FileGenerationSchedule, itemId) {

        $scope.FileGenerationSchedule = {};
        $scope.FileGenerationSchedule.MailNotificationSchedules = [];
        $scope.FileGenerationSchedule.DateTimestamp = new Date();
        $scope.FileGenerationSchedule.Repeat = "";
        
        if(itemId > 0){
            // FileGenerationSchedule is being edited
            FileGenerationSchedule.getWithEmailNotifications({ id: itemId }).$promise.then(function (res) {
                $scope.FileGenerationSchedule = res;

                $scope.FileGenerationSchedule.DateTimestamp = moment.unix($scope.FileGenerationSchedule.DateTimestamp).format("DD/MM/YYYY");

                angular.forEach($scope.FileGenerationSchedule.MailNotificationSchedules, function(mailnotif, key){
                    mailnotif.DateTimestamp = moment.unix(mailnotif.DateTimestamp).format("DD/MM/YYYY");
                })

                if($scope.FileGenerationSchedule == true){
                    $scope.FileGenerationSchedule.Repeat = "true";
                } else {
                    $scope.FileGenerationSchedule.Repeat = "false";
                }
            });
        }

        $scope.dateOptions = {
            format: "dd/MM/yyyy"
        };

        $scope.confirmSave = function () {
            /// <summary>
            /// Saves new MailNotification if fields are properly filled.
            /// </summary>
            var error = false;

            $scope.repeatErrorMessage = "";
            if ($scope.FileGenerationSchedule.Repeat == "") {
                error = true;
                $scope.repeatErrorMessage = "* Du skal udfylde 'Gentag månedligt'.";
            }

            $scope.payDateErrorMessage = "";
            if ($scope.FileGenerationSchedule.DateTimestamp == undefined) {
                error = true;
                $scope.payDateErrorMessage = "* Du skal vælge en gyldig lønkørselsdato.";
            }

            var result = {};
            if ($scope.FileGenerationSchedule.Repeat == "true") {
                $scope.FileGenerationSchedule.Repeat = true;
            } else {
                $scope.FileGenerationSchedule.Repeat = false;
            }

            angular.forEach($scope.FileGenerationSchedule.MailNotificationSchedules, function(mailnotif, key){
                mailnotif.DateTimestamp = moment(mailnotif.DateTimestamp).unix();
            })

            $scope.FileGenerationSchedule.DateTimestamp = moment($scope.FileGenerationSchedule.DateTimestamp).unix();
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