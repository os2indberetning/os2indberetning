var token = $("meta[name='_csrf']").attr("content");

var approverService;
$(document).ready(function () {
    approverService = new ApproverService();
    approverService.init();
});


function ApproverService() {
    var id;
    var editMode;

    this.init = function () {
        this.loadFragment();
    }

    this.loadFragment = function () {
        if (window.location.href.includes("admin")) {
            $('#substituteApproverFragmentDiv').load('/admin/approvers', this.initTab);
        }
        else {
            $('#approverFragmentDiv').load('approve/substitutes', this.initTab);
        }
    }

    this.initTab = function() {
        var subTable = $('#substituteTable').DataTable({
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 10,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "drawCallback": function (settings) {
                $("#inputSubstitute").select2({
                    allowClear: true,
                    placeholder: 'Indtast navn',
                    dropdownParent: $('#test'),
                    minimumInputLength: 2,
                    ajax: {
                        url: 'substitute/findByPrefix',
                        dataType: 'json',
                        delay: 250 // wait 250 milliseconds before triggering the request
                    }
                });
                $('[data-toggle="popover"]').popover();

                if(!($("#preSelectedApprover").data("value"))) {
                    $("#inputSubFor").select2({
                        allowClear: true,
                        placeholder: 'Indtast navn',
                        dropdownParent: $('#test'),
                        minimumInputLength: 2,
                        ajax: {
                            url: 'substitute/getAllLeaders',
                            dataType: 'json',
                            delay: 250 // wait 250 milliseconds before triggering the request
                        }
                    });

                    $("#inputSubFor").on("select2:unselecting", () => {
                        $('#dropDownList').val($('#dropDownList option:eq(0)').val());
                    });
                }
                else {
                     $("#inputSubFor").val($("#preSelectedApprover").data("value"));
                     $("#inputSubFor").attr('disabled', 'disabled');
                }

            },
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
        $.each($('.input-filter', subTable.table().footer()), function() {
            var column = subTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
        var personalSubTable = $('#personalSubTable').DataTable({
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 10,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "drawCallback": function (settings) {
                $("#inputPersonalSub").select2({
                    placeholder: 'Indtast navn',
                    dropdownParent: $('#personalModalBody'),
                    minimumInputLength: 2,
                    ajax: {
                        url: 'substitute/findByPrefix',
                        dataType: 'json',
                        delay: 250 // wait 250 milliseconds before triggering the request
                    }
                });
                $('[data-toggle="popover"]').popover();

                $("#inputPersonalSubFor").select2({
                    placeholder: 'Indtast navn',
                    dropdownParent: $('#personalModalBody'),
                    minimumInputLength: 2,
                    ajax: {
                        url: 'substitute/personal/subfor/findByPrefix',
                        dataType: 'json',
                        delay: 250 // wait 250 milliseconds before triggering the request
                    }
                });
            },
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
        $('.i-checks').iCheck({
            checkboxClass: 'icheckbox_square-blue',
            radioClass: 'iradio_square-blue',
        });
        $.each($('.input-filter', personalSubTable.table().footer()), function() {
            var column = personalSubTable.column($(this).index());
            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.getLeaderForOrgUnits = function () {
        let orgId = $("#dropDownList option:selected").val();
        if(orgId == 0) {
            $('#inputSubFor').val($('#inputSub option:eq(0)')).change();
            return;
        }

        $.ajax({
            method : "GET",
            url: "substitute/getLeaderForOrgUnit?id=" + orgId,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function(data) {
                var newOption = new Option(data.text, data.id, false, true);
                $('#inputSubFor').append(newOption).change();
            }
        });
    }


    this.openPersonalModal = function (obj) {
        // reset all the values...
        $('#endDatePersonalSub').val("");
        $('#startDatePersonalSub').val("");

        $('#inputPersonalSub').val(null).trigger("change");
        $('#inputPersonalSubFor').val(null).trigger("change");

        $('#noEndDatePersonalSub').iCheck("uncheck");

        $('#personalModal').modal("show");

        $('#personalModal .i-checks').iCheck({
            checkboxClass: 'icheckbox_square-blue',
            radioClass: 'iradio_square-blue',
        });

        id = $(obj).data("id");
        if (id) {
            editMode = true;
            start = $(obj).data("start");
            end = $(obj).data("end");
            subName = $(obj).data("sub");
            subForName = $(obj).data("for");

            if (end != null) {
                $('#endDatePersonalSub').val(end);
            } else {
                $('#noEndDatePersonalSub').iCheck('check');
                $('#endDatePersonalSub').prop('disabled', true);
            }

            $('#startDatePersonalSub').val(start);

            $.ajax({
                method : "GET",
                url: "substitute/getPerson?id=" + subName,
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                success: function(data) {
                    var newOption = new Option(data.text, data.id, false, true);
                    $('#inputPersonalSub').append(newOption).change();
                }
            });

            if(!($("#preSelectedApprover").data("value"))) {
                $.ajax({
                    method : "GET",
                    url: "substitute/getPerson?id=" + subForName,
                    headers: {
                        "content-type": "application/json",
                        'X-CSRF-TOKEN': token
                    },
                    success: function(data) {
                        var newOption = new Option(data.text, data.id, false, true);
                        $('#inputPersonalSubFor').append(newOption).change();
                    }
                });
            }
        }
        else {
            editMode = false;
        }

       $('#noEndDatePersonalSub').on("ifChanged", function () {
            if ($('#noEndDatePersonalSub').is(":checked")) {
                $("#endDatePersonalSub").prop('disabled', true);
            }
            else {
                $('#endDatePersonalSub').prop('disabled', false);
            }
        });
    }

    this.closePersonalModal = function () {
        $('#personalModal').modal("hide");
    }

    this.openSubModal = function (obj) {

        // reset all the values...
        $('#endDateSub').val("");
        $('#startDateSub').val("");

        $('#inputSubstitute').val($('#inputSub option:eq(0)')).change();

        if(!($("#preSelectedApprover").data("value"))) {
            $('#inputSubFor').val($('#inputSub option:eq(0)')).change();
        }

        $('#dropDownList').val($('#dropDownList option:eq(0)').val());
        $('#noEndDateSub').iCheck("uncheck");
        $('#isSubstituteExclusive').iCheck("uncheck");

        $('#substituteModal').modal("show");

        $('#substituteModal .i-checks').iCheck({
            checkboxClass: 'icheckbox_square-blue',
            radioClass: 'iradio_square-blue',
        });

        id = $(obj).data("id");
        if (id) {
            editMode = true;
            exclusive = $(obj).data("exclusive");
            start = $(obj).data("start");
            end = $(obj).data("end");
            subName = $(obj).data("sub");
            subForName = $(obj).data("for");
            orgUnit = $(obj).data("ou");

            $.ajax({
                method : "GET",
                url: "substitute/getPerson?id=" + subName,
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                success: function(data) {
                    var newOption = new Option(data.text, data.id, false, true);
                    $('#inputSubstitute').append(newOption).change();
                }
            });

            if(!($("#preSelectedApprover").data("value"))) {
                $.ajax({
                    method : "GET",
                    url: "substitute/getPerson?id=" + subForName,
                    headers: {
                        "content-type": "application/json",
                        'X-CSRF-TOKEN': token
                    },
                    success: function(data) {
                        var newOption = new Option(data.text, data.id, false, true);
                        $('#inputSubFor').append(newOption).change();
                    }
                });
            }

            $('#dropDownList').val(orgUnit);

            if (exclusive) {
                $('#isSubstituteExclusive').iCheck('check');
            }

            if (end != null) {
                $('#endDateSub').val(end);
            }
            else {
                $('#noEndDateSub').iCheck('check');
                $("#endDateSub").prop('disabled', true);
            }

            $('#startDateSub').val(start);

        }
        else {
            editMode = false;
        }

        $('#noEndDateSub').on("ifChanged", function () {
            if ($('#noEndDateSub').is(":checked")) {
                $("#endDateSub").prop('disabled', true);
            }
            else {
                $('#endDateSub').prop('disabled', false);
            }
        });

        $('#inputSubFor').on("change", function () {
            if ($('#inputSubFor').val() == null) {
                return;
            }
            else {
                let id = $('#inputSubFor').select2('data')[0].id;
                $.ajax({
                    method : "GET",
                    url: "substitute/getLeaderForPerson?personId=" + id,
                    headers: {
                        "content-type": "application/json",
                        'X-CSRF-TOKEN': token
                    },
                    success: function(data, textStatus, jqXHR) {
                        $('#dropDownList').val(data);
                    }
                });
            }
        })
    }

    this.closeSubModal = function () {

         $('#substituteModal').modal("hide");
    }

    this.createOrEditSub = function(obj) {

        var data = {
            "id": id,
            "subId": $('#inputSubstitute').val(),
            "subForId": $('#inputSubFor').val(),
            "orgUnitId": $("#dropDownList option:selected").val(),
            "startDate": $('#startDateSub').val(),
            "endDate": $('#endDateSub').val(),
            "unlimitedEndTime": $('#noEndDateSub').is(":checked"),
            "substituteExclusiveMode": $('#isSubstituteExclusive').is(":checked")
        }

        if (!data.subId || !data.subForId) {
            toastr.warning("Begge personfelter skal være udfyldt!");
            return;
        }

        if (data.subId == data.subForId) {
            toastr.warning("De to personer skal være forskellige!");
            return;
        }

        if (!(data.orgUnitId > 0)) {
            toastr.warning("Enhed skal være valgt!");
            return;
        }

        if (!data.startDate || (!data.unlimitedEndTime && !data.endDate)) {
            toastr.warning("Datoerne skal være udfyldt!");
            return;
        }

        if (editMode) {
            $.ajax({
                method : "POST",
                url: "substitute/editSub",
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                data: JSON.stringify(data),
                success: function(data, textStatus, jqXHR) {
                    toastr.success("Stedfortræder redigeret");
                    $('#substituteModal').modal("hide");
                    approverService.init();
                    },
                error: function(jqXHR, textStatus, errorThrown) {
                    errorResponse(jqXHR);
                }
            });
        }
        else {
            $.ajax({
                method : "POST",
                url: "substitute/createSub",
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                data: JSON.stringify(data),
                success: function(data, textStatus, jqXHR) {
                    toastr.success("Stedfortræder oprettet/redigeret");
                    $('#substituteModal').modal("hide");
                    approverService.init();
                    },
                error: function(jqXHR, textStatus, errorThrown) {
                    errorResponse(jqXHR);
                }
            });
        }
    }

    this.createOrEditPersonal = function (obj) {

        var data = {
            "id": id,
            "subId": $('#inputPersonalSub').val(),
            "subForId": $('#inputPersonalSubFor').val(),
            "startDate": $('#startDatePersonalSub').val(),
            "endDate": $('#endDatePersonalSub').val(),
            "unlimitedEndTime": $('#noEndDatePersonalSub').is(":checked")
        }

        if (!data.subId || !data.subForId) {
            toastr.warning("Begge personfelter skal være udfyldt!");
            return;
        }

        if (data.subId == data.subForId) {
            toastr.warning("De to personer skal være forskellige!");
            return;
        }

        if (!data.startDate || (!data.unlimitedEndTime && !data.endDate)) {
            toastr.warning("Datoerne skal være udfyldt!");
            return;
        }

        if (editMode) {
            $.ajax({
                method : "POST",
                url: "substitute/editPersonalSub",
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                data: JSON.stringify(data),
                success: function(data, textStatus, jqXHR) {
                    toastr.success("Stedfortræder redigeret");
                    approverService.closePersonalModal();
                    approverService.init();
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    errorResponse(jqXHR);
                }
            });
        } else {
            $.ajax({
                method : "POST",
                url: "substitute/createPersonal",
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                data: JSON.stringify(data),
                success: function(data, textStatus, jqXHR) {
                    toastr.success("Personlig oprettet");
                    approverService.closePersonalModal();
                    approverService.init();
                    },
                error: function(jqXHR, textStatus, errorThrown) {
                    errorResponse(jqXHR);
                }
            });
        }
    }

    this.deleteSub = function (id) {

        let data = {
            "id": id
        }

        $.ajax({
            method : "POST",
            url: "substitute/deleteSub",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Stedfortræder slettet!");
                approverService.init();
            },
            error: function(jqXHR, textStatus, errorThrown) {
                errorResponse(jqXHR);
            }
        });
    }

    this.openDeleteModal = function (obj) {
        let id = $(obj).data("id");

        var msg = $(obj).data("type") == "personal" ? "Er du sikker på at du vil slette denne personlige godkender? " : "Er du sikker på at du vil slette denne stedfortræder?"

        swal({
            title: msg,
            text: "Denne handling kan ikke fortrydes",
            type: "warning",
            showCancelButton: true,
            cancelButtonText: "Afbryd",
            confirmButtonColor: "#DD6B55",
            confirmButtonText: "slet",
            closeOnConfirm: false
        }, function () {
            swal.close();
            approverService.deleteSub(id);
        });

    }
}


