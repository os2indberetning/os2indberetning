angular.module("application").controller('RouteEditModalInstanceController', [
    "$scope", "Route", "Point", "NotificationService", "$modalInstance", "routeId", "personId", "Address", "AddressFormatter", "SmartAdresseSource", function ($scope, Route, Point, NotificationService, $modalInstance, routeId, personId, Address, AddressFormatter, SmartAdresseSource) {


        //Contains addresses as strings ex. "Road 1, 8220 Aarhus"
        $scope.viaPointModels = [];

        $scope.isSaveDisabled = false;
        $scope.canSubmitRoute = false;

        $scope.addressFieldOptions = {
            dataBound: function () {
                $scope.addressNotFound = this.dataSource._data.length == 0;
                $scope.$apply();
            }
        }

        if (routeId != undefined) {
            Route.getSingle({ id: routeId }, function (res) {
                $scope.newRouteDescription = res.Description;

                $scope.newStartPoint = res.Points[0].StreetName + " " + res.Points[0].StreetNumber + ", " + res.Points[0].ZipCode + " " + res.Points[0].Town;
                $scope.newEndPoint = res.Points[res.Points.length - 1].StreetName + " " + res.Points[res.Points.length - 1].StreetNumber + ", " + res.Points[res.Points.length - 1].ZipCode + " " + res.Points[res.Points.length - 1].Town;

                angular.forEach(res.Points, function (viaPoint, key) {
                    if (key != 0 && key != res.Points.length - 1) {
                        // If its not the first or last element -> Its a via point
                        var pointModel = viaPoint.StreetName + " " + viaPoint.StreetNumber + ", " + viaPoint.ZipCode + " " + viaPoint.Town;
                        $scope.viaPointModels.push(pointModel);
                    }
                });
            });
        }

        var validateAddressFormatted = function (formattedAddress) {
            if (formattedAddress == undefined || formattedAddress == "") {
                return false;
            }

            if (formattedAddress.StreetName == undefined || formattedAddress.StreetName == "") {
                return false;
            }

            if (formattedAddress.StreetNumber == undefined || formattedAddress.StreetNumber == "") {
                return false;
            }

            if (formattedAddress.ZipCode == undefined || formattedAddress.ZipCode == "" || formattedAddress.ZipCode.toString().length != 4) {
                return false;
            }

            if (formattedAddress.Town == undefined || formattedAddress.Town == "") {
                return false;
            }

            return true;
        }

        var validateAddress = function (addr) {            
            if (addr == undefined) {
                return false;
            }

            if ($scope.addressSelectionErrorMessage.length > 0) {
                return false;
            }

            return true;
        }

        var validateInput = function () {
            if (!validateAddress($scope.newStartPoint)) {
                return false;
            }

            if (!validateAddress($scope.newEndPoint)) {
                return false;
            }
            
            angular.forEach($scope.viaPointModels, function (addr, key) {
                if (!validateAddress(addr)) {
                    return false;
                }
            });
            return true;
        }

        $scope.saveRoute = function () {
            if (!validateInput()) { 
                return;
            }
            
            $scope.isSaveDisabled = true;
            if (routeId != undefined) {
                // routeId is defined -> User is editing existing route ->  Delete it, and then post the edited route as a new route.
                Route.delete({ id: routeId }, function () {
                    handleSaveRoute();
                });
            } else {
                // routeId is undefined -> User is making a new route.
                handleSaveRoute();
            }

        }

        var handleSaveRoute = function () {
            // Validate start and end point
            if ($scope.newStartPoint == undefined || $scope.newStartPoint == "" || $scope.newEndPoint == undefined || $scope.newEndPoint == "") {
                NotificationService.AutoFadeNotification("danger", "", "Start- og slutadresse skal udfyldes.");
                $scope.isSaveDisabled = false;
                return;
            }

            // Validate description
            if ($scope.newRouteDescription == "" || $scope.newRouteDescription == undefined) {
                NotificationService.AutoFadeNotification("danger", "", "Beskrivelse må ikke være tom.");
                $scope.isSaveDisabled = false;
                return;
            }

            var points = [];

            var startAddress = AddressFormatter.fn($scope.newStartPoint);

            points.push({
                "StreetName": startAddress.StreetName,
                "StreetNumber": startAddress.StreetNumber,
                "ZipCode": startAddress.ZipCode,
                "Town": startAddress.Town,
                "Latitude": "",
                "Longitude": "",
                "Description": ""
            });
            angular.forEach($scope.viaPointModels, function (address, key) {
                var point = AddressFormatter.fn(address);

                points.push({
                    "StreetName": point.StreetName,
                    "StreetNumber": point.StreetNumber,
                    "ZipCode": point.ZipCode,
                    "Town": point.Town,
                    "Latitude": "",
                    "Longitude": "",
                    "Description": ""
                });
            });

            var endAddress = AddressFormatter.fn($scope.newEndPoint);

            points.push({
                "StreetName": endAddress.StreetName,
                "StreetNumber": endAddress.StreetNumber,
                "ZipCode": endAddress.ZipCode,
                "Town": endAddress.Town,
                "Latitude": "",
                "Longitude": "",
                "Description": ""
            });

            Route.post({
                "Description": $scope.newRouteDescription,
                "PersonId": personId,
                "Points": points
            }, function () {
                if (routeId != undefined) {
                    NotificationService.AutoFadeNotification("success", "", "Personlig rute blev redigeret.");
                } else {
                    NotificationService.AutoFadeNotification("success", "", "Personlig rute blev oprettet.");
                }
                $modalInstance.close();
            });
        }

        $scope.onAddressBlurTriggered = function (addr) {
            if (addr == undefined) {
                return false;
            }

            if ($scope.addressNotFound) {
                $scope.addressSelectionErrorMessage = "Adressen " + addr + " er ikke valid, vælg fra drop down";
                return false;
            }

            // Format all addresses and add them to postRequest
            var formattedAddress = AddressFormatter.fn(addr);

            // validate formattedAddress
            if (formattedAddress == undefined || !validateAddressFormatted(formattedAddress)) {
                $scope.addressSelectionErrorMessage = "Adressen " + addr + " er ikke valid, vælg fra drop down";
                return false;
            } else {
                Address.setCoordinatesOnAddress({
                    StreetName: formattedAddress.StreetName,
                    StreetNumber: formattedAddress.StreetNumber,
                    ZipCode: formattedAddress.ZipCode,
                    Town: formattedAddress.Town
                }).$promise.then(function (data) {
                    $scope.addressSelectionErrorMessage = "";
                    
                }, function (reason) {

                    $scope.addressSelectionErrorMessage = "Adressen " + addr + " er ikke valid, vælg fra drop down";
                });
            }
        }

        $scope.onStartAddressBlurTriggered = function () {
            $scope.onAddressBlurTriggered($scope.newStartPoint);
        }

        $scope.onEndAddressBlurTriggered = function() {
            $scope.onAddressBlurTriggered($scope.newEndPoint);
        }

        $scope.onViaPointAddressBlurTriggered = function ($index) {
            $scope.onAddressBlurTriggered($scope.viaPointModels[index]);
        }

        $scope.removeViaPoint = function ($index) {
           $scope.viaPointModels.splice($index, 1);
        }

        $scope.addNewViaPoint = function () {
            $scope.viaPointModels.push("");
        }

        $scope.closeRouteEditModal = function () {
            $modalInstance.dismiss();
        };

        $scope.SmartAddress = SmartAdresseSource;
    }]);