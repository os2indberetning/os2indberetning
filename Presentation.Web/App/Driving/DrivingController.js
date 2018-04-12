﻿angular.module("application").controller("DrivingController", [
    "$scope", "Person", "PersonEmployments", "Rate", "LicensePlate", "PersonalRoute", "DriveReport", "Address", "SmartAdresseSource", "AddressFormatter", "$q", "$filter", "ReportId", "$timeout", "NotificationService", "PersonalAddress", "$rootScope", "$modalInstance", "$window", "$modal", "$location", "adminEditCurrentUser",
    function ($scope, Person, PersonEmployments, Rate, LicensePlate, PersonalRoute, DriveReport, Address, SmartAdresseSource, AddressFormatter, $q, $filter, ReportId, $timeout, NotificationService, PersonalAddress, $rootScope, $modalInstance, $window, $modal, $location, adminEditCurrentUser) {

        $scope.ReadReportCommentHelp = $rootScope.HelpTexts.ReadReportCommentHelp.text;
        $scope.PurposeHelpText = $rootScope.HelpTexts.PurposeHelpText.text;
        $scope.fourKmRuleHelpText = $rootScope.HelpTexts.FourKmRuleHelpText.text;
        $scope.noLicensePlateHelpText = $rootScope.HelpTexts.NoLicensePlateHelpText.text;
        $scope.sixtyDaysRuleHelptext = $rootScope.HelpTexts.SixtyDaysRuleHelpText.text;



        // Setup functions in scope.
        $scope.Number = Number;
        $scope.toString = toString;
        $scope.replace = String.replace;

        $scope.saveBtnDisabled = false;

        var isFormDirty = false;

        var fourKmAdjustment = 4;

        // Coordinate threshold is the amount two gps coordinates can differ and still be considered the same address.
        // Third decimal is 100 meters, so 0.001 means that addresses within 100 meters of each other will be considered the same when checking if route starts or ends at home.
        var coordinateThreshold = 0.001;


        var isEditingReport = ReportId > 0;
        $scope.container = {};
        $scope.container.addressNotFound = false;
        $scope.isEditingReport = isEditingReport;
        var kendoPromise = $q.defer();
        var loadingPromises = [kendoPromise.promise];

        //Set Alternative calculation
        $scope.buildDataSource = new kendo.data.DataSource();
        $scope.kilometerOptions = {
            dataSource: $scope.buildDataSource,
            dataTextField: "key",
            dataValueField: "value"
        };

        DriveReport.getCalculationMethod().$promise.then(function (res) {
            //Used for switch calculation profile between (ndk = Nordjurskommune, "" = standard)
            //$scope.alternativeCalculation = res.value;
            $scope.alternativeCalculationKey = res.value;
            if (res.value === "") {
                $scope.alternativeCalculation = false;
            } else {
                $scope.alternativeCalculation = true;
            }
            //
            if (!$scope.alternativeCalculation) {
                $scope.buildDataSource.data([
                    { value: "Calculated", key: "Beregnet" },
                    { value: "Read", key: "Aflæst" },
                    { value: "CalculatedWithoutExtraDistance", key: "Beregnet uden merkørsel" }
                ]);
                //Set calculation specific text
                $scope.alternativeCalculationTextReimbursement = "Merkørselsfradrag";
            } else {
                if ($scope.alternativeCalculationKey === "ndk") {
                    $scope.buildDataSource.data([
                        { value: "Calculated", key: "Beregnet" },
                        { value: "Read", key: "Aflæst" },
                    ]);
                    //Set calculation specific text
                    $scope.alternativeCalculationTextReimbursement = "Fradrag";
                    $scope.AlternativeCalculationTextDistanceForReport = " (Kan højst svare til hvis tjenesterejsen var påbegyndt og afsluttet på det faste tjenestested)";
                }
            }
        });


        $scope.canSubmitDriveReport = true;

        var mapChanging = false;

        // Is true the first time the map is loaded to prevent filling the address textboxes with the mapstart addresses.
        // Is initially false when loading a report to edit.
        var firstMapLoad = true;


        $scope.container.addressFieldOptions = {
            select: function () {
                $timeout(function () {
                    $scope.addressInputChanged();
                });
            },
            dataBound: function () {
                $scope.container.addressNotFound = this.dataSource._data.length == 0;
                $scope.$apply();
            },
        }

        $scope.addressPlaceholderText = "Indtast adresse her";
        $scope.SmartAddress = SmartAdresseSource;
        $scope.IsRoute = false;

        // Is set to actually contain something once data has been loaded from backend.
        $scope.validateInput = function () { };

        var setupForNewReport = function () {
            /// <summary>
            /// Initializes fields for new report.
            /// </summary>
            $scope.DriveReport = new DriveReport();

            console.log("name: " + $rootScope.CurrentUser.id);

            $scope.DriveReport.KilometerAllowance = $rootScope.CurrentUser.Employments[0].OrgUnit.DefaultKilometerAllowance;

            $scope.DriveReport.Addresses = [];
            $scope.DriveReport.Addresses.push({ Name: "", Personal: "" });
            $scope.DriveReport.Addresses.push({ Name: "", Personal: "" });
            $scope.container.datePickerMaxDate = new Date();
        }

        setupForNewReport();

        $scope.AddViapoint = function () {
            /// <summary>
            /// Adds via point
            /// </summary>
            var temp = $scope.DriveReport.Addresses.pop();
            $scope.DriveReport.Addresses.push({ Name: "", Personal: "", Save: false });
            $scope.DriveReport.Addresses.push(temp);
        };

        $scope.Remove = function (array, index) {
            /// <summary>
            /// Removes via point
            /// </summary>
            /// <param name="array"></param>
            /// <param name="index"></param>
            array.splice(index, 1);
            $scope.addressInputChanged(index);
        };

        var getKmRate = function () {
            for (var i = 0; i < $scope.KmRate.length; i++) {
                if ($scope.KmRate[i].Type.Id == $scope.DriveReport.KmRate) {
                    return $scope.KmRate[i];
                }
            }
        };

        $scope.shaveExtraCommasOffAddressString = function (address) {
            /// <summary>
            /// Removes commas from Address string from Septima.
            /// Septima addresses are in the format                 "StreetName StreetNumber, ZipCode, Town"
            /// Addresses used in the app need to be in the format  "StreetName StreetNumber, Zipcode Town"
            /// </summary>
            /// <param name="address"></param>
            var res = address.toString().replace(/,/, "###");
            res = res.replace(/,/g, "");
            res = res.replace(/###/, ",");
            return res;
        }

        var getCurrentUserEmployment = function (employmentId) {
            /// <summary>
            /// Gets employment for current user.
            /// </summary>
            /// <param name="employmentId"></param>
            var res;
            angular.forEach($scope.currentUser.Employments, function (empl, key) {
                if (empl.Id == employmentId) {
                    res = empl;
                }
            });
            return res;
        }

        var loadValuesFromReport = function (report) {
            /// <summary>
            /// Loads values from user's latest report and sets fields in the view.
            /// </summary>
            /// <param name="report"></param>
            $scope.DriveReport.FourKmRule = {};
            $scope.DriveReport.FourKmRule.Value = $scope.currentUser.DistanceFromHomeToBorder.toString().replace(".", ",");
            
            if(report.EmploymentId != null){
             // Set default DriveReport Position to position from previous report
            $scope.DriveReport.Position = report.EmploymentId;
            }
          
            // Select position in dropdown.
            $scope.container.PositionDropDown.select(function (item) {
                return item.Id == report.EmploymentId;
            });


            // Select the right license plate.
            $scope.container.LicensePlateDropDown.select(function (item) {
                return item.Plate == report.LicensePlate;
            });
            $scope.container.LicensePlateDropDown.trigger("change");

            // Select KmRate

            $scope.container.KmRateDropDown.select(function (item) {
                return item.Type.TFCode == report.TFCode;
            });

            angular.forEach($scope.KmRate, function (rate, key) {
                if (rate.Type.TFCode == report.TFCode) {
                    $scope.showLicensePlate = rate.Type.RequiresLicensePlate;
                }
            });

            $scope.container.KmRateDropDown.trigger("change");


            // Load additional data if a report is being edited.
            if (isEditingReport) {

                // Select kilometer allowance.
                switch (report.KilometerAllowance) {
                    case "Calculated":
                        $scope.container.KilometerAllowanceDropDown.select(0);

                        if (report.IsFromApp) {
                            //Notify user that editing a calculated report from app has special conditions.
                            var modalInstance = $modal.open({
                                templateUrl: '/App/Driving/EditCalculatedAppReportTemplate.html',
                                controller: 'NoLicensePlateModalController',
                                backdrop: "static",
                            });
                        }

                        break;
                    case "Read":
                        $scope.container.KilometerAllowanceDropDown.select(1);
                        break;
                    case "CalculatedWithoutExtraDistance":
                        $scope.container.KilometerAllowanceDropDown.select(2);
                        break;
                }

                $scope.DriveReport.KilometerAllowance = $scope.container.KilometerAllowanceDropDown._selectedValue;

                $scope.DriveReport.Purpose = report.Purpose;
                $scope.DriveReport.Status = report.Status;
                $scope.DriveReport.FourKmRule.Using = report.FourKmRule;
                $scope.DriveReport.FourKmRule.Deducted = report.FourKmRuleDeducted;
                $scope.DriveReport.SixtyDaysRule = report.SixtyDaysRule;
                $scope.DriveReport.Date = moment.unix(report.DriveDateTimestamp)._d;

                if (report.KilometerAllowance == "Read") {
                    firstMapLoad = false;

                    $scope.DriveReport.UserComment = report.UserComment;
                    if (!report.StartsAtHome && !report.EndsAtHome) {
                        $scope.container.StartEndHomeDropDown.select(0);
                        $scope.DriveReport.StartOrEndedAtHome = "Neither";
                    } else if (report.StartsAtHome && report.EndsAtHome) {
                        $scope.container.StartEndHomeDropDown.select(3);
                        $scope.DriveReport.StartOrEndedAtHome = "Both";
                    } else if (report.StartsAtHome) {
                        $scope.container.StartEndHomeDropDown.select(1);
                        $scope.DriveReport.StartOrEndedAtHome = "Started";
                    } else if (report.EndsAtHome) {
                        $scope.container.StartEndHomeDropDown.select(2);
                        $scope.DriveReport.StartOrEndedAtHome = "Ended";
                    }
                    $scope.DriveReport.StartsAtHome = report.StartsAtHome;
                    $scope.DriveReport.EndsAtHome = report.EndsAtHome;
                    updateDrivenKm();
                    // The distance value saved on a drivereport is the distance after subtracting transport allowance.
                    // Therefore it is needed to add the transport allowance back on to the distance when editing it.
                    if(report.FourKmRule){
                        report.Distance = Number(report.Distance) + $scope.DriveReport.FourKmRule.Deducted;
                    }
                    report.Distance = (report.Distance + $scope.TransportAllowance).toFixed(1);
                    if (report.IsRoundTrip) {
                        report.Distance = (Number(report.Distance) + $scope.TransportAllowance) / 2; // add transportallowance again becasue of roundtrip.
                    }
                    $scope.DriveReport.ReadDistance = Number(report.Distance).toFixed(1).replace(".", ",");
                } else {
                    $scope.initialEditReportLoad = true;
                    $scope.DriveReport.Addresses = [];
                    mapChanging = true;
                    angular.forEach(report.DriveReportPoints, function (point, key) {
                        var temp = { Name: point.StreetName + " " + point.StreetNumber + ", " + point.ZipCode + " " + point.Town, Latitude: point.Latitude, Longitude: point.Longitude };
                        $scope.DriveReport.Addresses.push(temp);
                    });
                    var res = "[";
                    angular.forEach($scope.DriveReport.Addresses, function (addr, key) {
                        res += "{name: \"" + addr.Name + "\", lat: " + addr.Latitude + ", lng: " + addr.Longitude + "},";
                    });
                    res += "]";

                    $scope.$on("kendoWidgetCreated", function (event, widget) {
                        if (widget === $scope.container.lastTextBox) {
                            mapChanging = false;
                            firstMapLoad = false;
                            $scope.addressInputChanged();
                        }
                    });

                }

                $scope.DriveReport.IsRoundTrip = report.IsRoundTrip;
            }

        }


        if (adminEditCurrentUser != 0) {
            // adminEditCurrentUser will have a value different from 0 if an admin is currently trying to edit a report.
            $scope.currentUser = adminEditCurrentUser;
        } else {
            $scope.currentUser = $rootScope.CurrentUser;
        }

        // Load all data
        var currentUser = $scope.currentUser;
        // Load user's positions.
        angular.forEach(currentUser.Employments, function (value, key) {
            value.PresentationString = value.Position + " - " + value.OrgUnit.LongDescription + " (" + value.EmploymentId + ")";
        });
        $scope.Employments = currentUser.Employments;

        // Load rates.
        loadingPromises.push(Rate.ThisYearsRates().$promise.then(function (res) {
            $scope.KmRate = res;

            // create array with a single set of rates for the dropdown, since we only need the TF codes' description for this.
            var tempRates = [];
            var driveYear = new Date().getFullYear();
            var j = 0;
            for (var i = 0; i < $scope.KmRate.length; i++) {
                if ($scope.KmRate[i].Year == driveYear) {
                    tempRates[j] = $scope.KmRate[i];
                    j++;
                }
            }
            $scope.KmRateView = tempRates
        }));

        // Load user's license plates.
        var plates = currentUser.LicensePlates.slice(0);
        if (plates.length > 0) {
            $scope.userHasLicensePlate = true;
            angular.forEach(plates, function (value, key) {
                if (value.Description != "") {
                    value.PresentationString = value.Plate + " - " + value.Description;
                } else {
                    value.PresentationString = value.Plate;
                }
            });
            $scope.LicensePlates = plates;
        } else {
            $scope.userHasLicensePlate = false;
            $scope.LicensePlates = [{ PresentationString: "Ingen nummerplader", Plate: "0000000" }];
        }

        // Load user's personal routes
        var routes = currentUser.PersonalRoutes.slice(0);
        angular.forEach(routes, function (value, key) {
            value.PresentationString = "";
            if (value.Description != "") {
                value.PresentationString += value.Description + " : ";
            }
            value.PresentationString += value.Points[0].StreetName + " " + value.Points[0].StreetNumber + ", " + value.Points[0].ZipCode + " " + value.Points[0].Town + " -> ";
            value.PresentationString += value.Points[value.Points.length - 1].StreetName + " " + value.Points[value.Points.length - 1].StreetNumber + ", " + value.Points[value.Points.length - 1].ZipCode + " " + value.Points[value.Points.length - 1].Town;
            value.PresentationString += " Antal viapunkter: " + Number(value.Points.length - 2);
        });
        routes.unshift({ PresentationString: "Vælg personlig rute" });
        $scope.Routes = routes;

        // Load map start address
        loadingPromises.push(Address.getMapStart().$promise.then(function (res) {
            $scope.mapStartAddress = res;
        }));

        if (!isEditingReport) {
            // Load latest drive report
            loadingPromises.push(DriveReport.getLatest({ id: currentUser.Id }).$promise.then(function (res) {
                $scope.latestDriveReport = res;
            }));
        } else {
            // Load report to be edited.
            loadingPromises.push(DriveReport.getWithPoints({ id: ReportId }).$promise.then(function (res) {
                $scope.latestDriveReport = res;
            }));
        }

        // Load personal and standard addresses.
        loadingPromises.push(Address.GetPersonalAndStandard({ personId: currentUser.Id }).$promise.then(function (res) {
            angular.forEach(res, function (value, key) {
                value.PresentationString = "";
                if (value.Description != "" && value.Description != null && value.Description != undefined) {
                    value.PresentationString += value.Description + " : ";
                }
                if (value.Type == "Home") {
                    // Store home address
                    $scope.HomeAddress = value;
                    value.PresentationString = "Hjemmeadresse : ";
                }
                if (value.Type == "AlternativeHome") {
                    // Overwrite home address if user has alternative home address.
                    $scope.HomeAddress = value;
                }

                value.PresentationString += value.StreetName + " " + value.StreetNumber + ", " + value.ZipCode + " " + value.Town;
                value.address = value.StreetName + " " + value.StreetNumber + ", " + value.ZipCode + " " + value.Town;
            });

            $scope.PersonalAddresses = new kendo.data.DataSource({
                data: res,
                sort: {
                    field: "PresentationString",
                    dir: "asc"
                }
            });

        }));


        $q.all(loadingPromises).then(function (res) {
            dataAndKendoLoaded();
        });

        var setNotRoute = function (resetMap) {
            /// <summary>
            /// Sets fields for report to be not a personal route.
            /// </summary>
            if (resetMap == undefined) {
                resetMap = true;
            }
            $scope.container.PersonalRouteDropDown.select(0);
            $scope.IsRoute = false;
            isFormDirty = false;
            if (resetMap) {
                setMap($scope.mapStartAddress, $scope.transportType);
            }
            $scope.DriveReport.Addresses = [{ Name: "" }, { Name: "" }];
            updateDrivenKm();
        }

        var setIsRoute = function (index) {
            /// <summary>
            /// Sets field for report to be a personal route.
            /// </summary>
            /// <param name="index"></param>
            $scope.IsRoute = true;
            var route = $scope.Routes[index];
            $scope.DriveReport.Addresses = [];
            var mapArray = [];
            angular.forEach(route.Points, function (address, key) {
                var addr = {
                    Name: address.StreetName + " " + address.StreetNumber + ", " + address.ZipCode + " " + address.Town,
                    Latitude: address.Latitude,
                    Longitude: address.Longitude
                };
                $scope.DriveReport.Addresses.push(addr);
                mapArray.push({ name: addr.Name, lat: addr.Latitude, lng: addr.Longitude });
            });
            setMap(mapArray, $scope.transportType);
            isFormDirty = true;
        }

        $scope.personalRouteDropdownChange = function (e) {
            /// <summary>
            /// Event handler for personal route dropdown.
            /// </summary>
            /// <param name="e"></param>
            var index = e.sender.selectedIndex;
            if (index == 0) {
                setNotRoute();
            } else {
                setIsRoute(index);
            }
        }

        $scope.clearErrorMessages = function () {
            $scope.addressSelectionErrorMessage = "";
            $scope.purposeErrorMessage = "";
            $scope.fourKmRuleValueErrorMessage = "";
            $scope.licensePlateErrorMessage = "";
            $scope.readDistanceErrorMessage = "";
            $scope.userCommentErrorMessage = "";
        }

        $scope.isAddressNameSet = function (address) {
            return !(address.Name == "" || address.Name == $scope.addressPlaceholderText || address.Name == undefined);
        }

        $scope.isAddressPersonalSet = function (address) {
            return !(address.Personal == undefined || address.Personal == "");
        }

        var validateAddressInput = function (setError) {
            setError = typeof setError !== 'undefined' ? setError : true;
            if ($scope.DriveReport.KilometerAllowance == "Read") {
                return true;
            }
            var res = true;
            if (setError === true) {
                $scope.addressSelectionErrorMessage = "";
            }
            angular.forEach($scope.DriveReport.Addresses, function (address, key) {
                if ($scope.isAddressNameSet(address) && $scope.isAddressPersonalSet(address)) {
                    address.Name = "";
                }
                if (!$scope.isAddressNameSet(address) && !$scope.isAddressPersonalSet(address)) {
                    res = false;
                    if (setError === true) {
                        $scope.addressSelectionErrorMessage = "*  Du skal udfylde alle adressefelter.";
                    }
                }
            });
            return res;
        }

        var validateDate = function () {
            $scope.dateErrorMessage = "";
            if ($scope.DriveReport.Date == null || $scope.DriveReport.Date == undefined) {
                $scope.dateErrorMessage = "* Du skal vælge en dato."
                return false;
            }
            return true;
        }

        var validatePurpose = function () {
            /// <summary>
            /// Validates purposes and sets error message in view accordingly.
            /// </summary>
            $scope.purposeErrorMessage = "";
            if ($scope.DriveReport.Purpose == undefined || $scope.DriveReport.Purpose == "") {
                $scope.purposeErrorMessage = "* Du skal angive et formål.";
                return false;
            }
            return true;
        }

        var validateFourKmRule = function () {
            /// <summary>
            /// Validates fourkmrule and sets error message in view accordingly.
            /// </summary>
            $scope.fourKmRuleValueErrorMessage = "";
            if ($scope.DriveReport.FourKmRule.Using === true && ($scope.DriveReport.FourKmRule.Value == "" || $scope.DriveReport.FourKmRule.Value == undefined)) {
                $scope.fourKmRuleValueErrorMessage = "* Du skal udfylde en 4 km-regel værdi.";
                return false;
            }
            return true;
        }

        var validateLicensePlate = function () {
            /// <summary>
            /// Validates license plate and sets error message in view accordingly.
            /// </summary>
            $scope.licensePlateErrorMessage = "";
            if (getKmRate($scope.DriveReport.KmRate).Type.RequiresLicensePlate && $scope.LicensePlates[0].PresentationString == "Ingen nummerplader") {
                $scope.openNoLicensePlateModal();
                $scope.licensePlateErrorMessage = "* Det valgte transportmiddel kræver en nummerplade.";
                return false;
            }
            return true;
        }

        var validateReadInput = function () {
            /// <summary>
            /// Validates Read Report driven distance and sets eror message in view accordingly.
            /// </summary>
            $scope.readDistanceErrorMessage = "";
            $scope.userCommentErrorMessage = "";
            var distRes = true;
            var commRes = true;
            if ($scope.DriveReport.KilometerAllowance == "Read") {
                if ($scope.DriveReport.ReadDistance <= 0 || $scope.DriveReport.ReadDistance == undefined) {
                    $scope.readDistanceErrorMessage = "* Du skal indtaste en kørt afstand.";
                    distRes = false;
                }
                if ($scope.DriveReport.UserComment == undefined || $scope.DriveReport.UserComment == "") {
                    $scope.userCommentErrorMessage = "* Du skal angive en kommentar.";
                    commRes = false;
                }
            }
            return commRes && distRes;
        }



        $scope.addressInputChanged = function (index) {

            /// <summary>
            /// Resolves address coordinates and updates map.
            /// </summary>
            /// <param name="index"></param>
            if (!validateAddressInput(false) || mapChanging || firstMapLoad) {
                return;
            }

            var mapArray = [];

            // Empty array to hold addresses
            var postRequest = [];
            angular.forEach($scope.DriveReport.Addresses, function (addr, key) {
                // Format all addresses and add them to postRequest
                if (!$scope.isAddressNameSet(addr) && addr.Personal != "") {
                    var format = AddressFormatter.fn(addr.Personal);
                    postRequest.push({ StreetName: format.StreetName, StreetNumber: format.StreetNumber, ZipCode: format.ZipCode, Town: format.Town });
                } else if ($scope.isAddressNameSet(addr)) {
                    var format = AddressFormatter.fn(addr.Name);
                    postRequest.push({ StreetName: format.StreetName, StreetNumber: format.StreetNumber, ZipCode: format.ZipCode, Town: format.Town });
                }
            });

            // Send request to backend
            Address.setCoordinatesOnAddressList(postRequest).$promise.then(function (data) {
                // Format address objects for OS2RouteMap once received.
                angular.forEach(data, function (address, value) {
                    mapArray.push({ name: address.streetName + " " + address.streetNumber + ", " + address.zipCode + " " + address.town, lat: address.latitude, lng: address.longitude });
                    $scope.DriveReport.Addresses[value].Latitude = address.latitude;
                    $scope.DriveReport.Addresses[value].Longitude = address.longitude;
                });

                setMap(mapArray, $scope.transportType);
                isFormDirty = true;
            });
        }

        var setMap = function (mapArray, transportType) {
            /// <summary>
            /// Updates the map widget in the view.
            /// </summary>
            /// <param name="mapArray"></param>
            $timeout(function () {
                setMapPromise = $q.defer();
                mapChanging = true;

                OS2RouteMap.set(mapArray, transportType);

                setMapPromise.promise.then(function () {
                    mapChanging = false;
                });
            });
        }

        // Wait for kendo to render.
        $scope.$on("kendoWidgetCreated", function (event, widget) {
            if (widget === $scope.container.KilometerAllowanceDropDown) {
                kendoPromise.resolve();
            }
        });

        var createMap = function () {
            /// <summary>
            /// Creates the map widget in the view.
            /// </summary>
            $timeout(function () {
                // Checks to see whether the map div has been created.
                if (angular.element('#map').length) {
                    OS2RouteMap.create({
                        id: 'map',
                        routeToken: $rootScope.HelpTexts.SEPTIMA_API_KEY.text,
                        change: function (obj) {

                            if (obj.status !== 0 && obj.status != undefined) {
                                createMap();
                                var modalInstance = $modal.open({
                                    templateUrl: '/App/Services/Error/ServiceError.html',
                                    controller: "ServiceErrorController",
                                    backdrop: "static",
                                    resolve: {
                                        errorMsg: function () {
                                            return 'OS2Indberetning kunne ikke beregne ruten. Fejlen kan skyldes, at det ikke er muligt at køre til en/eller flere af dine adresser. Prøv igen eller med en anden adresse tæt på.';
                                        }
                                    }
                                });
                                return;
                            }

                            if (firstMapLoad) {
                                firstMapLoad = false;
                                return;
                            }



                            isFormDirty = true;
                            $scope.currentMapAddresses = obj.Addresses;
                            $scope.latestMapDistance = obj.distance;
                            if (obj.distance == 0) {
                                isFormDirty = false;
                            }
                            updateDrivenKm();

                            // Return if the change comes from AddressInputChanged
                            if (mapChanging === true) {
                                setMapPromise.resolve();
                                return;
                            }

                            mapChanging = true;
                            $scope.DriveReport.Addresses = [];
                            // Load the adresses from the map.
                            var addresses = [];
                            angular.forEach(obj.Addresses, function (address, key) {
                                var shavedName = $scope.shaveExtraCommasOffAddressString(address.name);
                                addresses.push({ Name: shavedName, Latitude: address.lat, Longitude: address.lng });
                            });
                            $scope.DriveReport.Addresses = addresses;
                            // Apply to update the view.
                            $scope.$apply();
                            $timeout(function () {
                                // Wait for the view to render before setting mapChanging to false.

                                mapChanging = false;
                            });

                            // Prevents flickering of addresses when loading a report to be edited.
                            if ($scope.initialEditReportLoad === true) {
                                $scope.initialEditReportLoad = false;
                                return;
                            }

                            if ($scope.IsRoute) {
                                setNotRoute(false);
                            }


                        }
                    });
                    if (!$scope.isEditingReport) {
                        OS2RouteMap.set($scope.mapStartAddress);
                    }
                } else {
                    NotificationService.AutoFadeNotification("danger", "", "Kortet kunne ikke vises. Prøv at genopfriske siden.");
                }
            });
        }



        var dataAndKendoLoaded = function () {
            /// <summary>
            ///  Is called when Kendo has rendered up to and including KilometerAllowanceDropDown and data has been loaded from backend.
            /// Consider this function Main()
            /// Is needed to make sure data and kendo widgets are ready for setting values from previous drivereport.
            /// </summary>

            // Define validateInput now. Otherwise it gets called from drivingview.html before having loaded resources.
            $scope.validateInput = function () {
                $scope.canSubmitDriveReport = validateReadInput();
                $scope.canSubmitDriveReport &= validateAddressInput();
                $scope.canSubmitDriveReport &= validatePurpose();
                $scope.canSubmitDriveReport &= validateLicensePlate();
                $scope.canSubmitDriveReport &= validateFourKmRule();
                $scope.canSubmitDriveReport &= validateDate();
            }

            if (!isEditingReport) {
                $scope.container.driveDatePicker.open();
            }

            // Timeout for wait for dom to render.
            $timeout(function () {
                createMap();
                loadValuesFromReport($scope.latestDriveReport);
                updateDrivenKm();
            });

        }

        $scope.clearReport = function () {
            /// <summary>
            /// Clears user input
            /// </summary>
            setMap($scope.mapStartAddress, $scope.transportType);

            setNotRoute();

            $scope.DriveReport.IsRoundTrip = false;
            $scope.DriveReport.SixtyDaysRule = false;
            loadValuesFromReport($scope.latestDriveReport);
            $scope.DriveReport.Addresses = [{ Name: "" }, { Name: "" }];
            $scope.DriveReport.ReadDistance = 0;
            $scope.DriveReport.UserComment = "";
            $scope.DriveReport.Purpose = "";
            $scope.clearErrorMessages();
            updateDrivenKm();
            $window.scrollTo(0, 0);
            // Timeout to allow the page to scroll to the top before opening datepicker.
            // Otherwise datepicker would sometimes open in the middle of the page instead of anchoring to the control.
            if (!isEditingReport) {
                $timeout(function () {
                    $scope.container.driveDatePicker.open();
                }, 200);
            }

            $scope.DrivenKMDisplay = 0;
            $scope.TransportAllowance = 0;


        }

        $scope.transportChanged = function (res) {
            $q.all(loadingPromises).then(function () {
                var kmRate = getKmRate($scope.DriveReport.KmRate);
                $scope.showLicensePlate = kmRate.Type.RequiresLicensePlate;
                if (kmRate.Type.IsBike) {
                    // If transport was car and has been switched to bicycle.
                    if ($scope.transportType == "car") {
                        if ($scope.currentMapAddresses != undefined) {
                            if ($scope.currentMapAddresses.length > 0) {
                                $scope.transportType = "bicycle";
                                // Call setMap twice to trigger change.
                                setMap($scope.currentMapAddresses, $scope.transportType);
                                setMap($scope.currentMapAddresses, $scope.transportType);
                            }
                        }
                    }
                    $scope.transportType = "bicycle";

                } else {
                    if ($scope.transportType == "bicycle") {
                        if ($scope.currentMapAddresses != undefined) {
                            if ($scope.currentMapAddresses.length > 0) {
                                $scope.transportType = "car";
                                // Call setMap twice to trigger change.
                                setMap($scope.currentMapAddresses, $scope.transportType);
                                setMap($scope.currentMapAddresses, $scope.transportType);
                            }
                        }
                    }
                    $scope.transportType = "car";
                }
            });
        }

        var handleSave = function () {
            /// <summary>
            /// Handles saving of drivereport.
            /// </summary>
            $scope.canSubmitDriveReport = false;
            $scope.saveBtnDisabled = true;
            if (isEditingReport) {
                DriveReport.delete({ id: ReportId }).$promise.then(function () {
                    DriveReport.edit({ emailText: $scope.emailText }, $scope).$promise.then(function (res) {
                        $scope.latestDriveReport = res;
                        NotificationService.AutoFadeNotification("success", "", "Din tjenestekørselsindberetning blev redigeret");
                        $scope.clearReport();
                        $scope.saveBtnDisabled = false;
                        $modalInstance.close();
                        $scope.container.driveDatePicker.close();
                    }, function () {
                        $scope.saveBtnDisabled = false;
                        NotificationService.AutoFadeNotification("danger", "", "Der opstod en fejl under redigering af tjenestekørselsindberetningen.");
                    });
                });
            } else {
                DriveReport.create($scope).$promise.then(function (res) {
                    $scope.latestDriveReport = res;
                    NotificationService.AutoFadeNotification("success", "", "Din indberetning er sendt til godkendelse.");
                    $scope.clearReport();
                    $scope.saveBtnDisabled = false;
                }, function () {
                    $scope.saveBtnDisabled = false;
                    NotificationService.AutoFadeNotification("danger", "", "Der opstod en fejl under oprettelsen af tjenestekørselsindberetningen.");
                });
            }
        }

        $scope.Save = function () {
            $scope.validateInput();
            if (!$scope.canSubmitDriveReport) {
                return;
            }
            if ($scope.DriveReport.Status == "Accepted") {
                // An admin is trying to edit an already approved report.
                var modalInstance = $modal.open({
                    templateUrl: '/App/Admin/HTML/Reports/Modal/ConfirmEditApprovedReportTemplate.html',
                    controller: 'ConfirmEditApprovedReportModalController',
                    backdrop: "static",
                });

                modalInstance.result.then(function (res) {
                    if (res == undefined) {
                        res = "Ingen besked.";
                    }
                    $scope.emailText = res;
                    $scope.prepHandleSave();
                });
            } else {
                $scope.prepHandleSave();
            }
        }

        $scope.prepHandleSave = function () {
            if ($scope.currentUser.DistanceFromHomeToBorder != $scope.DriveReport.FourKmRule.Value && $scope.DriveReport.FourKmRule.Value != "" && $scope.DriveReport.FourKmRule.Value != undefined) {
                $scope.currentUser.DistanceFromHomeToBorder = $scope.DriveReport.FourKmRule.Value
                Person.patch({ id: $scope.currentUser.Id }, { DistanceFromHomeToBorder: $scope.DriveReport.FourKmRule.Value.toString().replace(",", ".") }).$promise.then(function () {
                    handleSave();
                });
            } else {
                handleSave();
            }
        }



        $scope.kilometerAllowanceChanged = function () {
            updateDrivenKm();
            switch ($scope.DriveReport.KilometerAllowance) {
                case "Read":
                    setMap($scope.mapStartAddress, $scope.transportType);
                    break;
                default:
                    $scope.addressInputChanged();
                    break;
            }
        }

        $scope.fourKmRuleChanged = function () {
            if ($scope.alternativeCalculation) {
                if ($scope.DriveReport.FourKmRule.Using) {
                    console.log("altcalc:" + $scope.alternativeCalculation);
                    console.log("usingfourkm:" + $scope.DriveReport.FourKmRule.Using);
                    $scope.AlternativeCalculationTextDistanceForReport = "";
                }
                else {
                    if ($scope.alternativeCalculationKey === "ndk") {
                        console.log("altcalc:" + $scope.alternativeCalculation);
                        console.log("usingfourkm:" + $scope.DriveReport.FourKmRule.Using);
                        $scope.AlternativeCalculationTextDistanceForReport = " (Kan højst svare til hvis tjenesterejsen var påbegyndt og afsluttet på det faste tjenestested)";
                    }
                }
            }
            updateDrivenKm();
        }

        $scope.employmentChanged = function () {
            angular.forEach($scope.currentUser.Employments, function (empl, key) {
                if (empl.Id == $scope.DriveReport.Position) {
                    $scope.WorkAddress = empl.OrgUnit.Address;
                    $scope.hasAccessToFourKmRule = empl.OrgUnit.HasAccessToFourKmRule;
                }
            });
            updateDrivenKm();
        }

        var routeStartsAtHome = function () {
            /// <summary>
            /// returns true if route starts at home
            /// </summary>
            if ($scope.DriveReport.KilometerAllowance == "Read") {
                var index = $scope.container.StartEndHomeDropDown.selectedIndex;
                if (index == 1 || index == 3) {
                    return true;
                }
                return false;
            } else {
                if ($scope.currentMapAddresses == undefined) {
                    return false;
                }
                var endAddress = $scope.currentMapAddresses[0];
                return areAddressesCloseToEachOther($scope.HomeAddress, endAddress);
            }
        }

        var routeEndsAtHome = function () {
            /// <summary>
            /// Returns true if route ends at home.
            /// </summary>
            if ($scope.DriveReport.KilometerAllowance == "Read") {
                var index = $scope.container.StartEndHomeDropDown.selectedIndex;
                if (index == 2 || index == 3) {
                    return true;
                }
                return false;
            } else {
                if ($scope.currentMapAddresses == undefined) {
                    return false;
                }
                var endAddress = $scope.currentMapAddresses[$scope.currentMapAddresses.length - 1];
                return areAddressesCloseToEachOther($scope.HomeAddress, endAddress);
            }
        }

        //Checks that two addresses are within 100 meters, in
        //which case we assume they are the same when regarding
        //if a person starts or ends their route at home.
        var areAddressesCloseToEachOther = function (address1, address2) {
            //Longitude and latitude is called different things depending on
            //whether we get the information from the backend or from septima
            var long1 = (address1.Longitude === undefined) ? address1.lng : address1.Longitude;
            var long2 = (address2.Longitude === undefined) ? address2.lng : address2.Longitude;
            var lat1 = (address1.Latitude === undefined) ? address1.lat : address1.Latitude;
            var lat2 = (address2.Latitude === undefined) ? address2.lat : address2.Latitude;

            var longDiff = Math.abs(Number(long1) - Number(long2));
            var latDiff = Math.abs(Number(lat1) - Number(lat2));
            return longDiff < coordinateThreshold && latDiff < coordinateThreshold;
        }

        $scope.startEndHomeChanged = function () {
            updateDrivenKm();
        }

        var updateDrivenKm = function () {
            /// <summary>
            /// Updates drivenkm fields under map widget.
            /// </summary>
            $timeout(function () {
            if ($scope.DriveReport.KilometerAllowance != "CalculatedWithoutExtraDistance") {
                if (!$scope.alternativeCalculation) {

                    if (routeStartsAtHome() && routeEndsAtHome()) {
                        $scope.TransportAllowance = Number(getCurrentUserEmployment($scope.DriveReport.Position).HomeWorkDistance) * 2;
                    } else if (routeStartsAtHome() || routeEndsAtHome()) {
                        $scope.TransportAllowance = getCurrentUserEmployment($scope.DriveReport.Position).HomeWorkDistance;
                    } else {
                        $scope.TransportAllowance = 0;
                    }
                }
                else {
                    // Get route based on work address if it starts or ends at home.
                    if ($scope.alternativeCalculationKey === "ndk") {
                        if (routeStartsAtHome() || routeEndsAtHome()) {
                            var employmentId = $scope.DriveReport.Position;
                            var transportType = 0;
                            if (getKmRate($scope.DriveReport.KmRate).Type.IsBike) {
                                transportType = 1;
                            }
                            var adresses = [];
                            if (employmentId != undefined && transportType != undefined) {
                                angular.forEach($scope.DriveReport.Addresses, function (addr, key) {
                                    // Format all addresses and add them to postRequest
                                    if (!$scope.isAddressNameSet(addr) && addr.Personal != "") {
                                        var format = AddressFormatter.fn(addr.Personal);
                                        adresses.push({ StreetName: format.StreetName, StreetNumber: format.StreetNumber, ZipCode: format.ZipCode, Town: format.Town });
                                    } else if ($scope.isAddressNameSet(addr)) {
                                        var format = AddressFormatter.fn(addr.Name);
                                        adresses.push({ StreetName: format.StreetName, StreetNumber: format.StreetNumber, ZipCode: format.ZipCode, Town: format.Town });
                                    }
                                });
                                $scope.TransportAllowance = 0;
                                DriveReport.getNDKWorkRouteCalculation({ employmentId: employmentId, transportType: transportType, startsHome: routeStartsAtHome(), endsHome: routeEndsAtHome() }, adresses).$promise.then(function (res) {
                                    $scope.NDKWorkRouteDistance = res.resultData;
                                    if ($scope.latestMapDistance != undefined) {
                                        if ($scope.latestMapDistance > $scope.NDKWorkRouteDistance) {
                                            $scope.TransportAllowance = $scope.latestMapDistance - $scope.NDKWorkRouteDistance;
                                            if ($scope.DriveReport.IsRoundTrip) {
                                                $scope.TransportAllowance = Number($scope.TransportAllowance) * 2;
                                            }
                                        }
                                    }
                                });
                            } else {
                                $scope.TransportAllowance = 0;
                            }

                        } else {
                            $scope.TransportAllowance = 0;
                        }
                    }
                }
                //
            } else {
                $scope.TransportAllowance = 0;
            }

            if ($scope.DriveReport.KilometerAllowance == "Read") {
                if ($scope.DriveReport.ReadDistance == undefined) {
                    $scope.DriveReport.ReadDistance = 0;
                }
                $scope.DrivenKMDisplay = Number($scope.DriveReport.ReadDistance.toString().replace(",", "."));
            } else {
                if ($scope.latestMapDistance == undefined) {
                    $scope.DrivenKMDisplay = 0;
                } else {
                    $scope.DrivenKMDisplay = $scope.latestMapDistance;
                }
            }

            if ($scope.DriveReport.IsRoundTrip === true) {
                // Double the driven km if its a roundtrip.
                $scope.DrivenKMDisplay = Number($scope.DrivenKMDisplay) * 2;
                // If the route starts xor ends at home -> double the transportallowance.
                // The case where the route both ends and starts at home is already covered.
                if (routeStartsAtHome() != routeEndsAtHome()) {

                    $scope.TransportAllowance = Number($scope.TransportAllowance) * 2;
                }
            }
            if ($scope.DriveReport.FourKmRule != undefined && $scope.DriveReport.FourKmRule.Using === true && $scope.DriveReport.FourKmRule.Value != undefined) {
                if (routeStartsAtHome() != routeEndsAtHome()) {
                    if ($scope.DriveReport.IsRoundTrip === true) {
                        $scope.TransportAllowance = (Number($scope.DriveReport.FourKmRule.Value.toString().replace(".", ",")) * 2);
                    }
                    else {
                        $scope.TransportAllowance = Number($scope.DriveReport.FourKmRule.Value.toString().replace(",", "."));
                    }
                } else if (routeStartsAtHome() && routeEndsAtHome()) {
                    $scope.TransportAllowance = (Number($scope.DriveReport.FourKmRule.Value.toString().replace(",", ".")) * 2);
                }
            }
           });
        }

        $scope.readDistanceChanged = function () {
            updateDrivenKm();
        }

        $scope.roundTripChanged = function () {
            updateDrivenKm();
        }

        $scope.closeModalWindow = function () {
            $modalInstance.dismiss();
        }


        var checkShouldPrompt = function () {
            /// <summary>
            /// Return true if there are unsaved changes on the page. 
            /// </summary>

            if (isFormDirty === true) {
                return true;
            }
            if ($scope.DriveReport.Purpose != undefined && $scope.DriveReport.Purpose != $scope.latestDriveReport.Purpose && $scope.DriveReport.Purpose != "") {
                return true;
            }
            if ($scope.DriveReport.ReadDistance != undefined && $scope.DriveReport.ReadDistance != $scope.latestDriveReport.Distance.toString().replace(".", ",") && $scope.DriveReport.ReadDistance != "") {
                return true;
            }
            if ($scope.DriveReport.UserComment != undefined && $scope.DriveReport.UserComment != $scope.latestDriveReport.UserComment && $scope.DriveReport.UserComment != "") {
                return true;
            }
            return false;
        }

        // Alert the user when navigating away from the page if there are unsaved changes.
        $scope.$on('$stateChangeStart', function (event) {
            if (checkShouldPrompt() === true) {
                var answer = confirm("Du har lavet ændringer på siden, der ikke er gemt. Ønsker du at kassere disse ændringer?");
                if (!answer) {
                    event.preventDefault();
                }
            }
        });

        window.onbeforeunload = function (e) {
            if (checkShouldPrompt() === true) {
                return "Du har lavet ændringer på siden, der ikke er gemt. Ønsker du at kassere disse ændringer?";
            }
        };

        $scope.$on('$destroy', function () {
            /// <summary>
            /// Unregister refresh event handler when leaving the page.
            /// </summary>
            window.onbeforeunload = undefined;
        });

        $scope.clearClicked = function () {
            /// <summary>
            /// Opens confirm clear report modal.
            /// </summary>
            /// <param name="id"></param>
            var modalInstance = $modal.open({
                templateUrl: '/App/Driving/ConfirmDiscardChangesTemplate.html',
                controller: 'ConfirmDiscardChangesController',
                backdrop: "static",
            });

            modalInstance.result.then(function () {
                $scope.clearReport();
            });
        }

        $scope.openNoLicensePlateModal = function () {
            /// <summary>
            /// Opens no license plate modal.
            /// </summary>
            /// <param name="id"></param>
            var modalInstance = $modal.open({
                templateUrl: '/App/Driving/NoLicensePlateModalTemplate.html',
                controller: 'NoLicensePlateModalController',
                backdrop: "static",
            });

            modalInstance.result.then(function () {
                $location.path("/settings");
            });
        }
    }
]);