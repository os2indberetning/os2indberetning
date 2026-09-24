var token = $("meta[name='_csrf']").attr("content");

var personalAddressService;
var personalAddressMap;
var addressService;

$(document).ready(function() {
    personalAddressService = new PersonalAddressService();
    addressService = new AddressService();
    personalAddressService.init();
});


function PersonalAddressService() {
    var addressMarkerAddress;
    var addressMarkerCoord;
    var addressMarker;

    this.init = function() {
        this.loadFragment();
    }

    this.loadFragment = function() {
        $('#addressFragmentDiv').load('/settings/addressFragment', this.initFrag);
    }

    this.initFrag = function() {
        $('[data-toggle="tooltip"]').tooltip();

        var twoMonths = new Date();
        twoMonths.setMonth(twoMonths.getMonth()-2);
        twoMonths = twoMonths.toISOString().split('T')[0];
        var today = new Date().toISOString().split('T')[0];
        $(".addressStartDateInput").each(function() {
            $(this)[0].setAttribute('max', today);
            $(this)[0].setAttribute('min', twoMonths);
        });

        var addressTable = $('#addressTable').DataTable({
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

        personalAddressMap = addressService.mapInit([$("#personalAddressMap").data('lat'), $("#personalAddressMap").data('lng')], "personalAddressMap");

        this.addressMarker = L.marker([0, 0]);
        personalAddressMap.invalidateSize();

        $(".addressinput").on('keyup', function(e) {
            clearTimeout(typingTimer);
            // Debounce logic
            typingTimer = setTimeout(() => {
                if(e.key === 'Enter' || e.keyCode === 13 ) {
                    personalAddressService.mark(this);
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

        $(".addressinput").on('focusout change', function(e) {
            personalAddressService.mark(this);
        });

        $.each($('.input-filter', addressTable.table().footer()), function() {
            var column = addressTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if(column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });

        $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
          //not the most elegant solutions, but forces map.invalidateSize, when tab is actual shown (and given size)
          //without this map doesn't correctly load
          if($(e.target).attr("href") === "#addresses") { personalAddressMap.invalidateSize(); }
        });
    }

    this.mark = function(field) {
        if (this.addressMarker) { personalAddressMap.removeLayer(this.addressMarker); }
        this.addressMarkerAddress = addressService.addressWash($(field).val())
        this.addressMarkerCoord = addressService.addressToLtLg(this.addressMarkerAddress);
        this.addressMarker = L.marker([this.addressMarkerCoord.lat, this.addressMarkerCoord.lng],{draggable: true, autoPan: true}).addTo(personalAddressMap);
        personalAddressMap.flyTo([this.addressMarkerCoord.lat, this.addressMarkerCoord.lng], 16, {animate: false});

        this.addressMarker.on('dragend', function(e) {
            this.addressMarkerCoord = this.getLatLng();
            this.addressMarkerAddress = addressService.latLngToAddress(this.addressMarkerCoord);
            $(field).val(addressService.addressString(this.addressMarkerAddress));

        })
    }

    this.del = function(element) {
        swal({
            title: element.dataset.title ? element.dataset.title : "Vil du slette adressen?",
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
                        url: "rest/personalAddress/delete/" + element.dataset.id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            toastr.success("Adresse slettet");
                            personalAddressService.init();
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            errorResponse(jqXHR);
                        }
                    });
                }
            }
        );
    }

    this.edit = function(element) {
        $("#editButton").removeAttr("hidden");
        $("#editButton").removeAttr("disabled");
        $("#saveButton").attr("hidden", "hidden");
        $("#saveButton").attr("disabled", "");
        $("#editButton").attr("data-id", element.dataset.id);
        $("#saveButtton").attr("data-id", element.dataset.id);
        if (this.addressMarker) {personalAddressMap.removeLayer(this.addressMarker); }
        this.addressMarker = L.marker([element.dataset.lat, element.dataset.lng],{draggable: true, autoPan: true, title: element.dataset.description}).addTo(personalAddressMap);
        this.addressMarkerCoord = this.addressMarker.getLatLng();
        personalAddressMap.flyTo([this.addressMarkerCoord.lat, this.addressMarkerCoord.lng], 16, {animate: false});
        this.addressMarkerAddress = addressService.latLngToAddress(this.addressMarkerCoord);
        $("#addressField").val(addressService.addressString(this.addressMarkerAddress));
        $("#descriptionField").val(element.dataset.description);

        this.addressMarker.on('dragend', function(e) {
            this.addressMarkerCoord = this.getLatLng();
            this.addressMarkerAddress = addressService.latLngToAddress(this.addressMarkerCoord);
            $("#addressField").val(addressService.addressString(this.addressMarkerAddress));
        })
    }

    this.editSave = function(element) {
        personalAddressService.mark(`#${element.dataset.field}`);
        var parsedAddr = personalAddressService.addressMarkerAddress;
        var parsed = parsedAddr.address;
        var body = {
            "id" : element.dataset.id || null,
            "parentId" : element.dataset.parentid || null,
            "description": element.dataset.field == "addressField" ? $("#descriptionField").val() : null,
            "type": element.dataset.type || "ALTERNATIVE",
            "lat": this.addressMarkerCoord.lat || "",
            "lng": this.addressMarkerCoord.lng || "",
            "lon": this.addressMarkerCoord.lng || "",
            "house_number": parsedAddr.husnr || "",
            "road": parsedAddr.vejstykke.navn || "",
            "town": parsedAddr.postnummer.navn || "",
            "postcode": parsedAddr.postnummer.nr || "",
            "deviatingKm": $("#dworkKmField").val()
        };

        if (element.dataset.parentid && body.type === "DWORK") {
           body.startDate = $("#deviatingWorkAddressStart" + element.dataset.parentid).val();
        }
        if (element.dataset.parentid && body.type === "DHOME") {
           body.startDate = $("#deviatingHomeAddressStart").val();
        }

        $.ajax({
            method : "POST",
            url : "rest/personalAddress/edit",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: JSON.stringify(body),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Adresse ændret");

                if(data) {
                    toastr.success(data + " indberetninger vil blive genberegnet pga. adresseændring");
                }
                personalAddressService.loadFragment();
            },
            error: function(kqXHR, textStatus, errorThrown) {
                toastr.warning("Der er opstået en teknisk fejl");
            }
        })
    }

    this.editJump = function(jumpPoint) {
        document.getElementById(jumpPoint + 'Head').scrollIntoView({behavior: 'smooth'});
        personalAddressService.mark(`#d${jumpPoint}Field`);
    }

    this.save = function(element) {
        personalAddressService.mark(`#${element.dataset.field}`);
        var parsedAddr = personalAddressService.addressMarkerAddress;
        var body = {
            "parentId": element.dataset.parentid || null,
            "description": element.dataset.field == "addressField" ? $("#descriptionField").val() : null,
            "type": element.dataset.type || "ALTERNATIVE",
            "lat": this.addressMarkerCoord.lat || "",
            "lng": this.addressMarkerCoord.lng || "",
            "lon": this.addressMarkerCoord.lng || "",
            "house_number": parsedAddr.husnr || "",
            "road": parsedAddr.vejstykke.navn || "",
            "town": parsedAddr.postnummer.navn || "",
            "postcode": parsedAddr.postnummer.nr || "",
            "deviatingKm": $("#dworkKmField").val()
        };

        if (element.dataset.parentid && body.type === "DWORK") {
           body.startDate = $("#deviatingWorkAddressStart" + element.dataset.parentid).val();
        }
        if (element.dataset.parentid && body.type === "DHOME") {
           body.startDate = $("#deviatingHomeAddressStart").val();
        }

        $.ajax({
            method : "POST",
            url : "rest/personalAddress/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: JSON.stringify(body),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Adresse gemt");

                if(data) {
                    toastr.success(data + " indberetninger vil blive genberegnet pga. adresseændring");
                }
                personalAddressService.loadFragment();
            },
            error: function(kqXHR, textStatus, errorThrown) {
                toastr.warning("Der er opstået en teknisk fejl");
            }
        })
    }
}
