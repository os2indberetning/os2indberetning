var token = $("meta[name='_csrf']").attr("content");

var rateService;
var rateTypeService;

$(document).ready(function() {
    rateService = new RateService();
    rateTypeService = new RateTypeService();
    rateService.init();
    rateTypeService.init();
});

function RateService() {
    this.init = function () {
        this.loadFragment();
    }

    this.loadFragment = function() {
        $('#ratesFragmentDiv').load('/admin/ratesFragment', this.initDatatable);
    }

    this.initDatatable = function() {
        var rateTable = $('#rateTable').DataTable({
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
    }

    this.closeModal = function() {
        $("#rateModal").modal("hide");
        this.loadFragment();
    }

    this.openModal = function() {
        $("#rateModal").modal("show");
        $('#rateYear:input:enabled:visible:first').focus();
        $('#rateModal input').bind('keypress', function(e) {
            var code = e.keyCode || e.which;
            if(code == 13) {
                rateService.createSave();
            }
        });
    }

    this.create = function() {
        $('#rateYear').val("");
        $('#ratePerKm').val("");
        $('#rateTypeSelect').val($('#rateTypeSelect option:eq(0)').val());

        this.openModal();
        $("#butRateCreate").show();
        $("#butRateEdit").hide();
    }

    this.createSave = function() {
        var data = {
            "activeYear": $('#rateYear').val(),
            "ratePerKm": $('#ratePerKm').val(),
            "rateType": $('#rateTypeSelect').val()
        }

        $.ajax({
            method : "POST",
            url: "rest/rate/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                rateService.closeModal();
                toastr.success("Takst oprettet");
                rateService.init();
            },
            error: function(jqXHR, textStatus, errorThrown) {
                rateService.errorResponse(jqXHR);
            }
        })
    }



    this.del = function (element) {
        swal({
            title: 'Slet takst',
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
                        url: "rest/rate/delete/" + element.dataset.id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            rateTypeService.closeModal();
                            toastr.success("Takst slettet");
                            rateService.init();
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            rateService.errorResponse(jqXHR);
                        }
                    });
                }
            }
        );
    }

    this.edit = function(element) {
        $('#rateId').val(element.dataset.id);
        $('#rateYear').val(element.dataset.activeyear);
        $('#rateTypeSelect').val($('#rateTypeSelect option').eq(element.dataset.ratetype).val());
        $('#ratePerKm').val(element.dataset.rateperkm);
        this.openModal();
        $("#butRateCreate").hide();
        $("#butRateEdit").show();
    }

    this.editSave = function () {
        var data = {
            "id": $('#rateId').val(),
            "activeYear": $('#rateYear').val(),
            "ratePerKm": $('#ratePerKm').val(),
            "rateType": $('#rateTypeSelect').val()
        }

        $.ajax({
            method : "POST",
            url: "rest/rate/update",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                rateService.closeModal();
                toastr.success("Takst gemt");
                rateService.init();
            },
            error: function(jqXHR, textStatus, errorThrown) {
                rateService.errorResponse(jqXHR);
            }
        });
    }

    this.errorResponse = function (error) {
        if(error.status == 401) {
            toastr.warning("Du har ikke tilladelse til at foretage denne ændring.");
        }
        else if(error.status == 406) {
            toastr.warning("Kunne ikke findes i databasen");
        }
        else if(error.status == 409) {
            toastr.warning("Der findes allerede en takst med samme år og taksttype");
        }
        else {
            toastr.warning("der opstod en teknisk fejl");
        }
    }
}

function RateTypeService() {
    this.init = function () {
        this.loadFragment();
    }

    this.loadFragment = function() {
        $('#rateTypesFragmentDiv').load('/admin/rateTypesFragment', this.initDatatable);
    }

    this.initDatatable = function() {
        var rTypeTable = $('#rateTypeTable').DataTable({
            "destroy": true,
            "bSort": true,
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
        $.each($('.input-filter', rTypeTable.table().footer()), function() {
            var column = rTypeTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if(column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }


    this.makePrimary = function (element) {
        $.ajax({
            method : "POST",
            url: "rest/rType/updatePrimary/" + element.dataset.id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            success: function(data, textStatus, jqXHR) {
                toastr.success("Taksttype sat som standard.");
                rateTypeService.loadFragment();
            },
            error: function(jqXHR, textStatus, errorThrown) {
                rateService.errorResponse(jqXHR);
            }
        })
    }

    this.removePrimary = function (element) {
        $.ajax({
            method : "POST",
            url: "rest/rType/removePrimary/" + element.dataset.id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            success: function(data, textStatus, jqXHR) {
                toastr.success("Primær-markering fjernet fra taksttype");
                rateTypeService.loadFragment();
            },
            error: function(jqXHR, textStatus, errorThrown) {
                rateService.errorResponse(jqXHR);
            }
        })
    }

    this.closeModal = function() {
        $("#rTypeModal").modal("hide");
        rateService.loadFragment();
    }

    this.openModal = function() {
        $("#rTypeModal").modal("show");
        $('#rTypeName:input:enabled:visible:first').focus();

        $('#rTypeModal input').bind('keypress', function(e) {
            var code = e.keyCode || e.which;
            if(code == 13) {
                if ($('#rTypeId').val()) {
                    rateTypeService.editSave();
                }
                else {
                    rateTypeService.createSave();
                }
            }
        });
    }

    this.create = function() {
        $('#rTypeId').val("");
        $('#rTypeName').val("");
        $('#sqNumber').val("");
        $('#payType').val("");
        this.openModal();
        $("#butRateTypeCreate").show();
        $("#butRateTypeEdit").hide();
    }

    this.createSave = function() {
        if ($('#payType').val().length == 0) {
            toastr.warning("Lønart feltet er tomt!");
            return;
        }
        if ($('#rTypeName').val().length == 0) {
            toastr.warning("Der mangler navn på takst typen!");
            return;
        }

        var data = {
            "name": $('#rTypeName').val(),
            "payType": $('#payType').val(),
            "sqNumber": $('#sqNumber').val()
        }

        $.ajax({
            method : "POST",
            url: "rest/rType/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                rateTypeService.closeModal();
                toastr.success("Takst type oprettet");
                rateTypeService.init();
            },
            error: function(jqXHR, textStatus, errorThrown) {
                rateTypeService.errorResponse(jqXHR);
            }
        });
    }

    this.del = function (element) {
        swal({
            title: 'Slet taksttype',
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
                        url: "rest/rType/delete/" + element.dataset.id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            rateTypeService.closeModal();
                            toastr.success("Takst type slettet");
                            rateService.init();
                            rateTypeService.init();
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            rateTypeService.errorResponse(jqXHR);
                        }
                    });
                }
            }
        );
        }

    this.edit = function (element) {
        $('#rTypeId').val(element.dataset.id);
        $('#rTypeName').val(element.dataset.name);
        $('#sqNumber').val(element.dataset.sqnumber);
        $('#payType').val(element.dataset.ptype);

        this.openModal();
        $("#butRateTypeCreate").hide();
        $("#butRateTypeEdit").show();
    }

    this.editSave = function () {

        if ($('#payType').val().length == 0) {
            toastr.warning("Lønart feltet er tomt!");
            return;
        }
        if ($('#rTypeName').val().length == 0) {
            toastr.warning("Der mangler navn på takst typen!");
            return;
        }

        var data = {
            "id": $('#rTypeId').val(),
            "name": $('#rTypeName').val().trim(),
            "payType": $('#payType').val().trim(),
            "sqNumber": $('#sqNumber').val().trim()
        }

        $.ajax({
            method : "POST",
            url: "rest/rType/update",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
                processData: false,
            data: JSON.stringify(data),
                success: function(data, textStatus, jqXHR) {
                    rateTypeService.closeModal();
                    toastr.success("Takst type ændret");
                    rateService.init();
                    rateTypeService.init();
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    rateTypeService.errorResponse(jqXHR);
                }
            });
    }

    this.errorResponse = function (error) {
        if(error.status == 401) {
            toastr.warning("Du har ikke tilladelse til at foretage denne ændring.");
        }
        else if(error.status == 406) {
            toastr.warning("Kunne ikke findes i databasen");
        }
        else if(error.status == 409) {
            toastr.warning("Der findes allerede en takst type med samme navn");
        }
        else {
            toastr.warning("der opstod en teknisk fejl");
        }
    }
}
