angular.module("application").controller("AddEditFileGenerationScheduleController", [
    "$scope", "$modalInstance", "NotificationService", "StandardAddress", "AddressFormatter", "SmartAdresseSource", "FileGenerationSchedule", "itemId",
    function ($scope, $modalInstance, NotificationService, StandardAddress, AddressFormatter, SmartAdresseSource, FileGenerationSchedule, itemId) {
        
        $scope.FileGenerationSchedule = {};
        $scope.FileGenerationSchedule.MailNotificationSchedules = [];
        $scope.FileGenerationSchedule.DateTimestamp = new Date();
        $scope.FileGenerationSchedule.Repeat = "false";
        $scope.ShowTextareaValues = [];
        $scope.DeletedMailsIds = [];
        $scope.ModalResult = {
            "FileGenerationSchedule" : {},
            "DeletedMailsIds" : []
        };
        
        if(itemId > 0){
            // FileGenerationSchedule is being edited
            FileGenerationSchedule.getWithEmailNotifications({ id: itemId }).$promise.then(function (res) {
                $scope.FileGenerationSchedule = res;

                $scope.FileGenerationSchedule.DateTimestamp = new Date(moment.unix($scope.FileGenerationSchedule.DateTimestamp).format("YYYY-MM-DD"));

                angular.forEach($scope.FileGenerationSchedule.MailNotificationSchedules, function(mailnotif, key){
                    mailnotif.DateTimestamp = new Date(moment.unix(mailnotif.DateTimestamp).format("YYYY-MM-DD"));
                })
                

                if($scope.FileGenerationSchedule.Repeat == true){
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
                $scope.ModalResult.FileGenerationSchedule = $scope.FileGenerationSchedule;
                $modalInstance.close($scope.ModalResult);
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
            $scope.FileGenerationSchedule.MailNotificationSchedules.push({DateTimestamp: new Date(), CustomText:""});
            $scope.ShowTextareaValues.push(false);         
        }

        $scope.RemoveMailNotificationSchedule = function(index){
            if(index > -1){
                if($scope.FileGenerationSchedule.MailNotificationSchedules[index].Id > 0) {
                    $scope.ModalResult.DeletedMailsIds.push($scope.FileGenerationSchedule.MailNotificationSchedules[index].Id);
                }
                $scope.FileGenerationSchedule.MailNotificationSchedules.splice(index, 1);
                $scope.ShowTextareaValues.splice(index, 1);
            }
        }

        $scope.EnableTextarea = function(index){
            $scope.ShowTextareaValues[index] = !$scope.ShowTextareaValues[index];
        }

        $scope.TextareaEnter = function(index){
            $scope.FileGenerationSchedule.MailNotificationSchedules[index].CustomText += "\n";
        }
    }
]);