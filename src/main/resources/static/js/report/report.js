var token = $("meta[name='_csrf']").attr("content");

var reportService;
var map;
var addressService;
var notificationService;

$(document).ready(function() {
    reportService = new ReportService();
    addressService = new AddressService();
    reportService.init();
    notificationService = new NotificationService();
    notificationService.getNotification();
});

class DynamicDebouncer {
    constructor(baseDelay = 250, increment = 100, resetInterval = 20000, maxWait = 500) {
        this.baseDelay = baseDelay; // Minimum delay
        this.increment = increment; // Incremental delay for each request
        this.resetInterval = resetInterval; // Time period to reset the count
        this.maxWait = maxWait; // Maximum delay cap
        this.callCount = 0; // Count of requests within the window
        this.timerId = null; // Timer to debounce calls

        // Reset call count after the interval
        setInterval(() => {
            this.callCount = 0;
        }, this.resetInterval);
    }

    debounce(func) {
        return (...args) => {
            // Clear any existing debounce timer
            clearTimeout(this.timerId);

            // Increment the call count
            this.callCount++;
            // Calculate the dynamic delay
            let delay = this.baseDelay + (this.callCount - 1) * this.increment;

            // Apply the maximum delay cap
            if (delay > this.maxWait) {
                delay = this.maxWait;
            }

            // Set a new timer
            this.timerId = setTimeout(() => {
                func(...args); // Execute the function
            }, delay);
        };
    }
}

function ReportService() {
    var routeMarkers;
    var routeMarkerAddresses;
    var routeMarkerCoords;
    var route;
    var routeLine;
    var markerEndAddress;
    var markerEndCoord;
    var resultsHidden = false;
    var currentIndex = -1;
    var addressDropdownSearchDebouncer = new DynamicDebouncer(250, 10, 20000, 500);
    var readyToPost;
    var amountOfExtraViaPoints = 0;

    this.init = function() {
        this.loadFragment();
    }

    this.loadFragment = function() {
        var idObj = $("#reportEditId");
        var id = idObj.val();
        if (id) {
            $('#reportFragmentDiv').load('/report/reportFragment/' + id, this.initFragment);
        }
        else {
            $('#reportFragmentDiv').load('/report/reportFragment', this.initFragment);
        }
    }

    this.initFragment = function() {
        map = addressService.mapInit([$("#map").data('lat'), $("#map").data('lng')]);

        var markerEnd = this.markerEnd = L.marker([0, 0]);
        this.routerLine = [{}];

        $("#employmentSelect").on('change', function(e) {
            reportService.employmentUpdated(this);
        });
        reportService.employmentUpdated($("#employmentSelect"));
        reportService.routeMarkers = [];
        reportService.routeMarkerAddresses = [];
        reportService.routeMarkerCoords = [];

        var today = new Date().toISOString().split('T')[0];
        $("#driveDate")[0].setAttribute('max', today);
        $(function () {
          $('[data-toggle="tooltip"]').tooltip()
        })

        if (!$("#editMapRoute").val()) {
            $("#driveDate").val(today);
        }

        $("#calculationType").on('change', function(e) {
            reportService.calcTypeChange($("#calculationType").val());
        });

        $(".addressinput").on('keyup', function(e) {
            clearTimeout(typingTimer);
            // Debounce logic
            typingTimer = setTimeout(() => {
                if(e.key === 'Enter' || e.keyCode === 13 ) {
                    if(reportService.ifReady() === true) { reportService.drawRoute("reportService.initFragment"); }
                }
                else if(this.value.length > 2) {
                    var autocompleteList = addressService.addressAutocomplete(this.value);
                    const suggestions = document.getElementById(this.id + 'List');
                    suggestions.innerHTML = '';

                    autocompleteList.forEach(function(sugg) {
                        var option = document.createElement('option');
                        option.value = sugg.tekst
                        suggestions.appendChild(option);
                    });
                }
                else {
                    document.getElementById(this.id + 'List').innerHTML = '';
                }
            }, typingDelay);
        });

        $('#startsAtHome').on("change", function () {
            addressService.getDeltaDistance();
        });

        $('#endsAtHome').on("change", function () {
            addressService.getDeltaDistance();
        });

        $('#fourKmDistance').on("input", function () {
            addressService.getDeltaDistance();
        })

        $(".addressinput").on('input', function() {
          const value = $(this).val();
          let isMatch = false;

          datalist = $("#" + $(this).attr("list"));

          // Loop through the options in the datalist
          let options = datalist.children("option");
          for (let i = 0; i < options.length; i++) {
            if (options[i].value === value) {
              isMatch = true;
              break;
            }
          }

          if (isMatch && reportService.ifReady() === true) {
            reportService.drawRoute(true, "reportService.initFragment");
          }
        });

        $(".addressinput").on('focusout change', function(e) {
            if(reportService.ifReady() === true) { reportService.drawRoute("reportService.initFragment"); }
        });

        $('.i-checks').iCheck({
            checkboxClass: 'icheckbox_square-blue',
            radioClass: 'iradio_square-blue',
        });

        $("#fourKmCheck").on('ifToggled', function(event){
            $("#fourKmDistanceDiv").attr('hidden', !$("#fourKmCheck").prop('checked'));
        });

        $("#roundTripCheck").on('ifToggled', function(){
            if($("#calculationType").val() === "1") {
                $("#distanceFieldKm").val(Math.round(($("#distanceFieldKmRead").val()*1000)/10*(1+$("#roundTripCheck").prop('checked'))+Number.EPSILON)/100);
            }
            else {
                if(reportService.route != undefined) { $("#distanceFieldKm").val(Math.round(reportService.route.distance/10*(1+$("#roundTripCheck").prop('checked'))+Number.EPSILON)/100); }
            }
        });

        $("#distanceFieldKmRead").on('change', function(event) {
            $("#distanceFieldKm").val(Math.round(($("#distanceFieldKmRead").val()*1000)/10*(1+$("#roundTripCheck").prop('checked'))+Number.EPSILON)/100);
            $("#rawDistanceFieldKm").val(Math.round(($("#distanceFieldKmRead").val()*1000)/10+Number.EPSILON)/100);
        });


        // TODO: MALTHE Commented for now, i dont really see any bad side effects, maybe we over-implemented something here?
        // Quick hack for fixing deltaDistance when editing a report
        // if ($('#driveDate').val()) {
        //     $.ajax({
        //         method: "POST",
        //         url: "/rest/report/available-information",
        //         headers: {
        //             "content-type": "application/json",
        //             'X-CSRF-TOKEN': token
        //         },
        //         data: $("#driveDate").val(),
        //         success: function(data, textStatus, jqXHR) {
        //             $('#subtractedBySavedReports').val(data.deltaDistance);
        //             $('#fourKmWithdrawnBySavedReports').val(data.fourKmWithdrawn);
        //             $('#deltaDistance').val(data.deltaDistance);
        //             $('#maxDistanceToSubtract').val(data.maxDistanceToSubtract);
        //         },
        //         error: function(jqXHR, textStatus, errorThrown) {}
        //     })
        // }

        $("#driveDate").on('change', function() {
            $.ajax({
                method: "POST",
                url: "/rest/report/available-information",
                headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
                },
                data: $("#driveDate").val(),
                success: function(data, textStatus, jqXHR) {
                    if (data.addresses !== undefined) {
                        let selects = $("#startSelect, #endSelect, select[id^=waymarker]");
                        for (var i = 0; i < selects.length; i++) {
                            let select = $(selects[i]);
                            select.empty();
                            select.append('<option value="">Eller vælg adresse fra liste</option>')
                            for (var j = 0; j < data.addresses.length; j++) {
                                var opt = document.createElement('option');
                                opt.value = data.addresses[j].address;
                                opt.innerHTML = data.addresses[j].description;
                                select.append(opt);
                            }
                        }
                    }
                    $('#subtractedBySavedReports').val(data.deltaDistance);
                    $('#fourKmWithdrawnBySavedReports').val(data.fourKmWithdrawn);
                    $('#deltaDistance').val(data.deltaDistance);
                    $('#maxDistanceToSubtract').val(data.maxDistanceToSubtract);

                    if ($('#driveDate').val() && $('#startField').val() && $('#endField').val()) {
                        addressService.getDeltaDistance();
                    }
                },
                error: function(jqXHR, textStatus, errorThrown) {}
            })
        });

        $('#roundTripCheck').on("change", function () {
            if ($('#driveDate').val() && $('#startField').val() && $('#endField').val()) {
                addressService.getDeltaDistance();
            }
        })

        $('#fourKmCheck').on("change", function () {
            if ($('#driveDate').val() && $('#startField').val() && $('#endField').val()) {
                addressService.getDeltaDistance();
            }
        })

        $("#driveDate").trigger("change");

        reportService.calcTypeChange($("#calculationType").val(), false);
        $("#fourKmCheck").trigger('ifToggled');
        $("#roundTripCheck").trigger('ifToggled');

        if ($("#editMapRoute").val()) {
            let routeCoordinates = JSON.parse($("#editMapRoute").val())
            if (routeCoordinates) {
                let routeLine = L.geoJSON(routeCoordinates.geometry).addTo(map);
                map.fitBounds(routeLine.getBounds());
            }

            $(".waymarker").each(function( index, element) {
                reportService.routeMarkerAddresses[index + 1] = addressService.addressWash($(element).val());
                let waymark = addressService.addressToLtLg(reportService.routeMarkerAddresses[index + 1]);
                reportService.routeMarkers[index + 1] = L.marker([waymark[0].lat, waymark[0].lng],{draggable: true, autoPan: true}).addTo(map)

            });

            reportService.drawRoute(true, "reportService.initFragment (edit)");
        }

        var urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('success')) {
            toastr.success("Indberetningen gemt");
        }
    }

    this.drawInitialRoute = function() {
        if (reportService.routeMarkers[0]) { map.removeLayer(reportService.routeMarkers[0]); }
        if (reportService.markerEnd) { map.removeLayer(this.markerEnd); }
        if (reportService.routeLine) { map.removeLayer(this.routeLine); }

        reportService.routeMarkerAddresses[0] = addressService.addressWash($("#startField").val());
        var pointStart = addressService.addressToLtLg(reportService.routeMarkerAddresses[0]);
        reportService.markerEndAddress = addressService.addressWash($("#endField").val());
        var pointEnd = addressService.addressToLtLg(reportService.markerEndAddress);

        reportService.routeMarkers[0] = L.marker([pointStart[0].lat, pointStart[0].lng],{draggable: true, autoPan: true, title: $("#startField").val()}).addTo(map)
        reportService.markerEnd = L.marker([pointEnd[0].lat, pointEnd[0].lng],{draggable: true, autoPan: true, title: $("#endField").val()}).addTo(map)

        reportService.routeMarkers[0].on('dragend', function(e) {
            reportService.routeMarkerCoords[0] = this.getLatLng();
            reportService.routeMarkerAddresses[0] = addressService.latLngToAddress(reportService.routeMarkerCoords[0]);
            $("#startField").val(addressService.addressString(reportService.routeMarkerAddresses[0]));
            reportService.drawRoute("reportService.drawInitialRoute");
        });
        this.markerEnd.on('dragend', function(e) {
            reportService.markerEndAddress = addressService.latLngToAddress(this.getLatLng());
            $("#endField").val(addressService.addressString(reportService.markerEndAddress.address));
            reportService.drawRoute("reportService.drawInitialRoute");
        });
    }

    this.clearRoute = function() {

        if (reportService.routeMarkers) {
            for (let i = 0; i < reportService.routeMarkers.length; i++) {
                map.removeLayer(reportService.routeMarkers[i]);
            }
            reportService.routeMarkers = [];
            reportService.routeMarkerAddresses = [];
        }
        if (reportService.markerEnd) {
            map.removeLayer(this.markerEnd);
        }
        if (reportService.routeLine) {
            map.removeLayer(this.routeLine);
        }

    }

    this.drawRoute = function(drawInitialAgain=false, functionName) {
        if(reportService.routeMarkers.length < 2 || drawInitialAgain === true) { this.drawInitialRoute() }
        else { this.additionalWaymarkers(); }
        $("path").remove();
        this.route = addressService.route(reportService.routeMarkers, reportService.markerEnd, functionName);
        if (this.route === false) {
            readyToPost = false;
            return;
        }
        else {
            // Draws the line and adds it to map
            this.routeLine = L.geoJSON(this.route.geometry,{onEachFeature: (_, layer) => layer.on('click', this.onRouteClick)}).addTo(map)
            map.fitBounds(this.routeLine.getBounds());
            let distanceFieldKM = Math.round(reportService.route.distance/10*(1+$("#roundTripCheck").prop('checked'))+Number.EPSILON)/100;
            let rawDistanceFieldKM = Math.round(reportService.route.distance/10+Number.EPSILON)/100;
            if (Number.isNaN(distanceFieldKM)) {
                $('#distanceFieldKm').val(0);
            }
            else {
                $("#distanceFieldKm").val(distanceFieldKM);
            }
            if (Number.isNaN(rawDistanceFieldKM)) {
                $('#rawDistanceFieldKm').val(0);
            }
            else {
                $('#rawDistanceFieldKm').val(rawDistanceFieldKM);
            }
            readyToPost = true;
            addressService.getDeltaDistance();
        }
    }

    this.ifReady = function() {
        let addressFilled = true;
        $(".addressinput").each(function() {
            if($(this).val() === "") {
                addressFilled = false;
                return false;
            }
        });
        if(addressFilled) { return true; }
    }

    this.personalRouteInputChange = function (obj) {
        // Get the selected option
        var selectedOption = $('#selectKnownRoute').find('option:selected');
        if (selectedOption.val() > 0) {
            $('#moreWayPointsButton').hide();
            $('#waypointDiv').hide();
            $('#fromRow').hide();
            $('#toRow').hide();

            $.ajax({
                method : "GET",
                url: "/rest/personalRoute/getAddresses?id=" +selectedOption.val(),
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                success: function(data, textStatus, jqXHR) {
                    reportService.clearRoute();
                    $("#waypointDiv").html('');
                    for (let i = 0; i < data.addresses.length; i++) {

                        if (i == 0) {
                            $('#startField').val(data.addresses[i]);
                        }
                        else if (i == data.addresses.length-1) {
                            $('#endField').val(data.addresses[i]);
                        }
                        else {
                            reportService.routeMarkerAddresses[i] = addressService.addressWash(data.addresses[i]);
                            let waymark = addressService.addressToLtLg(reportService.routeMarkerAddresses[i]);
                            reportService.routeMarkers[i] = L.marker([waymark[0].lat, waymark[0].lng],{draggable: true, autoPan: true}).addTo(map)

                            reportService.routeMarkers[i].on('dragend', function(e) {
                                reportService.routeMarkerCoords[i] = this.getLatLng();
                                reportService.routeMarkerAddresses[i] = addressService.latLngToAddress(reportService.routeMarkerCoords[i]);
                                $('#waymarker' + i).val(addressService.addressString(reportService.routeMarkerAddresses[i]));
                                reportService.drawRoute("reportService.personalRouteInputChange");
                            });
                        }
                    }

                    map.invalidateSize();
                    reportService.drawRoute(true, "reportService.personalRouteInputChange");
                    map.fitBounds(reportService.routeLine.getBounds());
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    toastr.warning("Der opstod en teknisk fejl");
                }
            })
        } else {
            $('#moreWayPointsButton').show();
            $('#fromRow').show();
            $('#toRow').show();
            $('#waypointDiv').show();
             map.invalidateSize();
             map.fitBounds(reportService.routeLine.getBounds());
        }
    }

    this.onRouteClick = function(event) {
        let listId = reportService.routeMarkers.length;
        reportService.routeMarkerCoords[listId] = event.latlng;
        reportService.routeMarkerAddresses[listId] = addressService.latLngToAddress(reportService.routeMarkerCoords[listId]);
        $('#waymarker' + listId).val(addressService.addressString(reportService.routeMarkerAddresses[listId]));
        reportService.routeMarkers[listId] = L.marker([event.latlng.lat, event.latlng.lng],{draggable: true, autoPan: true, title: addressService.addressString(reportService.routeMarkerAddresses[listId]), listId: listId}).addTo(map);

        reportService.routeMarkers[listId].on('dragend', function(e) {
            reportService.routeMarkerCoords[this.options.listId] = this.getLatLng();
            reportService.routeMarkerAddresses[this.options.listId] = addressService.latLngToAddress(reportService.routeMarkerCoords[this.options.listId]);
            $('#waymarker' + this.options.listId).val(addressService.addressString(reportService.routeMarkerAddresses[this.options.listId]));
            reportService.updateWaypoint(this.options.listId);
        });
        reportService.drawRoute("reportService.onRouteClick");
        amountOfExtraViaPoints += 1;
    }

    this.onAddWaymarkerPressed = function(event) {
        if (!$('#startField').val() || !$('#endField').val()) {
            toastr.warning("Du skal indtaste en start og stop addresse inden du kan tilføje delmål")
            return;
        }
        let listId = reportService.routeMarkers.length;
        if (listId == 0) {
            toastr.warning("Der opstod en teknisk fejl");
            return;
        }
        var newLatLng = reportService.routeMarkers[reportService.routeMarkers.length-1]._latlng;
        reportService.routeMarkerCoords[listId] = newLatLng;
        reportService.routeMarkerAddresses[listId] = undefined;
        reportService.routeMarkers[listId] = L.marker([newLatLng.lat, newLatLng.lng],{draggable: true, autoPan: true, title: addressService.addressString(addressService.latLngToAddress(reportService.routeMarkerCoords[listId])), listId: listId}).addTo(map);

        reportService.routeMarkers[listId].on('dragend', function(e) {
            reportService.routeMarkerCoords[this.options.listId] = this.getLatLng();
            reportService.routeMarkerAddresses[this.options.listId] = addressService.latLngToAddress(reportService.routeMarkerCoords[this.options.listId]);
            $('#waymarker' + this.options.listId).val(addressService.addressString(reportService.routeMarkerAddresses[this.options.listId]));
            reportService.updateWaypoint(this.options.listId);
            reportService.drawRoute("reportService.onAddWaymarkerPressed");
        });
        amountOfExtraViaPoints = amountOfExtraViaPoints + 1;
        reportService.drawRoute("reportService.onAddWaymarkerPressed");
    }

    this.isThereMissingFields = function() {
        let isSomethingMissing = false;

        let driveDate = $("#driveDate")
        if(!driveDate.val()) {
            isSomethingMissing = true;
            driveDate.addClass('error-field');
        } else {
            driveDate.css("background-color", "white")
        }

        let employmentId = $("#employmentSelect");
        if(!employmentId.val()) {
            isSomethingMissing = true
            employmentId.addClass('error-field');
        } else {
            employmentId.css("background-color", "white")
        }

        let rateTypeId = $("#rateSelect")
        if(!rateTypeId.val()) {
            isSomethingMissing = true
            rateTypeId.addClass('error-field');
        } else {
            rateTypeId.css("background-color", "white")
        }

        let licensePlateId = $("#plateSelect")
        if(!licensePlateId.val()) {
            isSomethingMissing = true
            licensePlateId.addClass('error-field');
        } else {
            licensePlateId.css("background-color", "white")
        }

        let purpose = $("#purposeField")
        if(!purpose.val()) {
            isSomethingMissing = true
            purpose.addClass('error-field');

        } else {
            purpose.css("background-color", "white")
        }

        let rawDistance = $("#distanceFieldKmRead")
        if($("#calculationType").val() === "1" && (!rawDistance.val() || rawDistance.val() === "0")) {
            isSomethingMissing = true;
            rawDistance.addClass('error-field');
        } else {
            rawDistance.css("background-color", "white");
        }

        let comment = $("#commentField")
        if($("#calculationType").val() === "1" && !comment.val()) {
            isSomethingMissing = true;
            comment.addClass('error-field');
        } else {
            comment.css("background-color", "white");
        }

        let startAddr = $("#startField");
        if($("#calculationType").val() !== "1" && !startAddr.val()) {
            isSomethingMissing = true;
            startAddr.addClass('error-field');
        } else {
            startAddr.css("background-color", "white");
        }

       let endAddr = $("#endField");
       if($("#calculationType").val() !== "1" && !endAddr.val()) {
           isSomethingMissing = true;
            endAddr.addClass('error-field');
        } else {
            endAddr.css("background-color", "white");
        }

        return isSomethingMissing;
    }

    this.save = function() {
        if (!readyToPost && $('#calculationType').val() != 1) {
            toastr.warning("Der er en fejl med ruten, prøv at danne ruten igen");
            return;
        }
        if(this.isThereMissingFields()) {
            toastr.warning("Et eller flere felter mangler at blive udfyldt")
            return;
        }

        let comment = $("#commentField")
        if($("#calculationType").val() === "1" && comment.val().length > 255) {
            comment.css("background-color", "#FFD6E4");
            toastr.warning("Feltet til yderligere bemærkninger kan kun være 255 tegn langt!")
            return;
        } else {
            comment.css("background-color", "white");
        }

        let purpose = $("#purposeField")
        if(purpose.val().length > 255) {
            purpose.css("background-color", "#FFD6E4");
            toastr.warning("Feltet til formål kan kun være 255 tegn langt!")
            return;
        } else {
            comment.css("background-color", "white");
        }
        if (amountOfExtraViaPoints > 0) {
            let inputEntered = false;
            for (let i = 0; i < amountOfExtraViaPoints; i++) {
                if (!$('#waymarker' + (i + 1)).val()) {
                    $('#waymarker' + (i+1)).css("background-color", "#FFD6E4");
                    toastr.warning("Du mangler at udfylde alle addresse felter!");
                    return;
                }
            }
        }
        let chosenDriveDate = $("#driveDate");
        if (!reportService.isValidDateFormat(chosenDriveDate.val())) {
            toastr.warning("Datoen er ikke i korrekt format: " + chosenDriveDate.val());
            return;
        }
        if(moment().isBefore(moment(chosenDriveDate.val()))) {
            chosenDriveDate.css("background-color", "#FFD6E4");
            toastr.warning("Dato må ikke være i fremtiden!")
            return;
        } else {
            chosenDriveDate.css("background-color", "white");
        }

        if(!($("#calculationType").val() === "1")) {
            var data = this.route.coordinates;
        }
        var body = JSON.stringify({
            "id": !!($("#reportEditId").val()) ? $("#reportEditId").val() : 0 ,
            "driveDate": $("#driveDate").val(),
            "employmentId": $("#employmentSelect").val(),
            "rateTypeId": $("#rateSelect").val(),
            "licensePlateId": $("#plateSelect").val(),
            "purpose": $("#purposeField").val(),
            "routeGeometry": data ? data.toString() : "",
            "fourKmRule": $("#fourKmCheck").prop('checked'),
            "roundTrip": $("#roundTripCheck").prop('checked'),
            "startsAtHome": $("#calculationType").val() === "1" ? $("#startsAtHome").prop('checked')  : false,
            "endsAtHome": $("#calculationType").val() === "1" ? $("#endsAtHome").prop('checked') : false,
            "calculationType": $("#calculationType").val(),
            "rawDistance": $("#calculationType").val() === "1" ? $("#distanceFieldKmRead").val() : $("#rawDistanceFieldKm").val(),
            "comment": $("#calculationType").val() === "1" ? $("#commentField").val() : "",
            "homeToBorder": $("#fourKmCheck").prop('checked') === true ? $("#fourKmDistance").val() : -1.0,
            "addressList": !($("#calculationType").val() === "1") ? reportService.createAddressList() : []
        });
        $('#saveButton').prop("disabled", true);
        $('#editSaveButton').prop("disabled", true);
        $.ajax({
            method : "POST",
            url: "/rest/report/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: body,
            success: function(data, textStatus, jqXHR) {
                toastr.success("Indberetningen gemt");
                window.location = '/report?success'
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en teknisk fejl");
                $('#saveButton').prop("disabled", false);
                $('#editSaveButton').prop("disabled", false);
            }
        })
    }

    this.additionalWaymarkers = function() {
        $("#waypointDiv").html('');
        for (let i = 1; i < reportService.routeMarkers.length; i++ ) {
            if(reportService.routeMarkers[i] === undefined) {
                continue;
            }

            var waypoint =
                '<div class="form-group row">' +
                '    <label class="col-sm-3 col-form-label" style="text-align:left;">' +
                '        <p style="text-align:left;">Via</p>' +
                '        <label class="col-form-row" style="text-align:left;">' +
                '           <i class="fa fa-trash-o" data-id="' + i + '" onclick="reportService.deleteWaymarker(this)" title="Slet"></i>' +
                '           <i class="fa fa-arrow-up" data-id="' + i + '" onclick="reportService.moveWaymarker(true, this)" title="Flyt op"></i>' +
                '           <i class="fa fa-arrow-down" data-id="' + i + '" onclick="reportService.moveWaymarker(false, this)" title="Flyt ned"></i>' +
                '       </label>' +
                '    </label>' +
                '    <div class="col-sm-9">' +
                '        <input class="addressinput form-control" type="text" id="waymarker' + i + '" value="' + (reportService.routeMarkerAddresses[i] === undefined ? '' : addressService.addressString(reportService.routeMarkerAddresses[i])) + '" placeholder="Indtast addresse her" data-listid="' + i + '" autocomplete="off" list="waymarker' + i + 'List">' +
                '        <dataList id="waymarker' + i + 'List"></dataList>' +
                '        <div class="form-group">' +
                '           <div class="input-group">' +
                '               <input class="form-control" id="searchAddresses' + i + '" placeholder="Eller vælg adresse fra liste" oninput="reportService.onSearch(' + i + ')" onclick="reportService.triggerSearchOnClick(' + i + ')" onkeydown="reportService.handleKeyNavigation(event, ' + i + ')" onfocusin="reportService.onSearch(' + i + ')">' +
                '               <span class="input-group-addon">' +
                '                   <span class="fa fa-arrow-down" onclick="reportService.initSearch(' + i + ')"></span>' +
                '               </span>' +
                '           </div>' +
                '           <div id="searchResults' + i + '" class="list-group resultContainer" style="position: absolute; z-index: 1000; width: 95.5%; background: white; border: 1px solid #ccc; box-shadow: 0px 40px 6px rgba(0, 0, 0, 0.02);"></div>' +
                '        </div>' +
                '    </div>' +
                '</div>';

            $("#waypointDiv").append(waypoint);

            reportService.routeMarkers[i].on('dragend', function(e) {
                reportService.routeMarkerCoords[i] = this.getLatLng();
                reportService.routeMarkerAddresses[i] = addressService.latLngToAddress(reportService.routeMarkerCoords[i]);

                $('#waymarker' + i).val(addressService.addressString(reportService.routeMarkerAddresses[i]));
                reportService.updateWaypoint(i);
            });

            $('#startSelect option').clone().appendTo('#waymarker' + i + 'Select');
            $('#waymarker' + i).on('keyup', function(e) {
                clearTimeout(typingTimer);
                // Debounce logic
                typingTimer = setTimeout(() => {
                    if (e.key === 'Enter' || e.keyCode === 13) {
                        reportService.updateWaypoint(this.dataset.listid);
                        if (reportService.ifReady() === true) {
                            reportService.drawRoute("reportService.additionalWaymarkers");
                        }
                    } else if (this.value.length > 2) {
                        var autocompleteList = addressService.addressAutocomplete(this.value);
                        const suggestions = document.getElementById(this.id + 'List');
                        suggestions.innerHTML = '';

                        autocompleteList.forEach(function(sugg) {
                            var option = document.createElement('option');
                            option.value = sugg.tekst
                            suggestions.appendChild(option);
                        });
                    } else {
                        document.getElementById(this.id + 'List').innerHTML = '';
                    }
                }, typingDelay);
            });

            $('#waymarker' + i).on('input', function() {
              const value = $(this).val();
              let isMatch = false;

              datalist = $("#" + $(this).attr("list"));

              // Loop through the options in the datalist
              let options = datalist.children("option");
              for (let i = 0; i < options.length; i++) {
                if (options[i].value === value) {
                  isMatch = true;
                  break;
                }
              }

              if (isMatch && reportService.ifReady() === true) {
                reportService.updateWaypoint(this.dataset.listid);
                reportService.drawRoute("reportService.additionalWaymarkers");
              }
            });

            $('#waymarker' + i).on('focusout', function(e) {
                reportService.updateWaypoint(this.dataset.listid);
                if (reportService.ifReady() === true) {
                    reportService.drawRoute("reportService.additionalWaymarkers");
                }
            });
        }
    }

    this.updateWaypoint = function(i) {
        map.removeLayer(reportService.routeMarkers[i]);
        reportService.routeMarkerAddresses[i] = addressService.addressWash($('#waymarker' + i).val());
        var wpLoc = addressService.addressToLtLg(reportService.routeMarkerAddresses[i]);
        reportService.routeMarkers[i] = L.marker([wpLoc[0].lat, wpLoc[0].lng], {
            draggable: true,
            autoPan: true,
            title: addressService.addressString(reportService.routeMarkerAddresses[i]),
            listId: i
        }).addTo(map);

        reportService.routeMarkers[i].on('dragend', function(e) {
            reportService.routeMarkerCoords[this.options.listId] = this.getLatLng();
            reportService.routeMarkerAddresses[this.options.listId] = addressService.latLngToAddress(reportService.routeMarkerCoords[this.options.listId]);
            $('#waymarker' + this.options.listId).val(addressService.addressString(reportService.routeMarkerAddresses[this.options.listId]));
        });
        reportService.drawRoute("reportService.updateWaypoint");
    }

    this.deleteWaymarker = function(data) {
        var i = data.dataset.id;
        map.removeLayer(reportService.routeMarkers[i]);
        reportService.routeMarkers.splice(i, 1);
        reportService.routeMarkerAddresses.splice(i, 1);
        if (reportService.routeMarkers.length < 2) {
            this.additionalWaymarkers();
        }
        amountOfExtraViaPoints--;
        reportService.drawRoute("reportService.deleteWayMarker");
    }

    this.moveWaymarker = function(direction, element) {
        var placeholderAddress = reportService.routeMarkerAddresses[element.dataset.id];
        var placeholderCoords = reportService.routeMarkerCoords[element.dataset.id];
        var placeholderMarker = reportService.routeMarkers[element.dataset.id];
        if (direction == true) {
            // Move waymarker up
            reportService.routeMarkerAddresses[element.dataset.id] = reportService.routeMarkerAddresses[element.dataset.id - 1];
            reportService.routeMarkerCoords[element.dataset.id] = reportService.routeMarkerCoords[element.dataset.id - 1];
            reportService.routeMarkers[element.dataset.id] = reportService.routeMarkers[element.dataset.id - 1];
            reportService.routeMarkerAddresses[element.dataset.id - 1] = placeholderAddress;
            reportService.routeMarkerCoords[element.dataset.id - 1] = placeholderCoords;
            reportService.routeMarkers[element.dataset.id - 1] = placeholderMarker;
            if (element.dataset.id - 1 === 0) {
                $("#startField").val(addressService.addressString(placeholderAddress));
            }
        } else if (parseInt(element.dataset.id) + 1 == reportService.routeMarkers.length) {
            // Switch waymarker to end field
            reportService.routeMarkerAddresses[element.dataset.id] = reportService.markerEndAddress;
            reportService.routeMarkerCoords[element.dataset.id] = reportService.markerEnd.getLatLng();
            reportService.routeMarkers[element.dataset.id] = reportService.markerEnd;
            reportService.markerEndAddress = placeholderAddress;
            reportService.markerEnd = placeholderMarker;
            $("#endField").val(addressService.addressString(reportService.markerEndAddress));
        } else {
            // Move waymarker down
            reportService.routeMarkerAddresses[element.dataset.id] = reportService.routeMarkerAddresses[parseInt(element.dataset.id) + 1];
            reportService.routeMarkerCoords[element.dataset.id] = reportService.routeMarkerCoords[parseInt(element.dataset.id) + 1];
            reportService.routeMarkers[element.dataset.id] = reportService.routeMarkers[parseInt(element.dataset.id) + 1];
            reportService.routeMarkerAddresses[parseInt(element.dataset.id) + 1] = placeholderAddress;
            reportService.routeMarkerCoords[parseInt(element.dataset.id) + 1] = placeholderCoords;
            reportService.routeMarkers[parseInt(element.dataset.id) + 1] = placeholderMarker;
        }
        this.additionalWaymarkers();
        this.drawRoute(true, "reportService.moveWaymarker");
    }

    this.createAddressList = function() {
        let jsonAddressList = '[';
        reportService.routeMarkers.push(reportService.markerEnd);
        reportService.routeMarkerAddresses.push(reportService.markerEndAddress);

        for (let i = 0; i < reportService.routeMarkers.length; i++) {
            let addr = reportService.routeMarkerAddresses[i];
            if(addr === undefined) {
                continue;
            }

            var coord = reportService.routeMarkers[i].getLatLng();
            jsonAddressList += JSON.stringify({
                "road": (addr.vejstykke.navn || ""),
                "house_number": (addr.husnr || ""),
                "postcode": (addr.postnummer.nr || ""),
                "town": (addr.postnummer.navn || ""),
                "lat": (coord.lat || 0),
                "lon": (coord.lng || 0),
                "lng": (coord.lng || 0)
            });
            jsonAddressList += ','
        }
        jsonAddressList = jsonAddressList.substring(0, jsonAddressList.length - 1);
        jsonAddressList += ']';
        return JSON.parse(jsonAddressList);
    }

    this.cancel = function() {
         window.location.href = "/report";
    }

    this.employmentUpdated = function (obj) {
        var opt = $(obj).find(":selected");

        var fourKmRuleAllowed = opt.data("four-km-rule");
        var defaultCalculationType = opt.data("default-calculation-type");

        if (fourKmRuleAllowed) {
            $("#fourKmDiv").attr('hidden', false);
        } else {
            $("#fourKmDiv").attr('hidden', true);
            $("#fourKmCheck").iCheck('uncheck');
            $("#fourKmDistance").val(null);
        }

        if (!$("#reportEditId").val()) {
            $("#calculationType").val(defaultCalculationType).change();
        }
    }

    var debouncedOnSearch ;
    this.onSearch = function (id) {
        if (!debouncedOnSearch) {
            debouncedOnSearch = addressDropdownSearchDebouncer.debounce((id) => {
                currentIndex = -1;
                let searchInput = $('#searchAddresses' + id).val();
                let resultsContainer = $('#searchResults' + id);
                resultsContainer.show();
                if (searchInput !== undefined && searchInput.length > 1) {
                    $.ajax({
                        url: "/rest/report/addresses/search",
                        headers: {
                            'X-CSRF-TOKEN': token
                        },
                        type: 'POST',
                        data: JSON.stringify({input:$('#searchAddresses' + id).val(), date: $("#driveDate").val()}),
                        contentType: 'application/json',
                        success: function (data, textStatus, jQxhr) {
                            let icon = '<i class="fa-solid fa fa-location-arrow"></i>';
                            resultsContainer.empty();
                            for (let dataKey in data) {
                                let addressString = data[dataKey].addressString;
                                let addressStringActual = data[dataKey].addressString;
                                if (data[dataKey].type == "HOME") {
                                    icon = '<i class="fa-solid fa fa-home"></i>';
                                    addressString += (" ("+data[dataKey].description+")");
                                }
                                if (data[dataKey].type == "STANDARD") {
                                    icon = '<i class="fa-solid fa fa-location-arrow"></i>';
                                    addressString += (" ("+data[dataKey].description+")");
                                }
                                if (data[dataKey].type == "DHOME") {
                                    icon = '<i class="fa-solid fa fa-home"></i>';
                                    addressString += " (Afvigende hjemmeadresse)";
                                }
                                if (data[dataKey].type == "WORK") {
                                    icon = '<i class="fa-solid fa fa-group"></i>';
                                    addressString += (" ("+data[dataKey].description+")")
                                }
                                if (data[dataKey].type == "DWORK") {
                                    icon = '<i class="fa-solid fa fa-group"></i>';
                                    addressString += " (Afvigende arbejdsadresse)";
                                }
                                if (data[dataKey].type == "ALTERNATIVE") {
                                    addressString += " (" + data[dataKey].description + ")";
                                }
                                var resultItem = $('<a>')
                                    .addClass('list-group-item list-group-item-action')
                                    .attr('id', data[dataKey].id)
                                    .html(`<span>${icon}</span> &nbsp;${addressString}<p style="display: none">${addressStringActual}</p>`)
                                    .on('click', function () {
                                        reportService.handleItemClick(data[dataKey], id);
                                    });
                                resultsContainer.append(resultItem);
                            }
                        }
                    });
                }
                else {
                    reportService.initSearch(id);
                }
            });
        }
        debouncedOnSearch(id);
    }

    this.initSearch = function (id) {
        let resultsContainer = $('#searchResults' + id);
        if (!reportService.isValidDateFormat($('#driveDate').val())) {
            return;
        }
        resultsContainer.empty();
        $.ajax({
            url: "/rest/report/addresses/search/init",
            headers: {
                'X-CSRF-TOKEN': token
            },
            type: 'POST',
            data: JSON.stringify({input:"", date: $("#driveDate").val()}),
            contentType: 'application/json',
            success: function (data, textStatus, jQxhr) {
                let icon = '<i class="fa-solid fa fa-location-arrow"></i>';
                for (let dataKey in data) {
                    let addressString = data[dataKey].addressString;
                    let addressStringActual = data[dataKey].addressString
                    if (data[dataKey].type == "HOME") {
                        icon = '<i class="fa-solid fa fa-home"></i>';
                        addressString += " (Hjemmeadresse)";
                    }
                    if (data[dataKey].type == "STANDARD") {
                        icon = '<i class="fa-solid fa fa-location-arrow"></i>';
                        addressString += (" ("+data[dataKey].description+")");
                    }
                    if (data[dataKey].type == "DHOME") {
                        icon = '<i class="fa-solid fa fa-home"></i>';
                        addressString += " (Afvigende hjemmeadresse)";
                    }
                    if (data[dataKey].type == "WORK") {
                        icon = '<i class="fa-solid fa fa-group"></i>';
                        addressString += " (Arbejdsadresse)";
                    }
                    if (data[dataKey].type == "DWORK") {
                        icon = '<i class="fa-solid fa fa-group"></i>';
                        addressString += " (Afvigende arbejdsadresse)";
                    }
                    if (data[dataKey].type == "ALTERNATIVE") {
                        addressString += " (" + data[dataKey].description + ")";
                    }
                    var resultItem = $('<a>')
                        .addClass('list-group-item list-group-item-action')
                        .attr('id', data[dataKey].id)
                        .html(`<span>${icon}</span> &nbsp;${addressString}<p style="display: none">${addressStringActual}</p>`)
                        .on('click', function () {
                            reportService.handleItemClick(data[dataKey], id);
                        });
                    resultsContainer.append(resultItem);
                }
            }
        });
    }

    this.handleItemClick = function (element, id) {
        $('#searchAddresses' + id).val("");
        if (id == "Start") {
            reportService.routeMarkerAddresses[0] = addressService.addressWash(element.addressString);
            $('#startField').val(element.addressString);
            if (reportService.ifReady() === true) {
                reportService.drawRoute(true, "reportService.handleItemClick");
            }
        } else if (id == "End") {
            reportService.markerEndAddress = addressService.addressWash(element.addressString);
            $('#endField').val(element.addressString);
            if (reportService.ifReady() === true) {
                reportService.drawRoute(true, "reportService.handleItemClick");
            }
        } else if (id !== undefined) {
            $('#waymarker' + id).val(element.addressString);
            reportService.updateWaypoint(id);
            if (reportService.ifReady() === true) {
                reportService.drawRoute(true, "reportService.handleItemClick");
            }
        }

        let resultContainer = $('#searchResults' + id).hide();
        resultContainer.empty();
    }

    this.handleKeyNavigation = function (event, id) {
        var resultsItems = $('#searchResults' + id + ' .list-group-item-action');
        // When we hide the results and press arrow, we initialise the search again
        if (resultsHidden && (event.key === 'ArrowDown' || event.keyCode == 40)) {
            reportService.onSearch(id);
            return;
        }
        if (resultsItems.length === 0) return;

        if (event.key === 'ArrowDown' || event.keyCode == 40) {
            event.preventDefault();
            if (currentIndex < resultsItems.length - 1) {
                currentIndex++;
                reportService.highlightItem(resultsItems);
            }
        } else if (event.key === 'ArrowUp' || event.keyCode == 38) {
            event.preventDefault();
            if (currentIndex > 0) {
                currentIndex--;
                reportService.highlightItem(resultsItems);
            }
        } else if ((event.key === 'Enter' || event.keyCode == 13) && currentIndex > -1) {
            event.preventDefault();
            let element = {
                "addressString": $(resultsItems[currentIndex]).children("p").text().trim()
            }
            reportService.handleItemClick(element, id);
        } else if (event.key === 'Escape' || event.keyCode == 27) {
            $('#searchResults' + id).hide();
            resultsHidden = true;
            currentIndex = -1;
        }
    }

    this.highlightItem = function (resultsItems) {
        resultsItems.removeClass('active');
        $(resultsItems[currentIndex]).addClass('active');
    }

    this.triggerSearchOnClick = function (id) {
        reportService.onSearch(id);
    }

    this.calcTypeChange = function(calcTypeValue, zeroFields = true) {
        if(calcTypeValue === "1") {
            $("#calculationTypeCalculatedFields").attr('hidden', true);
            $("#calculationTypeReadFields").attr('hidden', false);

            if (zeroFields) {
                reportService.clearRoute();
                $("#distanceFieldKm").val(0);
                $("#distanceFieldKmRead").val(0);
                $("#rawDistanceFieldKm").val(0);
            }
        } else {
            $("#calculationTypeCalculatedFields").attr('hidden', false);
            $("#calculationTypeReadFields").attr('hidden', true);
        }
    }
    
    this.closeAll = function () {
        $('.resultContainer').hide();
    }

    this.isValidDateFormat = function(dateString) {
        // Format check: YYYY-MM-DD
        const regex = /^(\d{4})-(\d{2})-(\d{2})$/;
        const match = dateString.match(regex);
        if (!match) {
            toastr.warning("Formateringen af kørselsdato er forkert, benyt venligst datovælgeren igen");
            return false;
        }

        const year = parseInt(match[1], 10);
        const month = parseInt(match[2], 10);
        const day = parseInt(match[3], 10);

        // Year must be between 1500 and 9999
        if (year < 1500 || year > 9999) {
            toastr.warning("Kørselsdato er forkert, benyt venligst datovælgeren igen");
            return false;
        }

        // Use numeric Date constructor to avoid iOS/Safari issues
        const date = new Date(year, month - 1, day);

        return (
            date.getFullYear() === year &&
            date.getMonth() + 1 === month &&
            date.getDate() === day
        );
    }
}
