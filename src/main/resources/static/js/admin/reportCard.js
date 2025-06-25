var token = $("meta[name='_csrf']").attr("content");
var reportCardService;
$(document).ready(function () {
    reportCardService = new ReportCardService();
    reportCardService.init();
});


function ReportCardService() {
    this.init = function () {
        this.loadFragment();

    }

    this.loadFragment = function () {
        $('#reportCardFragmentDiv').load(`/reportCard`, this.initSelect2);
    };

    this.initSelect2 = function () {
        const url = window.location.href;
        let backendUrl = "Prefix";
        if (url.includes("approve")) {
            backendUrl = "Approver";

        }
        $('#inputWorkerName').select2({
            placeholder: "indtast navn",
            minimumInputLength: 2,
            allowClear: true,
            ajax: {
                url: "/reportCard/getPersonsBy" + backendUrl,
                dataType: "json",
                delay: 250 // wait a bit so we get the right input
            }
        });
        $('#tableDiv').hide();

        // Load the pages differently according to where we call it from
        if (url.includes("approve")) {
            $.ajax({
                method : "GET",
                url: "/reportCard/whoDoILeadOrSub",
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                success: function(data) {
                    let indexes = [];
                    for (let orgUnitsKey in data) {
                        indexes.push(orgUnitsKey);
                    }
                    $('#inputOrgUnitName').find("option").each(function () {
                        if (!indexes.includes($(this).val())) {
                            $(this).hide();
                        }
                        else {
                            $('#inputOrgUnitName').val($(this).val());
                        }
                    });
                }
            });
        }

        if (url.includes("list")) {
            $('#inputWorkerName').prop("disabled", true);
            $.ajax({
                method : "GET",
                url: "/reportCard/whoAmI",
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                success: function(data) {
                    var newOption = new Option(data.personName, data.personId, false, true);
                    $('#inputWorkerName').append(newOption).change();
                    let indexes = [];
                    for (let orgUnitsKey in data.orgUnits) {
                        indexes.push(orgUnitsKey);
                    }
                    $('#inputOrgUnitName').find("option").each(function () {
                        if (!indexes.includes($(this).val())) {
                            $(this).hide();
                        }
                        else {
                            $('#inputOrgUnitName').val($(this).val());
                        }
                    });
                }
            });
        }

        // When a new person has been selected, filter OrgUnits to show only the ones they have an employment in.
        $('#inputWorkerName').on("select2:select", function() {
            $.ajax({
                method : "GET",
                url: "/reportCard/getOrgunitsByEmployment/" + $('#inputWorkerName').val(),
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                success: function(data) {
                    let indexes = [];
                    for (let orgUnitsKey in data.orgUnits) {
                        indexes.push(orgUnitsKey);
                    }
                    $('#inputOrgUnitName').find("option").each(function () {
                        if (!indexes.includes($(this).val())) {
                            $(this).hide();
                        }
                        else {
                            $('#inputOrgUnitName').val($(this).val());
                        }
                    });
                }
            });
        })
        // If the input is cleared, we reset the OU selection with the code below
        $('#inputWorkerName').on("change", function () {
            if ($('#inputWorkerName').val() == null) {
                $('#inputOrgUnitName').find("option").each(function () {
                    $(this).show();
                    $('#inputOrgUnitName').val(0).change();
                });
            }
        });
    }

    this.getReport = function () {
        if ($('#inputWorkerName').val() == "Indtast navn") {
            toastr.warning("Mangler navn på medarbejderen");
            return;
        }
        if ($('#inputOrgUnitName').val() == "Indtast Organisationsenhed") {
            toastr.warning("Mangler navn på organisationsenheden");
            return;
        }
        if ($('#inputDateFrom').val().length == 0 || $('#inputDateTo').val().length == 0) {
            toastr.warning("Mangler dato!");
            return;
        }

        let data = {
            "personId": $('#inputWorkerName').val(),
            "orgUnitId": $('#inputOrgUnitName').val(),
            "from": $('#inputDateFrom').val(),
            "to": $('#inputDateTo').val()
        }

        $.ajax({
            method: "POST",
            url: "/rest/reportCard/getReports",
            headers: {
                "content-type": "application/json",
                "X-CSRF-TOKEN": token
            },
            data: JSON.stringify(data),
            success: function(data) {
                // Set some values that need to be above the table pre-emptively
                $('#workerName').text(data.fullName);
                $('#orgUnitName').text(data.orgName);
                $('#dateOfCreation').text(data.now);
                $('#startStopDate').text(data.from + " - " + data.to);
                $('#licensePlate').text(data.licensePlates);

                reportCardService.initTable(data.processedReports);
            }
        });
    }

    this.initTable = function (reports) {
        $('#tableDiv').show();

        var repTable = $('#reportTable').DataTable({
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 20,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "language": {
                "search": "Søg",
                "info": "Viser _START_ til _END_ af _TOTAL_ rækker",
                "zeroRecords": "Ingen data...",
                "infoEmpty": "Henter data...",
                "infoFiltered": "(ud af _MAX_ rækker)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                }
            },
            "footerCallback": function (row, data, start, end, display) {
                var api = this.api();

                // Remove the formatting to get integer data for summation
                var intVal = function (i) {
                    return typeof i === 'string'
                        ? i * 1
                        : typeof i === 'number'
                            ? i
                            : 0;
                };

                moneyPageTotal = api
                    .column(13, { page: 'current' })
                    .data()
                    .reduce(function (a, b) {
                        return intVal(a) + intVal(b);
                    }, 0);

                // Update footer
                $(api.column(13).footer()).html(
                    'Total: <br><strong>' + Math.round((moneyPageTotal + Number.EPSILON) * 100) / 100 + ' kr.</strong>'
                );

                kmPageTotal = api
                    .column(12, { page: 'current' })
                    .data()
                    .reduce(function (a, b) {
                        return intVal(a) + intVal(b);
                    }, 0);

                // Update footer
                $(api.column(12).footer()).html(
                    'Total: <br><strong>' + Math.round((kmPageTotal + Number.EPSILON) * 100) / 100
                    + ' km</strong>'
                );
            },
            "data": reports,
            "columns": [
                { "data": "driveDate" },
                { "data": "createdDate" },
                { "data": "personName" },
                { "data": "employeeId" },
                { "data": "orgName" },
                { "data": "purpose" },
                { "data": "route",
                  "render" : function(data, type, row) {
                       return '<i class="fa fa-fw fa-2x fa-road" aria-hidden="true" title="' + data + '"></i>';
                  }
                },
                { "data": "roundTrip" },
                { "data": "extraDistance" },
                { "data": "fourKmRule" },
                { "data": "homeToBorderDistance" },
                { "data": "sixtyDayRule" },
                { "data": 'distance',
                  "render": function(data, type, row) {
                    return (Math.round(data * 100) / 100).toFixed(2);
                  }
                },
                { "data": "amount" },
                { "data": "kmRateType" },
                { "data": "kmRate" },
                { "data": "status" },
                { "data": "approvedBy" },
                { "data": "timeOfApproval" },
                { "data": "flagged60days",
                    "render" : function(data, type, row) {
                        if(data) {
                            return '<i class="fa fa-fw fa-2x fa-exclamation-triangle" aria-hidden="true" title="Denne indberetning er muligvis i overtrædelse af 60-dages reglen"></i>';
                        }
                        else {
                            return '';
                        }
                    }
                }
            ]
        });

        // Apply column search filters
        $.each($('.input-filter', repTable.table().footer()), function () {
            var column = repTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.clearValues = function () {
        const url = window.location.href;

        if (!url.includes("list")) {
            $('#inputWorkerName').val("").change();
        }
        if (url.includes("approve")) {
            $("#inputOrgUnitName").val();
        }
        $('#tableDiv').hide();
        $('#inputDateFrom').val("");
        $('#inputDateTo').val("");
    }

    this.downloadCSV = function () {

        let data = {
            "personId": $('#inputWorkerName').val(),
            "orgUnitId": $('#inputOrgUnitName').val(),
            "from": $('#inputDateFrom').val(),
            "to": $('#inputDateTo').val()
        };

        $.ajax({
            method: "POST",
            url: "/reportCard/downloadReport",
            headers: {
                "content-type": "application/json",
                "X-CSRF-TOKEN": token
            },
            data: JSON.stringify(data),
            xhrFields: {
                responseType: 'blob'
            },
            success: function (data, status, xhr) {
                var blob = new Blob([data], { type: 'application/zip' });
                var link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download = 'Rapporteringer.zip';
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                errorResponse(jqXHR);
            }
        });
    }
}
