var token = $("meta[name='_csrf']").attr("content");

var appLoginService;
var plateService;
var notificationService;
$(document).ready(function () {
    plateService = new plateService();
    plateService.init();

    $('#admin').on('change', (e) => { toggleEmailNotifications(e.target) });
    $('#personal').on('change', (e) => { toggleEmailNotifications(e.target) });
    $('#approver').on('change', (e) => { toggleEmailNotifications(e.target) });

    appLoginService = new appLoginService();
    appLoginService.init();

    notificationService = new NotificationService();
    notificationService.getNotification();
});

function toggleEmailNotifications(element) {

    $.ajax({
        method: "POST",
        url: "rest/mail/toggle/" + element.id,
        headers: {
            "content-type": "application/json",
            'X-CSRF-TOKEN': token
        },
        success: function () {
            toastr.success("Adviseringsindstilling er ændret");
        },
        error: function () {
            toastr.error("Fejl. Ændring kunne ikke gennemføres");
        }

    })
}

function plateService() {
    this.init = function () {
        var table = $('#platesTable').DataTable({
            "bSort": false,
            "paging": false,
            "responsive": true,
            "dom": "<'row'<'col-sm-12'tr>>",
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

        $.each($('.input.filter', table.table().footer()), function () {
            var column = table.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });

        $(function () {
          $('[data-toggle="tooltip"]').tooltip()
        })
    }

    this.createPlate = function () {
        $("#plateId").val("");
        $("#plateNumber").val("");
        $("#plateDescription").val("");
        $("#plateModal").modal("show");
        $('#plateNumber:input:enabled:visible:first').focus();
        $('#butCreate').show();
        $('#butEdit').hide();
        $('#plateModal input').bind('keypress', function(e) {
            var code = e.keyCode || e.which;
            if(code == 13) {
                plateService.createSave();
            }
        });
        $("#butEdit").hide();
    }

    this.createSave = function () {
        var freshPlateNumber = $("#plateNumber").val();
        var freshPlateDescription = $("#plateDescription").val();

        if (freshPlateNumber.length == 0) {
            toastr.warning("Der mangler tal/bogstaver på pladen!");
            return;
        }
        if (freshPlateDescription == 0) {
            toastr.warning("Der er ingen beskrivelse på pladen!");
            return;
        }

        var data = {
            "registrationNumber": freshPlateNumber,
            "description": freshPlateDescription,
            "prime": false
        }

        $.ajax({
            method: "POST",
            url: "rest/plate/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
                },
                processData: false,
                data: JSON.stringify(data),
                success: function(data, textStatus, jqXHR) {
                    plateService.closeModal();
                    toastr.success("Nummerpladen tilføjet");
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    toastr.warning("Der opstod en teknisk fejl");
                }
        })
    }

    this.editPlate = function (plateElement) {
        $("#plateId").val(plateElement.dataset.id);
        $("#plateNumber").val(plateElement.dataset.number);
        $("#plateDescription").val(plateElement.dataset.description);
        $("#butCreate").hide();
        $("#butEdit").show();
        $("#plateModal").show();


        $("#plateModal").modal("show");
        $('#plateNumber:input:enabled:visible:first').focus();

        $('#plateModal input').bind('keypress', function(e) {
            var code = e.keyCode || e.which;
            if(code == 13) {
                plateService.editSave();
            }
        });
    }

    this.editSave = function () {
        var newPlateId = $("#plateId").val();
        var newPlateNumber = $("#plateNumber").val();
        var newPlateDescription = $("#plateDescription").val();

        if (newPlateId.length == 0) {
            toastr.warning("Denne nummerplade id eksister ikke!");
            return;
        }
        if (newPlateNumber.length == 0) {
            toastr.warning("Der mangler tal/bogstaver på pladen!");
            return;
        }
        if (newPlateDescription == 0) {
            toastr.warning("Der er ingen beskrivelse på pladen!");
            return;
        }

        var data = {
            "id": newPlateId,
            "registrationNumber": newPlateNumber,
            "description": newPlateDescription,
            "prime": false
        }

        $.ajax({
            method: "POST",
            url: "rest/plate/update",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
                },
                processData: false,
                data: JSON.stringify(data),
                success: function(data, textStatus, jqXHR) {
                    plateService.closeModal();
                    toastr.success("Nummerpladen ændret");
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    toastr.warning("Der opstod en teknisk fejl");
                }
            })
    }

    this.deletePlate = function (element) {
        swal({
            title: 'Slet nummerplade',
            text: "er du sikker?",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            confirmButtonText: "Ja",
            cancelButtonText: "Nej",
            closeOnConfirm: true,
            closeOnCancel: true
        },
            function (isConfirm) {
                if (isConfirm) {
                    $.ajax({
                        method: "POST",
                        url: "rest/plate/delete/" + element.dataset.id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                            }
                    }).done(function(data) {
                        plateService.closeModal();
                        toastr.success("Nummerpladen er slettet");
                    }).fail(function () {
                        toastr.warning("Der opstod en teknisk fejl.");
                    });
                }
            }
        );
    }

    this.closeModal = function() {
        $("#plateModal").modal("hide");
        $('#plateModal input').off();
        $('#platesTable').load(window.location.href + " #platesTable");
    }

    this.openModal = function() {
        $("#plateModal").modal("show");
    }

    this.primePlate = function (element) {
        $.ajax({
            method: "POST",
            url: "rest/plate/prime/" + element.dataset.id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function (data, textStatus, jqXHR) {
                plateService.closeModal();
                toastr.success("Primære nummerplade ændret");
            },
            error: function (jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en teknisk fejl");
            }
        })
    }
}

function appLoginService() {
    this.init = function() {
        this.load();
    }

    this.load = function() {
        $('#appLoginFragmentDiv').load('/settings/appLoginFragment', appLoginService.initFragment);
    }
    this.initFragment = function() {
        $('[data-toggle="tooltip"]').tooltip();
    }

    this.close = function () {
        $('#appLoginModal').modal("hide");
    }

    this.create = function() {
        $("#appUsername").val("");
        $("#appUsername").prop('disabled', false);
        $("#passwordProper").val("");
        $("#passwordConfirm").val("");
        $("#butAppCrea").show();
        $("#butAppEdit").hide();
        $("#appLoginModal").modal("show");
    }

    this.createSave = function() {
        var appLogUsername = $("#appUsername").val();
        var appLogPassProp = $("#passwordProper").val();
        var appLogPassConf = $("#passwordConfirm").val();

        if (!appLogUsername) {

            toastr.warning("Brugernavn skal være udfyldt!");
            return;
        }
        if (appLogPassProp != appLogPassConf) {
            toastr.warning("De to kodeord skal være ens!");
            return;
        }

        $("#butAppCrea").prop('disabled', true);

        $.ajax({
            method : "POST",
            url: "rest/appLogin/create",
            headers: {
            "content-type": "application/json",
            'X-CSRF-TOKEN': token
            },
            processData: false,
            data: '{"username": "' + appLogUsername + '", "password": "' + appLogPassProp + '", "passwordConfirm": "' + appLogPassConf + '"}',
            success: function(data, textStatus, jqXHR) {
                appLoginService.close();
                appLoginService.load();
                toastr.success("Login til app tilføjet");
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en teknisk fejl");
            }
        })
    }

    this.del = function(element) {
        swal({
            title: 'Slet app login',
            text: "er du sikker?",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            confirmButtonText: "Ja",
            cancelButtonText: "Nej",
            closeOnConfirm: true,
            closeOnCancel: true
            },
            function(isConfirm) {
                if(isConfirm) {
                    $.ajax({
                        method : "POST",
                        url: "rest/appLogin/delete/" + element.dataset.id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                            }
                        }).done(function(data) {
                            appLoginService.load();
                            toastr.success("login slettet");
                            }).fail(function () {
                                toastr.warning("Der opstod en teknisk fejl.");
                                });
                    }
                }
            );
        }

    this.edit = function(element) {
        $("#appLogId").val(element.dataset.id);
        $("#appUsername").val(element.dataset.username);
        $("#appUsername").prop('disabled', true);
        $("#passwordProper").val("");
        $("#passwordConfirm").val("");
        $("#butAppCrea").hide();
        $("#butAppEdit").show();
        $("#appLoginModal").modal("show");
        }

    this.editSave = function() {
        var appLogId = $("#appLogId").val();
        var appLogUsername = $("#appUsername").val();
        var appLogPassProp = $("#passwordProper").val();
        var appLogPassConf = $("#passwordConfirm").val();

        if (appLogPassProp != appLogPassConf) {
            toastr.warning("De to kodeord skal være ens!");
            return;
        }

        $.ajax({
            method : "POST",
            url: "rest/appLogin/update",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: '{"id": "' + appLogId + '", "username": "' + appLogUsername + '", "password": "' + appLogPassProp + '", "passwordConfirm": "' + appLogPassConf + '"}',
            success: function(data, textStatus, jqXHR) {
                appLoginService.close();
                appLoginService.load();
                toastr.success("Login til app ændret");
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en teknisk fejl");
            }
        })
    }
}
