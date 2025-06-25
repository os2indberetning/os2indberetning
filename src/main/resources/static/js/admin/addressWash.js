var token = $("meta[name='_csrf']").attr("content");

var addressWashService;
var addressService;
var washingMap;
var marker;

$(document).ready(function() {
    addressWashService = new AddressWashService();
    addressService = new AddressService();
    addressWashService.init();
});

function AddressWashService() {
    this.init = function() {
        $("#addressWashFragmentDiv").load('/admin/addressWashFragment', this.initFrag);
    }

    this.initFrag = function() {
        $('[data-toggle="tooltip"]').tooltip({
            container: $('#dirtyModalBody')
        });

        $(".reading").prop("readonly", false);

        var addressWashTable = $('#addressWashTable').DataTable({
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

        washingMap = L.map('washingMap').setView([$("#washingMap").data('lat'), $("#washingMap").data('lng')], 14);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: false
            }).addTo(washingMap);
        this.marker = L.marker([0, 0]);

        $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
            //not the most elegant solutions, but forces map.invalidateSize, when tab is actual shown (and given size)
            //without this map doesn't correctly load
            if($(e.target).attr("href") === "#addressWash") { washingMap.invalidateSize(); }
        });

        $.each($('.input-filter', addressWashTable.table().footer()), function() {
            var column = addressWashTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if(column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });

        $.each($('.checkbox-filter', addressWashTable.table().footer()), function() {
            var column = addressWashTable.column($(this).index());

            $('input', this).on('change', function () {
                var query = $('input:checked',this.parentElement).map(function() { return this.checked; }).get().join('|');
                if (column.search() !== query) {
                    column.search((query && "false"), true).draw();
                }
            });
        });

        let checkBoxValue = localStorage.getItem(window.location.pathname + "_table_filter_addressWash");
        if (checkBoxValue === "true") {
            $('#washedCheck').prop("checked", true).trigger("change");
        }
        else if (checkBoxValue === "false") {
            $('#washedCheck').prop("checked", false).trigger("change");
        }
    }

    this.clean = function(element) {
        if (element.dataset.latitude == 0 || element.dataset.longitude == 0) {
            swal({
                    title: 'Vask adresse',
                    text: "Er du sikker? \n Ingen koordinater er registreret for denne adresse. \n Dette vil besværliggøre udregning af merkørsel for medarbejdere der gør brug af denne adresse som hjemme eller arbejdsadresse.",
                    showCancelButton: true,
                    confirmButtonColor: "red",
                    confirmButtonText: "Ja",
                    cancelButtonText: "Nej",
                    closeOnConfirm: true,
                    closeOnCancel: true
                },
                function(isConfirm) {
                    if(isConfirm) {
                        addressWashService.cleanConfirmed(element.dataset.id);
                    }
            });
        }
        else {
            addressWashService.cleanConfirmed(element.dataset.id);
        }
    }

    this.cleanConfirmed = function(id) {
        $.ajax({
            method: "POST",
            url: "/rest/waddress/" + id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            success: function(data, textStatus, jqXHR) {
                toastr.success("Adressen er vasket uden ændringer");
                addressWashService.init();
            },
            error: function(jqXHR, textStatus, errorThown) {
                toastr.warning("Der opstod en teknisk fejl");
            }
        });
    }

    this.closeModal = function() {
        $("#dirtyModal").modal("hide")
    }

    this.updateLocalStorage = function() {
        localStorage.setItem(window.location.pathname + "_table_filter_addressWash", $('#washedCheck').prop("checked"));
    }

    this.edit = function(element) {
        if(this.marker) { washingMap.removeLayer(this.marker); }
        if(element.dataset !== undefined) { $("#resetButton").data(element.dataset); }
        $("#dirtyId").val($("#resetButton").data().id);
        $("#dirtyStreet").val($("#resetButton").data().street);
        $("#dirtyNumber").val($("#resetButton").data().number);
        $("#dirtyZip").val($("#resetButton").data().zip);
        $("#dirtyTown").val($("#resetButton").data().town);
        $("#dirtyModal").modal("show")
        washingMap.invalidateSize();
        this.marker = L.marker([$("#resetButton").data().lat, $("#resetButton").data().lng],{draggable: true, autoPan: true, title: "lokation"}).addTo(washingMap);
        washingMap.flyTo(this.marker.getLatLng(), 15, {animate: false});

        /*
        this.marker.on('dragend', function(e) {
            var coord = this.getLatLng();
            $("#resetButton").data().lat = coord.lat;
            $("#resetButton").data().lng = coord.lng;
        });
        */

        $("#addressWashAutocompleteField").on('keyup', function(e) {
            clearTimeout(typingTimer);
            // Debounce logic
            typingTimer = setTimeout(() => {
                if(e.key === 'Enter' || e.keyCode === 13 ) {
                    var address = addressService.addressWash(this.value);
                    $("#dirtyStreet").val(address.vejstykke.navn);
                    $("#dirtyNumber").val(address.husnr);
                    $("#dirtyZip").val(address.postnummer.nr);
                    $("#dirtyTown").val(address.postnummer.navn);
                    addressWashService.toLatLng();
                }
                else if(this.value.length > 2) {
                    var autocompleteList = addressService.addressAutocomplete(this.value);
                    const suggestions = document.getElementById("addressWashAutocompleteList");
                    suggestions.innerHTML = '';

                    autocompleteList.forEach(function(sugg) {
                        var option = document.createElement('option');
                        option.value = sugg.tekst
                        suggestions.appendChild(option);
                    });
                }
            }, typingDelay);
        });

        $("#addressWashAutocompleteField").on('focusout change', function(e) {
            var address = addressService.addressWash(this.value);
            $("#dirtyStreet").val(address.vejstykke.navn);
            $("#dirtyNumber").val(address.husnr);
            $("#dirtyZip").val(address.postnummer.nr);
            $("#dirtyTown").val(address.postnummer.navn);
            addressWashService.toLatLng();
        });
    }

    this.save = function() {
        var body = JSON.stringify({
            "id": parseInt($("#dirtyId").val()),
            "streetName": $("#dirtyStreet").val(),
            "streetNumber": $("#dirtyNumber").val(),
            "zipCode": parseInt($("#dirtyZip").val()),
            "town": $("#dirtyTown").val(),
            "latitude": this.marker.getLatLng().lat,
            "longitude": this.marker.getLatLng().lng
        });

        if (this.marker.getLatLng().lat == 0 || this.marker.getLatLng().lng == 0) {
            swal({
                title: 'Vask adresse',
                text: "Er du sikker? \n Ingen koordinater er valgt endnu. \n Dette vil besværliggøre udregning af merkørsel for medarbejdere der gør brug af denne adresse som hjemme eller arbejdsadresse.",
                showCancelButton: true,
                confirmButtonColor: "red",
                confirmButtonText: "Ja",
                cancelButtonText: "Nej",
                closeOnConfirm: true,
                closeOnCancel: true
            }, function(isConfirm) {
                if (isConfirm) {
                    addressWashService.saveConfirm(body);
                }
            });
        }
        else {
            addressWashService.saveConfirm(body);
        }
    }

    this.saveConfirm = function(body) {
        $.ajax({
            method: "POST",
            url: "/rest/waddress/",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: body,
            success: function(data, textStatus, jqXHR) {
                toastr.success("Adressen vasket");
                addressWashService.closeModal();
                addressWashService.init();
            },
            error: function(jqXHR, textStatus, errorThown) {
                toastr.warning("Der opstod en teknisk fejl");
            }
        });
    }

    this.toAddr = function() {
         var address = addressService.latLngToAddress(this.marker.getLatLng());
         $("#dirtyStreet").val(address.vejstykke.navn);
         $("#dirtyNumber").val(address.husnr);
         $("#dirtyZip").val(address.postnummer.nr);
         $("#dirtyTown").val(address.postnummer.navn);
    }

    this.toLatLng = function() {
        var coord = addressService.addressToLtLg(addressService.addressWash($("#dirtyStreet").val() + " " + $("#dirtyNumber").val() + ", " + $("#dirtyZip").val()));
        if(this.marker) { washingMap.removeLayer(this.marker); }
        this.marker = L.marker([coord[0].lat, coord[0].lng],{draggable: true, autoPan: true, title: "lokation"}).addTo(washingMap);
        washingMap.flyTo(this.marker.getLatLng(), 15, {animate: false});
    }

    this.originate = function(element) {
        $.ajax({
            method: "POST",
            url: "/rest/waddress/dirtify/" + element.dataset.id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            success: function(data, textStatus, jqXHR) {
                toastr.success("Adressevasken er fjernet");
                addressWashService.init();
            },
            error: function(jqXHR, textStatus, errorThown) {
                toastr.warning("Der opstod en teknisk fejl");
            }
        });
    }
}