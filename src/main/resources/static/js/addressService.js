var token = $("meta[name='_csrf']").attr("content");

var typingTimer;
var typingDelay = 350;

function AddressService() {
    this.mapInit = function(startView, mapId = 'map') {
        let map = L.map(mapId).setView(startView, 14);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          }).addTo(map);
        return map;
    }

    this.get = function(url) { //basic http get helper function
        var request = new XMLHttpRequest();
        request.open("GET", url, false);
        request.send(null);
        return request.responseText;
    }

    this.addressAutocomplete = function(unComplete) {
        return JSON.parse(this.get('https://api.dataforsyningen.dk/adgangsadresser/autocomplete?q=' + unComplete));
    }

    this.addressWash = function(add) { //get and parse from openstreetmap
        var address = JSON.parse(this.get('https://api.dataforsyningen.dk/datavask/adresser?betegnelse=' + add));
        var vAdd = address.resultater[0].vaskeresultat.parsetadresse;
        //makes relevant information more easily available
        return {
            "vejstykke": {
                "navn": vAdd.vejnavn
            },
            "husnr": vAdd.husnr,
            "postnummer": {
                "nr": vAdd.postnr,
                "navn": vAdd.postnrnavn
            }
        };
    }

    var addressLatLngErrorTimer;
    this.addressToLtLg = function(addr) {
        let ltLgJSON = {};
        $.ajax({
            method: "GET",
            url: "/routeapi/addressToLatLng/" + addr.vejstykke.navn + ',' + addr.husnr + ',' + addr.postnummer.nr,
            async: false,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function(data, textStatus, jqXHR) {
                ltLgJSON.lat = data.lat;
                ltLgJSON.lon = data.lon;
                ltLgJSON.lng = data.lon;
            },
            error: function(jqXHR, textStatus, errorThrown) {
                if(jqXHR.status === 400) {
                    return;
                }

                clearTimeout(addressLatLngErrorTimer);
                addressLatLngErrorTimer = setTimeout(() => {
                    toastr.warning(jqXHR.responseText ? jqXHR.responseText : "Der er opstået en teknisk fejl");
                }, 250);
            }
        })
        return ltLgJSON;
    }

    var latLngAddressErrorTimer;
    this.latLngToAddress = function(coord) {
        let responseData;
        $.ajax({
            method: "GET",
            url: "/routeapi/latLngToAddress/" + coord.lat + ',' + coord.lng,
            async: false,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function(data, textStatus, jqXHR) {
                responseData = data;
            },
            error: function(jqXHR, textStatus, errorThrown) {
                clearTimeout(latLngAddressErrorTimer);
                latLngAddressErrorTimer = setTimeout(() => {
                    toastr.warning(jqXHR.responseText ? jqXHR.responseText : "Der er opstået en teknisk fejl");
                }, 250);
            }
        });

        return responseData;
    }

    // a failed address lookup yields undefined, so callers must not be able to break on it
    this.addressString = function(parsedJson) {
        if (!parsedJson) { return ""; }
        var addString = "";
        if (parsedJson.vejstykke && parsedJson.vejstykke.navn != null) { addString += parsedJson.vejstykke.navn + ' '; }
        if (parsedJson.husnr != null) { addString += parsedJson.husnr + ', ' }
        if (parsedJson.postnummer && parsedJson.postnummer.nr != null) { addString += parsedJson.postnummer.nr + ' '; }
        if (parsedJson.postnummer && parsedJson.postnummer.navn != null) { addString += parsedJson.postnummer.navn; }
        return addString;
    }


    this.createAddressList = function() {
        let jsonAddressList = '[';
        let rm = [];
        let rma = [];

        rm = rm.concat(reportService.routeMarkers)
        rm.push(reportService.markerEnd);


        rma = rma.concat(reportService.routeMarkerAddresses);
        rma.push(reportService.markerEndAddress);

        for(let i = 0; i < rm.length; i++) {
            var addr = rma[i];
            if(addr === undefined) {
                continue;
            }

            var coord = rm[i].getLatLng();
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

    let deltaDistanceTimer;
    const deltaDistanceTimerDelay = 200;
    this.getDeltaDistance = function () {
        if ($('#calculationType').val() == "2") {
            $('#deltaDistance').val(0).change();
        }
        clearTimeout(deltaDistanceTimer);
        deltaDistanceTimer = setTimeout(() => {
            var data = {
                "calculationType": $('#calculationType').val(),
                "employmentId": $('#employmentSelect').val(),
                "driveDate": $('#driveDate').val(),
                "addressRecords": reportService.routeMarkers && reportService.routeMarkers.length  ? addressService.createAddressList() : [],
                "distance": $("#calculationType").val() === "1" ? $("#distanceFieldKmRead").val() : $("#rawDistanceFieldKm").val(),
                "roundTrip": $("#roundTripCheck").prop('checked'),
                "startsOrEndsHomeForRead": $('#startsAtHome').prop("checked") || $('#endsAtHome').prop("checked"),
                "maxDistanceToSubtract": $('#maxDistanceToSubtract').val() === undefined ? 0 : $('#maxDistanceToSubtract').val(),
                "alreadySubtracted": $('#subtractedBySavedReports').val() === undefined ? 0 : $('#subtractedBySavedReports').val(),
                "fourKmWithdrawn": $('#fourKmWithdrawnBySavedReports').val() === undefined ? 0 : $('#fourKmWithdrawnBySavedReports').val(),
                "fourKmRuleEnabled": $('#fourKmCheck').prop("checked"),
                "homeToBorderDistance": $('#fourKmDistance').val() === undefined ? 0 : $('#fourKmDistance').val()
            }

            $.ajax({
                method: "POST",
                url: "/rest/report/calculateDeltaDistance",
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                data: JSON.stringify(data),
                success: function(data, textStatus, jqXHR) {
                    $('#deltaDistance').val(Math.round((data + Number.EPSILON) * 100) / 100);
                },
                error: function(jqXHR, textStatus, errorThrown) {}
            })
        }, deltaDistanceTimerDelay);
    }

    this.route = function(coordList, lastmarker=reportService.markerEnd, functionName) {
        var request = new XMLHttpRequest();
        request.open("POST", "/routeapi/route/", false);
        request.setRequestHeader('X-CSRF-TOKEN', token);
        request.setRequestHeader("content-type", "application/json");

        let coordString = "";
        for(let i = 0; i < coordList.length; i++) {
            if(coordList[i] === undefined) {
                continue;
            }

            coordString += '[' + coordList[i].getLatLng().lng + ',' + coordList[i].getLatLng().lat + '],';
        }
        var endpoint = lastmarker.getLatLng();
        coordString += '[' + endpoint.lng + ',' + endpoint.lat + ']';

        let pref = $('#routeCalculationPreference').val();
        if (!pref) {
            pref = "recommended"
        }
        request.send(JSON.stringify({
            coordinates: coordString,
            routeCalculationPreference: pref,
            functionName: functionName
        }));

        result = JSON.parse(request.responseText);
        if (result.error) {
            switch(result.error) {
                case "NoRoute":
                    toastr.warning("Ingen rute i mellem de 2 punkter");
                case "2010":
                    toastr.warning("Ingen rute i mellem de 2 punkter");
                    return false;
                case "502":
                    toastr.warning("Ruteservice ikke tilgængeligt. Prøv igen senere");
                    return false;
                case "500":
                    toastr.warning("Ruteservicen tog for langt tid til at svare. Vent venligst 2 minutter og prøv igen");
                    return false;
                default:
                    toastr.warning("Uforventet fejl: " + result.error);
                    return false;
            }
        }

        return result;
    }

}
