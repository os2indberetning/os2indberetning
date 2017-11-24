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
               $window.location.href = "login.ashx"
           });
       }

    }
]);