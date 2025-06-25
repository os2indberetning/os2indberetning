var token = $("meta[name='_csrf']").attr("content");

var standardAddressService;
var standardAddressMap;
var addressService;

$(document).ready(function() {
    standardAddressService = new StandardAddressService();
    addressService = new AddressService();
    standardAddressService.init();
});


function StandardAddressService() {
    var addressMarkerAddress;
    var addressMarkerCoord;
    var addressMarker;

    this.init = function() {
        this.loadFragment();
    }

    this.loadFragment = function() {
        $('#standardAddressFragmentDiv').load('/admin/standardAddressFragment', this.initFrag);

    }

    this.initFrag = function() {
        var standardAddressTable = $('#standardAddressTable').DataTable({
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

        standardAddressMap = addressService.mapInit([$("#standardAddressMap").data('lat'), $("#standardAddressMap").data('lng')], "standardAddressMap");

        this.addressMarker = L.marker([0, 0]);

        $("#standardAddressField").on('keyup', function(e) {
            clearTimeout(typingTimer);
            // Debounce logic
            typingTimer = setTimeout(() => {
                if(e.key === 'Enter' || e.keyCode === 13 ) {
                    standardAddressService.mark();
                }
                else if(this.value.length > 2) {
                    var autocompleteList = addressService.addressAutocomplete(this.value);
                    const suggestions = document.getElementById("standardAddressFieldList");
                    suggestions.innerHTML = '';

                    autocompleteList.forEach(function(sugg) {
                        var option = document.createElement('option');
                        option.value = sugg.tekst
                        suggestions.appendChild(option);
                    });
                }
                else {
                    $("#standardAddressFieldList").innerHTML = '';
                }
            }, typingDelay);
        });

        $("#standardAddressField").on('focusout change', function(e) {
            standardAddressService.mark();
        });

        $.each($('.input-filter', standardAddressTable.table().footer()), function() {
            var column = standardAddressTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if(column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });

        $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
          //not the most elegant solutions, but forces map.invalidateSize, when tab is actual shown (and given size)
          //without this map doesn't correctly load
          if($(e.target).attr("href") === "#standardAddresses") { standardAddressMap.invalidateSize(); }
        });
    }

    this.mark = function() {
        if (this.addressMarker) { standardAddressMap.removeLayer(this.addressMarker); }
        this.addressMarkerAddress = addressService.addressWash($("#standardAddressField").val());
        this.addressMarkerCoord = addressService.addressToLtLg(this.addressMarkerAddress);
        this.addressMarker = L.marker([this.addressMarkerCoord[0].lat, this.addressMarkerCoord[0].lng],{draggable: true, autoPan: true}).addTo(standardAddressMap);
        standardAddressMap.flyTo([this.addressMarkerCoord[0].lat, this.addressMarkerCoord[0].lng], 16, {animate: false});

        this.addressMarker.on('dragend', function(e) {
            this.addressMarkerCoord = this.getLatLng();
            standardAddressService.addressMarkerAddress = addressService.latLngToAddress(this.addressMarkerCoord);
            $("#standardAddressField").val(addressService.addressString(standardAddressService.addressMarkerAddress));
        })
    }

    this.del = function(element) {
        swal({
            title: 'Slet adresse',
            text: "er du sikker?",
            showCancelButton: true,
            confirmButtonColor: "red",
            confirmButtonText: "Ja",
            cancelButtonText: "Nej",
            closeOnConfirm: true,
            closeOnCancel: true
        },
            function(isConfirm) {
                if(isConfirm) {
                    $.ajax({
                        method : "POST",
                        url: "rest/standardAddress/delete/" + element.dataset.id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            toastr.success("Adresse slettet");
                            standardAddressService.loadFragment();
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            errorResponse(jqXHR);
                        }
                    });
                }
            }
        )
        standardAddressService.loadFragment();
    }

    this.edit = function(element) {
        $("#idField").val(element.dataset.id);
        $("#editButton").removeAttr("hidden");
        $("#editButton").removeAttr("disabled");
        $("#saveButton").attr("hidden", "hidden");
        $("#saveButton").attr("disabled", "");
        if (this.addressMarker) { standardAddressMap.removeLayer(this.addressMarker); }
        this.addressMarker = L.marker([element.dataset.lat, element.dataset.lng],{draggable: true, autoPan: true, title: element.dataset.description}).addTo(standardAddressMap);
        this.addressMarkerCoord = this.addressMarker.getLatLng();
        standardAddressMap.flyTo([this.addressMarkerCoord.lat, this.addressMarkerCoord.lng], 16, {animate: false});
        standardAddressService.addressMarkerAddress = addressService.latLngToAddress(this.addressMarkerCoord);
        $("#standardAddressField").val(addressService.addressString(standardAddressService.addressMarkerAddress));
        $("#standardDescriptionField").val(element.dataset.description);

        this.addressMarker.on('dragend', function(e) {
            this.addressMarkerCoord = this.getLatLng();
            standardAddressService.addressMarkerAddress = addressService.latLngToAddress(this.addressMarkerCoord);
            $("#standardAddressField").val(addressService.addressString(standardAddressService.addressMarkerAddress));
        })
    }

    this.editSave = function() {
        var parsedAddr = standardAddressService.addressMarkerAddress;
        var body = JSON.stringify({
            "id": $("#idField").val(),
            "description": $("#standardDescriptionField").val() || "",
            "lat": standardAddressService.addressMarkerCoord.lat || "",
            "lng": standardAddressService.addressMarkerCoord.lng || "",
            "lon": standardAddressService.addressMarkerCoord.lng || "",
            "house_number": parsedAddr.husnr || "",
            "road": parsedAddr.vejstykke.navn || "",
            "town": parsedAddr.postnummer.navn || "",
            "postcode": parsedAddr.postnummer.nr || ""
        });

        $.ajax({
            method : "POST",
            url : "rest/standardAddress/edit",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: body,
            success: function(data, textStatus, jqXHR) {
                toastr.success("adresse ændret");
                standardAddressService.loadFragment();
            },
            error: function(kqXHR, textStatus, errorThrown) {
                toastr.warning("Der er opstået en teknisk fejl");
            }
        })
        this.loadFragment();
    }

    this.save = function() {
        if(standardAddressService.addressMarkerAddress === undefined) { standardAddressService.mark(); }
        var parsedAddr = standardAddressService.addressMarkerAddress;
        var body = JSON.stringify({
            "description": $("#standardDescriptionField").val() || "",
            "lat": standardAddressService.addressMarkerCoord[0].lat || "",
            "lng": standardAddressService.addressMarkerCoord[0].lng || "",
            "lon": standardAddressService.addressMarkerCoord[0].lng || "",
            "house_number": parsedAddr.husnr || "",
            "road": parsedAddr.vejstykke.navn || "",
            "town": parsedAddr.postnummer.navn || "",
            "postcode": parsedAddr.postnummer.nr || ""
        });

        $.ajax({
            method : "POST",
            url : "rest/standardAddress/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: body,
            success: function(data, textStatus, jqXHR) {
                toastr.success("adresse gemt");
                standardAddressService.loadFragment();
            },
            error: function(kqXHR, textStatus, errorThrown) {
                toastr.warning("Der er opstået en teknisk fejl");
            }
        })
    }

    this.primeAddress = function(element) {
        $.ajax({
            method : "POST",
            url: "rest/standardAddress/prime/" + element.dataset.id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
                },
                success: function(data, textStatus, jqXHR) {
                    toastr.success("Adressen er sat som primær");
                    standardAddressService.loadFragment();
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    toastr.warning("Der opstod en teknisk fejl");
                }
        });
    }
}