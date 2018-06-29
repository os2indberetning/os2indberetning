angular.module("application").controller("MainMenuController", [
   "$scope", "$window", "Person", "PersonalAddress", "HelpText", "$rootScope", "OrgUnit", function ($scope, $window, Person, PersonalAddress, HelpText, $rootScope, OrgUnit) {


       HelpText.getAll().$promise.then(function (res) {
           $scope.helpLink = res.InformationHelpLink;
           $rootScope.HelpTexts = res;
       });

       if ($rootScope.CurrentUser == undefined) {
           $rootScope.CurrentUser = Person.GetCurrentUser().$promise.then(function (res) {
               $rootScope.CurrentUser = res;
               $scope.showAdministration = res.IsAdmin;
               $scope.showApproveReports = res.IsLeader || res.IsSubstitute;
               $scope.UserName = res.FullName;
           }).catch(function(e){
               if ($rootScope.HelpTexts.AUTHENTICATION.text == "SAML") {
                var msg = e.data.error.innererror.message;
                if (msg == "Gyldig domænebruger ikke fundet." || msg == "AD-bruger ikke fundet i databasen." || msg == "Inaktiv bruger forsøgte at logge ind.")   {
                    return; // TDOD: such an ugly solution, needs to be refactored. Consider using exception types instead of comparing messages.
                }
                $window.location.href = "login.ashx"
               }
           });
       }

    }
]);