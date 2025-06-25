var token = $("meta[name='_csrf']").attr("content");

var personalRouteService;
var addressService;
var mapRoute

$(document).ready(function() {
    personalRouteService = new PersonalRouteService();
    addressService = new AddressService();
    personalRouteService.init();
});

function PersonalRouteService() {
    var routeMarkers;
    var routeMarkerCoords;
    var routeMarkerAddresses;
    var route;
    var editMode;
    var id;
    var readyToPost = false;


    this.init = function () {
        this.loadFragment();
    }

    this.loadFragment = function() {
        $('#personalRoutesFragmentDiv').load('/settings/personalRoutesFragment', this.initFrag);

        personalRouteService.routeMarkers = [];
        personalRouteService.routeMarkerAddresses = [];
        personalRouteService.routeMarkerCoords = [];
    }

    this.initFrag = function() {
        var rateTable = $('#personalRoutesTable').DataTable({
            "destroy": true,
            "bSort": true,
            "order": [[0, 'desc']],
            "paging": true,
            "pageLength": 10,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "language": {
                "search": "Søg",
                "lengthMenu": "_MENU_ rækker per side",
                "info": "Viser _START_ til _END_ af _TOTAL_ rækker",
                "zeroRecords": "Ingen data...",
                "infoEmpty": "Henter data...",
                "infoFiltered": "(ud af _MAX_ rækker)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                }
            }
        });
        $.each($('.input-filter', rateTable.table().footer()), function() {
            var column = rateTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if(column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });

        mapRoute = L.map('mapRoute').setView([$("#mapRoute").data('lat'), $("#mapRoute").data('lng')], 14);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: false
        }).addTo(mapRoute);

        $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
             if($(e.target).attr("href") === "#personalRoutes") { mapRoute.invalidateSize(); }
        });
    }


    this.openCreateModal = function (obj) {
        $("#waypointDiv").html('');
        personalRouteService.clearRoute();
        personalRouteService.routeMarkerCoords = [];
        personalRouteService.routeMarkerAddresses = [];
        personalRouteService.routeMarkers = [];

        if ($(obj).data("id")) {
            editMode = true;

            id = $(obj).data("id");
            $('#descriptionInput').val($(obj).data("description"));

            $('#street'+ id + " li").each(function (value, element) {
                if (value == 0) {
                    $('#startField').val($(this).text());
                }
                if (value == $('#street' + id).children().length - 1) {
                    $('#endField').val($(this).text());
                }
                if (value != 0 && value != $('#street' + id).children().length - 1) {
                    var waypoint =
                        '<div class="form-group row">' +
                        '    <label class="col-sm-3 col-form-label" style="text-align:left;">' +
                        '        <p style="text-align:left;">Via</p>' +
                        '        <label class="col-form-row" style="text-align:left;">' +
                        '           <i class="fa fa-trash-o" data-id="' + value + '" onclick="personalRouteService.deleteWaymarker(this)" title="Slet"></i>' +
                        '           <i class="fa fa-arrow-up" data-id="' + value + '" onclick="personalRouteService.moveWaymarker(true, this)" title="Flyt op"></i>' +
                        '           <i class="fa fa-arrow-down" data-id="' + value + '" onclick="personalRouteService.moveWaymarker(false, this)" title="Flyt ned"></i>' +
                        '       </label>' +
                        '    </label>' +
                        '    <div class="col-sm-9">' +
                        '        <input class="addressinput form-control" type="text" id="waymarker' + value + '"  placeholder="Indtast adresse her" data-listid="' + value + '" autocomplete="off" list="waymarker' + value + 'List">' +
                        '        <dataList id="waymarker' + value + 'List"></dataList>' +
                        '    </div>' +
                        '</div>';
                    $("#waypointDiv").append(waypoint);
                    $('#waymarker' + value).val(value);


                    personalRouteService.routeMarkerAddresses[value] = addressService.addressWash($(this).text());
                    // personalRouteService.routeMarkerAddresses[value] = $(element).text();

                    // var newLatLng = addressService.addressToLtLg(personalRouteService.routeMarkerAddresses[value]);
                    // newLatLng.lat = newLatLng[0].lat;
                    // newLatLng.lng = newLatLng[0].lng;
                    var newLatLng = {};
                    newLatLng.lat = $(element).data("lat");
                    newLatLng.lng = $(element).data("lng");

                    personalRouteService.routeMarkerCoords[value] = newLatLng;
                    personalRouteService.routeMarkers[value] = L.marker([newLatLng.lat, newLatLng.lng],{draggable: true, autoPan: true, title: $(element).text(), listId: value}).addTo(mapRoute);
                    // $('#waymarker' + value).val(addressService.addressString(personalRouteService.routeMarkerAddresses[value]));
                    $('#waymarker' + value).val($(element).text());
                    $('#startSelect option').clone().appendTo('#waymarker' + value + 'Select');
                }
            });

            // Makes sure they are set into the array they need to be in for routing etc
            $(".waymarker").each(function(index, element) {
                personalRouteService.routeMarkerAddresses[index + 1] = addressService.addressWash($(element).val());
                let waymark = addressService.addressToLtLg(personalRouteService.routeMarkerAddresses[index + 1]);
                personalRouteService.routeMarkers[index + 1] = L.marker([waymark[0].lat, waymark[0].lng],{draggable: true, autoPan: true}).addTo(mapRoute)

            });
            personalRouteService.drawRoute(true, "personalRouteService.openCreateModal");
        }
        else {
            id = 0;
            editMode = false;
            personalRouteService.clearRoute();
            $(".addressinput").val("");
            $('#distanceFieldKm').val("");
            $('#rawDistanceFieldKm').val("");
            $('#descriptionInput').val("");
        }

        $('#createModal').modal("show");

        $('#descriptionInput:input:enabled:visible:first').focus();

        mapRoute.invalidateSize();

        $(".addressinput").on('keyup', function(e) {
            clearTimeout(typingTimer);
            // Debounce logic
            typingTimer = setTimeout(() => {
                if(e.key === 'Enter' || e.keyCode === 13 ) {
                    if($('#startField').val().length != 0 && $('#endField').val().length != 0) { personalRouteService.drawRoute("personalRouteService.openCreateModal"); }
                }
                if (this.value.length > 2) {
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

        $(".addressinput").on('focusout change', function(e) {
            if($('#startField').val().length != 0 && $('#endField').val().length != 0) { personalRouteService.drawRoute("personalRouteService.openCreateModal"); }
        });

        if ($("#editMapRoute").val()) {
            let routeCoordinates = JSON.parse($("#editMapRoute").val())
            if (routeCoordinates) {
                let routeLine = L.geoJSON(routeCoordinates.geometry).addTo(mapRoute);
                mapRoute.fitBounds(this.routeLine.getBounds());
            }

            $(".waymarker").each(function( index, element) {
                personalRouteService.routeMarkerAddresses[index + 1] = addressService.addressWash($(element).val());
                let waymark = addressService.addressToLtLg(personalRouteService.routeMarkerAddresses[index + 1].address);
                personalRouteService.routeMarkers[index + 1] = L.marker([waymark[0].lat, waymark[0].lng],{draggable: true, autoPan: true}).addTo(mapRoute)

            });
            // not sure if this code is ever called
            personalRouteService.drawRoute(true, "personalRouteService.openCreateModal (edit)");
            personalRouteService.drawRoute("personalRouteService.openCreateModal (edit)");
        }

        mapRoute.fitBounds(this.routeLine.getBounds());
    }

    this.drawRoute = function(drawInitialAgain=false, functionName) {
        var failed = false;
        if (personalRouteService.routeMarkers.length < 2 || drawInitialAgain === true) {
            failed = (this.drawInitialRoute()) === false;
        } else {
            this.additionalWaymarkers();
        }

        if (failed) {
            readyToPost = false;
            return;
        }

        $("path").remove();
        this.route = addressService.route(personalRouteService.routeMarkers, personalRouteService.markerEnd, functionName);
        // debugger
        this.routeLine = L.geoJSON(this.route.geometry,{onEachFeature: (_, layer) => layer.on('click', this.onRouteClick)}).addTo(mapRoute)
        if (this.route === false) {
            readyToPost = false;
            return;
        }
        else {
            mapRoute.fitBounds(this.routeLine.getBounds());

            let distanceFieldKM = Math.round(personalRouteService.route.distance/10+Number.EPSILON)/100;
            if (Number.isNaN(distanceFieldKM)) {
                $('#distanceFieldKm').val(0);
                $('#rawDistanceFieldKm').val(0);
            }
            else {
                $("#distanceFieldKm").val(distanceFieldKM);
                $('#rawDistanceFieldKm').val(distanceFieldKM);
            }
            readyToPost = true;
        }
    }

    this.drawInitialRoute = function() {
        if (personalRouteService.routeMarkers[0]) { mapRoute.removeLayer(personalRouteService.routeMarkers[0]); }
        if (personalRouteService.markerEnd) { mapRoute.removeLayer(this.markerEnd); }
        if (personalRouteService.routeLine) { mapRoute.removeLayer(this.routeLine); }

        personalRouteService.routeMarkerAddresses[0] = addressService.addressWash($("#startField").val());
        var a = personalRouteService.routeMarkerAddresses[0];
        if (!a.vejstykke || !a.vejstykke.navn || !a.husnr || !a.postnummer || !a.postnummer.nr || !a.postnummer.navn) {
            return false;
        }

        var pointStart = addressService.addressToLtLg(personalRouteService.routeMarkerAddresses[0]);
        personalRouteService.markerEndAddress = addressService.addressWash($("#endField").val());
        var b = personalRouteService.markerEndAddress;
        if (!b.vejstykke || !b.vejstykke.navn || !b.husnr || !b.postnummer || !b.postnummer.nr || !b.postnummer.navn) {
            return false;
        }
        var pointEnd = addressService.addressToLtLg(personalRouteService.markerEndAddress);

        personalRouteService.routeMarkers[0] = L.marker([pointStart[0].lat, pointStart[0].lng],{draggable: true, autoPan: true, title: $("#startField").val()}).addTo(mapRoute)
        personalRouteService.markerEnd = L.marker([pointEnd[0].lat, pointEnd[0].lng],{draggable: true, autoPan: true, title: $("#endField").val()}).addTo(mapRoute)

        personalRouteService.routeMarkers[0].on('dragend', function(e) {
            personalRouteService.routeMarkerCoords[0] = this.getLatLng();
            personalRouteService.routeMarkerAddresses[0] = addressService.latLngToAddress(personalRouteService.routeMarkerCoords[0]);
            $("#startField").val(addressService.addressString(personalRouteService.routeMarkerAddresses[0]));
            personalRouteService.drawRoute("personalRouteService.drawInitialRoute");
        });
        this.markerEnd.on('dragend', function(e) {
            personalRouteService.markerEndAddress = addressService.latLngToAddress(this.getLatLng());
            $("#endField").val(addressService.addressString(personalRouteService.markerEndAddress.address));
            personalRouteService.drawRoute("personalRouteService.drawInitialRoute");
        });
    }

    this.onRouteClick = function(event) {
        let listId = personalRouteService.routeMarkers.length;
        personalRouteService.routeMarkerCoords[listId] = event.latlng;
        personalRouteService.routeMarkerAddresses[listId] = addressService.latLngToAddress(personalRouteService.routeMarkerCoords[listId]);
        $('#waymarker' + listId).val(addressService.addressString(personalRouteService.routeMarkerAddresses[listId]));
        personalRouteService.routeMarkers[listId] = L.marker([event.latlng.lat, event.latlng.lng],{draggable: true, autoPan: true, title: addressService.addressString(personalRouteService.routeMarkerAddresses[listId]), listId: listId}).addTo(mapRoute);
        personalRouteService.drawRoute("personalRouteSerivce.onRouteClick");

        personalRouteService.routeMarkers[listId].on('dragend', function(e) {
            personalRouteService.routeMarkerCoords[this.options.listId] = this.getLatLng();
            personalRouteService.routeMarkerAddresses[this.options.listId] = addressService.latLngToAddress(personalRouteService.routeMarkerCoords[this.options.listId]);
            $('#waymarker' + this.options.listId).val(addressService.addressString(personalRouteService.routeMarkerAddresses[this.options.listId]));
            personalRouteService.updateWaypoint(this.options.listId);
            personalRouteService.drawRoute("personalRouteSerivce.onRouteClick");
        });
    }

    this.updateWaypoint = function(i) {
        if (personalRouteService.routeMarkers[i]) { mapRoute.removeLayer(personalRouteService.routeMarkers[i]); }
        personalRouteService.routeMarkerAddresses[i] = addressService.addressWash($('#waymarker' + i).val());
        var wpLoc = addressService.addressToLtLg(personalRouteService.routeMarkerAddresses[i]);
        personalRouteService.routeMarkers[i] = L.marker([wpLoc[0].lat, wpLoc[0].lng],{draggable: true, autoPan: true, title: addressService.addressString(personalRouteService.routeMarkerAddresses[i]), listId: i}).addTo(mapRoute);

        personalRouteService.routeMarkers[i].on('dragend', function(e) {
            personalRouteService.routeMarkerCoords[this.options.listId] = this.getLatLng();
            personalRouteService.routeMarkerAddresses[this.options.listId] = addressService.latLngToAddress(personalRouteService.routeMarkerCoords[this.options.listId]);
            $('#waymarker' + this.options.listId).val(addressService.addressString(personalRouteService.routeMarkerAddresses[this.options.listId]));
        });
        personalRouteService.drawRoute("personalRouteSerivce.updateWaypoint");
    }

    this.deleteWaymarker = function(data) {
        var i = data.dataset.id;
        mapRoute.removeLayer(personalRouteService.routeMarkers[i]);
        personalRouteService.routeMarkers.splice(i, 1);
        if(personalRouteService.routeMarkers.length < 2) { this.additionalWaymarkers(); }
        personalRouteService.drawRoute("personalRouteSerivce.deleteWaymarker");
    }

    this.additionalWaymarkers = function() {
        $("#waypointDiv").html('');
        for (let i = 1; i < personalRouteService.routeMarkers.length; i++ ) {
            var waypoint =
                '<div class="form-group row" style="margin-bottom: 0px;">' +
                '    <label class="col-sm-3 col-form-label" style="text-align:left;">' +
                '        <p>Via &nbsp;' +
                '        <span>' +
                '           <i class="fa fa-trash-o" data-id="' + i + '" onclick="personalRouteService.deleteWaymarker(this)" title="Slet"></i>' +
                '           <i class="fa fa-arrow-up" data-id="' + i + '" onclick="personalRouteService.moveWaymarker(true, this)" title="Flyt op"></i>' +
                '           <i class="fa fa-arrow-down" data-id="' + i + '" onclick="personalRouteService.moveWaymarker(false, this)" title="Flyt ned"></i>' +
                '       </span>' +
                '        </p>' +
                '    </label>' +
                '    <div class="col-sm-9">' +
                '        <input class="addressinput form-control" type="text" id="waymarker' + i + '" value="' + (personalRouteService.routeMarkerAddresses[i] === undefined ? '' : addressService.addressString(personalRouteService.routeMarkerAddresses[i])) + '" data-listid="' + i + '" autocomplete="off" placeholder="Indtast addresse her" list="waymarker' + i + 'List">' +
                '        <dataList id="waymarker' + i + 'List"></dataList>' +
                '    </div>' +
                '</div>';

            $("#waypointDiv").append(waypoint);
            $('#startSelect option').clone().appendTo('#waymarker' + i + 'Select');


            personalRouteService.routeMarkers[i].on('dragend', function(e) {
                personalRouteService.routeMarkerCoords[i] = this.getLatLng();
                personalRouteService.routeMarkerAddresses[i] = addressService.latLngToAddress(personalRouteService.routeMarkerCoords[i]);

                $('#waymarker' + i).val(addressService.addressString(personalRouteService.routeMarkerAddresses[i]));
                personalRouteService.updateWaypoint(i);
            });

            $('#waymarker' + i).on('keyup', function(e) {
                clearTimeout(typingTimer);
                // Debounce logic
                typingTimer = setTimeout(() => {
                    if(e.key === 'Enter' || e.keyCode === 13 ) {
                        personalRouteService.updateWaypoint(this.dataset.listid);
                        if($('#startField').val().length != 0 && $('#endField').val().length != 0) { personalRouteService.drawRoute(); }
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

                if (isMatch) {
                    personalRouteService.updateWaypoint(this.dataset.listid);
                }
            });

            $('#waymarker' + i).on('focusout', function(e) {
                personalRouteService.updateWaypoint(this.dataset.listid);
            });
        }
    }

    this.moveWaymarker = function(moveEarlier, element) {
        let index = parseInt(element.dataset.id);  // Parse index from element's dataset

        var placeholderAddress = personalRouteService.routeMarkerAddresses[index];
        var placeholderCoords = personalRouteService.routeMarkerCoords[index];
        var placeholderMarker = personalRouteService.routeMarkers[index];

        if (moveEarlier === true) {
            personalRouteService.routeMarkerAddresses[index] = personalRouteService.routeMarkerAddresses[index - 1];
            personalRouteService.routeMarkerCoords[index] = personalRouteService.routeMarkerCoords[index - 1];
            personalRouteService.routeMarkers[index] = personalRouteService.routeMarkers[index - 1];
            personalRouteService.routeMarkerAddresses[index - 1] = placeholderAddress;
            personalRouteService.routeMarkerCoords[index - 1] = placeholderCoords;
            personalRouteService.routeMarkers[index - 1] = placeholderMarker;
            if (index - 1 === 0) {
                $("#startField").val(addressService.addressString(placeholderAddress));
            }
        } else {
            if (index === personalRouteService.routeMarkers.length - 1) {
                personalRouteService.routeMarkerAddresses[index] = personalRouteService.markerEndAddress;
                personalRouteService.routeMarkerCoords[index] = personalRouteService.markerEnd.getLatLng();
                personalRouteService.routeMarkers[index] = personalRouteService.markerEnd;
                personalRouteService.markerEndAddress = placeholderAddress;
                personalRouteService.markerEnd = placeholderMarker;
                $("#endField").val(addressService.addressString(personalRouteService.markerEndAddress));
            } else {
                personalRouteService.routeMarkerAddresses[index] = personalRouteService.routeMarkerAddresses[index + 1];
                personalRouteService.routeMarkerCoords[index] = personalRouteService.routeMarkerCoords[index + 1];
                personalRouteService.routeMarkers[index] = personalRouteService.routeMarkers[index + 1];
                personalRouteService.routeMarkerAddresses[index + 1] = placeholderAddress;
                personalRouteService.routeMarkerCoords[index + 1] = placeholderCoords;
                personalRouteService.routeMarkers[index + 1] = placeholderMarker;
            }
        }
        this.additionalWaymarkers();
        this.drawRoute(true, "personalRouteService.moveWaymarker");
    }


    this.onAddWaymarkerPressed = function(event) {
        let listId = personalRouteService.routeMarkers.length;
        if (listId == 0) {
            toastr.warning("Der opstod en teknisk fejl");
            return;
        }
        var newLatLng = personalRouteService.routeMarkers[personalRouteService.routeMarkers.length-1]._latlng;
        personalRouteService.routeMarkerCoords[listId] = newLatLng;
        personalRouteService.routeMarkerAddresses[listId] = undefined;
        personalRouteService.routeMarkers[listId] = L.marker([newLatLng.lat, newLatLng.lng],{draggable: true, autoPan: true, title: addressService.addressString(addressService.latLngToAddress(personalRouteService.routeMarkerCoords[listId])), listId: listId}).addTo(mapRoute);
        personalRouteService.drawRoute("personalRouteService.onAddWaymarkerPressed");

        personalRouteService.routeMarkers[listId].on('dragend', function(e) {
            personalRouteService.routeMarkerCoords[this.options.listId] = this.getLatLng();
            personalRouteService.routeMarkerAddresses[this.options.listId] = addressService.latLngToAddress(personalRouteService.routeMarkerCoords[this.options.listId]);
            $('#waymarker' + this.options.listId).val(addressService.addressString(personalRouteService.routeMarkerAddresses[this.options.listId]));
            personalRouteService.updateWaypoint(this.options.listId);
        });
    }

    this.createAddressList = function() {
        let jsonAddressList = '[';
        personalRouteService.routeMarkers.push(personalRouteService.markerEnd);
        personalRouteService.routeMarkerAddresses.push(personalRouteService.markerEndAddress);

        for(let i = 0; i < personalRouteService.routeMarkers.length; i++) {
            var addr = personalRouteService.routeMarkerAddresses[i];
            var coord = personalRouteService.routeMarkers[i].getLatLng();
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
        };
        jsonAddressList = jsonAddressList.substring(0,jsonAddressList.length-1);
        jsonAddressList += ']';
        return JSON.parse(jsonAddressList);
    }

    this.clearRoute = function() {
        for (let i = 0; i < personalRouteService.routeMarkers.length; i++) {
            if (personalRouteService.routeMarkers[i]) {
                mapRoute.removeLayer(personalRouteService.routeMarkers[i]);
            }
        }

        if (personalRouteService.markerEnd) {
            mapRoute.removeLayer(this.markerEnd);
        }

        if (personalRouteService.routeLine) {
            mapRoute.removeLayer(this.routeLine);
        }
    }

    this.closeModal = function () {
        $('#createModal').modal("hide");
    }

    this.latLngToAddress = function(coord) {
        parser = new DOMParser();
        var unparsedResp = this.get('/routeapi/latLngToAddress/' + coord.lat + ',' + coord.lng);
        var parsedResp = JSON.parse(unparsedResp);
        //nakes relevant information more easily available
        parsedResp.address = parsedResp;
        return parsedResp;
    }

    this.createRoute = function () {
        if (!readyToPost) {
            toastr.warning("Ruten er ikke bleven dannet ordentlig, prøv igen");
            return;
        }
        let geometry = this.route.coordinates;
        var url;
        var data = {
            "id": id,
            "description": $('#descriptionInput').val(),
            "startAddress": $('#startField').val(),
            "stopAddress": $('#endField').val(),
            "routeGeometry": geometry ? geometry.toString() : "",
            "addressList": personalRouteService.createAddressList()
        }

        if (!data.description) {
            toastr.warning("Beskrivelse skal være udfyldt");
            return;
        }

        if (editMode) {
            url = "rest/personalRoutes/edit"
        }
        else {
            url = "rest/personalRoutes/create"
        }
        $.ajax({
            method : "POST",
            url : url,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Rute gemt");
                $('#createModal').modal("hide");
                personalRouteService.init();
            },
            error: function(kqXHR, textStatus, errorThrown) {
                toastr.warning("Der er opstået en teknisk fejl");
            }
        });

    }

    this.openDeleteModal = function (obj) {
        let id = $(obj).data("id");

        swal({
            title: "Er du sikker at du vil slette denne personlige rute?",
            text: "Denne handling kan ikke fortrydes",
            type: "warning",
            showCancelButton: true,
            cancelButtonText: "Afbryd",
            confirmButtonColor: "#DD6B55",
            confirmButtonText: "slet",
            closeOnConfirm: false
        }, function () {
            swal.close();
            editMode = false;
            personalRouteService.delete(id);
        });

    }

    this.delete = function (data) {
        $.ajax({
            method : "POST",
            url : "rest/personalRoutes/delete?id=" + data,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            success: function(data, textStatus, jqXHR) {
                toastr.success("Rute slettet!");
                personalRouteService.init();
            },
            error: function(kqXHR, textStatus, errorThrown) {
                toastr.warning("Der er opstået en teknisk fejl");
            }
        });
    }





}