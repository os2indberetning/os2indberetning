var token = $("meta[name='_csrf']").attr("content");

function ReportListFragmentService() {
    this.tables = {}; // Store references to each DataTable


    var id;
    var approveMultiple;
    var url;

    this.init = function (url, element) {
        var status = $(element).data("status");
        var personal = $(element).data("personal");
        var self = this;
        this.url = url;

        $(element).load(url + '?status=' + status, function () {
            var fromDateInput = document.getElementById(status + 'driveDateFrom');
            var toDateInput = document.getElementById(status + 'driveDateTo');
            var employeeSearch = document.getElementById(status + 'searchEmployee');
            var orgUnitSearch = document.getElementById(status + 'searchOrgUnit');

            self.loadTable(url, status, personal);
            if (employeeSearch) {
                $(employeeSearch).on("change", function (e) {
                    self.tables[status].employeeSearch = e.target.value;
                    self.tables[status].employeeSearchText = $(e.target).children("option:selected").text();
                    self.updateTable(status, url, personal);
               });
                //assumes that orgUnitSearch exists since they're used in tandem. if this changes it'll break.
                initSelect2ListFragment(orgUnitSearch.id, employeeSearch.id);
            }

            if (orgUnitSearch) {
                $(orgUnitSearch).on('change', function (e) {
                    self.tables[status].orgUnitSearch = e.target.value;
                    self.tables[status].orgUnitSearchText = $(e.target).children("option:selected").text();
                    self.updateTable(status, url, personal);
                });
            }

            if (fromDateInput) {
                fromDateInput.addEventListener('change', function (e) {
                    self.tables[status].fromDate = e.target.value;
                    self.updateTable(status, url, personal);
                });
            }
            if (toDateInput) {
                toDateInput.addEventListener('change', function (e) {
                    self.tables[status].toDate = e.target.value;
                    self.updateTable(status, url, personal);
                });
            }
        });
        window.onbeforeunload = function () {
            reportListFragmentService.storeFilterInLocalStorage();
        }
    }

    this.storeFilterInLocalStorage = function () {
        let self = this;
        var status = $("div.tab-pane.active > div.panel-body > div.report-list-panel").data("status");
        var filters = {
            fromDate:               self.tables[status].fromDate,
            toDate:                 self.tables[status].toDate,
            employee:               self.tables[status].employeeSearch,
            employeeSearchText:     self.tables[status].employeeSearchText,
            orgUnit:                self.tables[status].orgUnitSearch,
            orgUnitSearchText:      self.tables[status].orgUnitSearchText,
        }
        localStorage.setItem(window.location.pathname + "_table_filter", JSON.stringify(filters));
    }

    this.openMobileReport = function (id) {
        if ($("#alertOnAppReportEditing").val() != "true") {
            window.location.replace("/report/" + id);
        } else {
            swal({
                    title: 'Redigering af app-indberetning',
                    text: "OBS: ved redigering af indberetninger fra mobil laves ruten ud fra de angivende adresser og almindelig ruteberegning og ikke de tilhørende GPS målinger. Vær derfor ekstra opmærksom på at din indberetning er korrekt efter redigering",
                    showCancelButton: true,
                    confirmButtonColor: "#1ab394",
                    confirmButtonText: "Redigér",
                    cancelButtonText: "Annullér",
                    closeOnConfirm: true,
                    closeOnCancel: true
                },
                function(isConfirm) {
                    if(isConfirm) {
                        window.location.replace("/report/" + id);
                    }
                }
            );
        }
    }
    this.loadTable = function (url, status, isPersonal) {
        var self = this;
        self.tables[status] = self.tables[status] || {};

        // Initialize DataTable
        self.tables[status].instance = $('#' + status + 'Table').DataTable({
            "serverSide": true,
            "stateSave": true,
            "lengthMenu": [ 10, 25, 50, 100, 250, 500 ],
            "destroy": true,
            "ajax": {
                "url": self.buildUrl(url, status),
                "type": "POST",
                "headers": {
                    "X-CSRF-TOKEN": token
                },
                "data": function (d) {
                    return JSON.stringify(d);
                },
                "contentType": "application/json"
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

                // KM efter merkørsel
                kmPageTotal = api
                    .column(9, { page: 'current' })
                    .data()
                    .reduce(function (a, b) {
                        return intVal(a) + intVal(b);
                    }, 0);

                $(api.column(9).footer()).html(
                    Math.round((kmPageTotal + Number.EPSILON) * 100) / 100
                 + ' km'
                );

                // Penge til udbetaling
                moneyPageTotal = api
                    .column(10, { page: 'current' })
                    .data()
                    .reduce(function (a, b) {
                        return intVal(a) + intVal(b);
                    }, 0);

                $(api.column(10).footer()).html(
                    Math.round((moneyPageTotal + Number.EPSILON) * 100) / 100 + ' kr.'
                );

                // MK trukket
                mkPageTotal = api
                    .column(11, { page: 'current' })
                    .data()
                    .reduce(function (a, b) {
                        return intVal(a) + intVal(b);
                    }, 0);

                $(api.column(11).footer()).html(
                    Math.round((mkPageTotal + Number.EPSILON) * 100) / 100
                    + ' km'
                );
            },
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
                "infoFiltered": "(ud af _TOTAL_ rækker)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                },
            },
            "order": [[4, 'desc']],
            "columns": [
                { "data": 'id',
                  "visible": status === "PENDING" && !isPersonal,
                  "orderable": false,
                  "render": function (data, type, row) {
                    return '<input class="select-row-boxes" type="checkbox" data-id="' + row.id + '"></input>';
                    }
                },
                { "data": 'fullName' },
                { "data": 'orgunitName',
                  "visible": !isPersonal
                },
                { "data": 'employeeNumber' },
                { "data": 'driveDate', "render": function (data, type, row) {
                    if (type === 'display') {
                        return '<span class="hover-text" data-modtext="' + data + ' "> ' + moment(data).format("DD-MM-YYYY") + ' </span>';
                    }
                    return data;
                    }
                },
                { "data": 'purpose' },
                { "data": 'kmRateType' },
                {
                    "data": 'id',
                    "render": function (data, type, row) {
                        let text = "";
                        if (row.addresses && row.addresses.trim() !== '') {
                            let modText = row.addresses.split("\n");
                            if(modText && modText.length >= 2) {
                                text += "Fra: ";
                                if (row.startsAtHome) {
                                  text += '<i class="fa fa-fw fa-home" aria-hidden="true"></i>'
                                }
                                text += modText[0] + "<br>Til:  ";

                                if (row.endsAtHome && !row.roundTrip) {
                                  text += '<i class="fa fa-fw fa-home" aria-hidden="true"></i>'
                                }
                                text += modText[modText.length-1];
                                if (modText.length > 2) {
                                    text += '<br>Via <span class="hover-text" data-modtext="' + modText + '">' + (modText.length - 2) + ' punkt(er)</span>';

                                    $(document).on('mouseenter', '.hover-text', function(event) {
                                        // Each row has unique entries therefore we need to get it again
                                        let tooltipText = $(this).data('modtext');

                                        $('body').append('<div class="custom-tooltip">' + tooltipText.replace(/,/g, "") + '</div>');

                                        $('.custom-tooltip').css({
                                            position: 'absolute',
                                            top: event.pageY + 10 + 'px',
                                            left: event.pageX + 10 + 'px',
                                            padding: '5px',
                                            backgroundColor: '#333',
                                            color: '#fff',
                                            borderRadius: '5px',
                                            fontSize: '12px',
                                            zIndex: 1000,
                                            maxWidth: '200px',
                                            wordWrap: 'break-word'
                                        });
                                    });

                                    $(document).on('mouseleave', '.hover-text', function() {
                                        $('.custom-tooltip').remove();
                                    });

                                }
                            }
                        }
                        return text;
                    }
                },
                {
                    "data": 'id', "render": function (data, type, row) {
                        var htmlString = '';
                        if (row.routeGeometry) {
                            htmlString += '<i class="fa fa-fw fa-2x fa-globe" aria-hidden="true" style="cursor:pointer" data-id="' + row.id + '" onclick="showRouteService.open(this)"></i>';
                        }

                        if (row.comment && row.comment.trim() !== '' && row.comment !== "Ingen kommentar indtastet") {
                            htmlString += '<i class="fa fa-fw fa-2x fa-comment" aria-hidden="true" data-toggle="tooltip" data-placement="top" title="" data-original-title="' + row.comment + '"></i>';
                        }

                        if (row.fromApp) {
                            htmlString += '<i class="fa fa-fw fa-2x fa-mobile" aria-hidden="true"></i>';
                        }

                        if (row.divergentAddress) {
                            htmlString += '<i class="fa fa-fw fa-2x fa-tag" aria-hidden="true"></i>';
                        }

                        if (row.userComment && row.userComment.trim() !== '') {
                            htmlString += '<i class="fa fa-fw fa-2x fa-user-times" aria-hidden="true" title="Begrundelse for afvisning:\n' + row.userComment + '"></i>';
                        }

                        if (row.roundTrip) {
                            htmlString += '<i class="fa fa-fw fa-2x fa-exchange" aria-hidden="true" title="Ruten er tur/retur"></i>';
                        }

                        if (row.error_code != null) {
                            htmlString += '<i class="fa fa-fw fa-2x fa-exclamation-triangle"  style="color: #FFCC00" aria-hidden="true" data-toggle="tooltip" data-placement="top" title="" data-original-title="Der er sket fejl ved indsendelse af din rapport til OPUS, venligst kontakt din IT-Afdeling for flere informationer"></i>';
                        }
                        return htmlString;

                    }
                },
                { "data": 'distance',
                  "render": function(data, type, row) {
                    return (Math.round(data * 100) / 100).toFixed(2);
                  }
                },
                { "data": 'amountToReimburse' },
                { "data": 'extraDistanceAmount',
                  "render": function(data, type, row) {
                    return (Math.round(data * 100) / 100).toFixed(2);
                  }
                },
                { "data": 'fourKmRule', "render": function(data, type, row) {
                    return data ? "Ja" : "Nej";
                } },
                { "data": 'createdDate', "render": function (data, type, row) {
                    if (type === 'display') {
                        return '<span class="hover-text" data-modtext="' + data + ' "> ' + moment(data).format("DD-MM-YYYY") + ' </span>';
                    }
                    return data;
                    }
                },
                { "data": 'approvedByName', "visible": status !== "PENDING" },
                { "data" : 'potentialApprovers',
                  "visible": !isPersonal && status === "PENDING",
                  "render": function (data, type, row) {
                        if (!row.potentialApprovers) {
                            return row.potentialApprovers;
                        }

                        let approvers = row.potentialApprovers.split(",");
                        let approversStr = "";
                        for (let i = 0; i < approvers.length && i < 5; i++) {
                          approversStr += approvers[i] + "<br>";
                        }

                        return approversStr;
                    }
                },
                {
                    "data": "closedDate",
                    "visible": (status === "ACCEPTED"),
                    "render": function (data, type, row) {
                        return '<span class="hover-text" data-modtext="' + data + ' "> ' + moment(data).format("DD-MM-YYYY") + ' </span>';
                    }
                },
                {
                    "data": "processedDate",
                    "visible": (status === "ACCEPTED"),
                    "render": function (data, type, row) {
                        if (data) {
                            return '<i class="fa fa-fw fa-2x fa-check" aria-hidden="true" data-toggle="tooltip" data-placement="top" title="" data-original-title="' + moment(data).format('DD-MM-YYYY HH:mm:ss') + '"></i>';
                        }
                        return data;
                    }
                },
                {
                    "data": "id", "render": function (data, type, row) {
                        var htmlString = '';

                        //not personal means it's either admin or approver page
                        if (status === "PENDING") {

                            if (!isPersonal && isPersonal !== undefined && isPersonal !== null) {

                                //Approve
                                htmlString += `<a data-id="${row.id}" onclick="reportListFragmentService.openApproveModal(${row.id})" style="color: black; margin-left: 5%" title="Godkend"><em class="fa fa-fw fa-lg fa-check"></em></a>`

                                //Reject
                                htmlString += `<a data-id="${row.id}" onclick="reportListFragmentService.reject(${row.id})" style="color: black; margin-left: 5%" title="Afvis"><em class="fa fa-fw fa-lg fa-times"></em></a>`

                                //Delete
                                if(url.includes("admin")) {
                                    htmlString += `<a data-id="${row.id}" onclick="reportListFragmentService.del(${row.id})" style="color: black; margin-left: 5%" title="Slet"><em class="fa fa-fw fa-lg fa-trash"></em></a>`
                                }


                            } else {
                                if ($("#allowNewReportingToggle").val() != "false") {
                                    if (row.fromApp) {
                                        htmlString += `<a style="color: black; margin-left: 5%" title="Rediger" onclick="reportListFragmentService.openMobileReport(${row.id})"><em class="fa fa-fw fa-lg fa-pencil"></em></a>`;
                                    }
                                    else {
                                        htmlString += `<a href="/report/${row.id}" style="color: black; margin-left: 5%" title="Rediger"><em class="fa fa-fw fa-lg fa-pencil"></em></a>`;
                                    }
                                    htmlString += `<a th:data-id="${row.id}" onclick="reportListFragmentService.del(${row.id})" style="color: black; margin-left: 5%;" title="Slet"><em class="fa fa-fw fa-lg fa-trash"></em></a>`
                                }
                            }
                        }

                        if(status === "ACCEPTED") {
                            if (!isPersonal && isPersonal !== undefined && isPersonal !== null) {
                                if(url.includes("admin")) {
                                    htmlString += `<a th:data-id="${row.id}" onclick="reportListFragmentService.rejectAcceptedReport(${row.id})" style="color: black; margin-left: 5%" title="Afvis"><em class="fa fa-fw fa-lg fa-times"></em></a>`
                                }
                            }
                        }

                        if(status === "REJECTED") {
                            if (!isPersonal && isPersonal !== undefined && isPersonal !== null) {
                                //Approve
                                if (row.status === "REJECTED") {
                                    htmlString += `<a data-id="${row.id}" onclick="reportListFragmentService.openApproveModal(${row.id})" style="color: black; margin-left: 5%" title="Godkend"><em class="fa fa-fw fa-lg fa-check"></em></a>`
                                }
                            }
                            if (isPersonal) {
                                if (row.status === "REJECTED") {
                                    if ($("#allowNewReportingToggle").val() != "false") {
                                        if (row.fromApp) {
                                            htmlString += `<a style="color: black; margin-left: 5%" title="Rediger" onclick="reportListFragmentService.openMobileReport(${row.id})"><em class="fa fa-fw fa-lg fa-pencil"></em></a>`;
                                        }
                                        else {
                                            htmlString += `<a href="/report/${row.id}" style="color: black; margin-left: 5%" title="Rediger"><em class="fa fa-fw fa-lg fa-pencil"></em></a>`;
                                        }
                                    }
                                }
                            }

                            //Delete
                            if ((url.includes("admin") || isPersonal) && row.status !== "REJECTED_AFTER_INVOICE") {
                                htmlString += `<a data-id="${row.id}" onclick="reportListFragmentService.del(${row.id})" style="color: black; margin-left: 5%" title="Slet"><em class="fa fa-fw fa-lg fa-trash"></em></a>`
                            }

                        }
                        return htmlString;
                    }
                },
                {
                    "data": 'id',
                    'visible': false
                },
                {
                    "data": 'fromApp',
                    "visible": false
                },
                {
                    "data": 'divergentAddress',
                    "visible": false
                }
                ]
        });

        $('#' + status + 'Table').on( 'draw.dt', function () {
          $('[data-toggle="tooltip"]').tooltip();
        });

        $(".checkAllBoxes").off();
        $(".checkAllBoxes").on('click', function() {
            var val = $(this).prop('checked');

            $(this).parents("table").find(".select-row-boxes").each(function() {
                 $(this).prop('checked', val);
             });
        });

        $('#' + status + 'table').on("ready", function () {
            reportListFragmentService.transferFilterSettings();
        });
    }

    this.transferFilterSettings = function () {
        var status = $("div.tab-pane.active > div.panel-body > div.report-list-panel").data("status");
        var filters = localStorage.getItem(window.location.pathname + "_table_filter");
        if (filters !== null && filters !== undefined) {
            filters = JSON.parse(filters);
            this.tables[status].fromDate = filters.fromDate;
            this.tables[status].toDate = filters.toDate;
            this.tables[status].employeeSearch = filters.employee;
            this.tables[status].employeeSearchText = filters.employeeSearchText;
            this.tables[status].orgUnitSearch = filters.orgUnit;
            this.tables[status].orgUnitSearchText = filters.orgUnitSearchText;

            $('#' + status + 'driveDateFrom').val(this.tables[status].fromDate);
            $('#' + status + 'driveDateTo').val(this.tables[status].toDate);

            if (window.location.pathname.includes("admin") || window.location.pathname.includes("approve")) {
                if (this.tables[status].employeeSearchText) {
                    const url = window.location.href;
                    let backendUrl = "Prefix";
                    if (url.includes("approve")) {
                        backendUrl = "Approver";
                    }

                    $.ajax({
                        type: 'GET',
                        url: "/reportCard/getPersonsBy" + backendUrl + "/id?q=" + this.tables[status].employeeSearch,
                    }).then(function (data) {
                        // create the option and append to Select2
                        var option = new Option(data.text, data.id, true, true);
                        $('#' + status + 'searchEmployee').append(option).trigger('change');
                        // manually trigger the `select2:select` event
                        $('#' + status + 'searchEmployee').trigger({
                            type: 'select2:select',
                            params: {
                                data: data
                            }
                        });
                    });
                }
                else {
                    $('#' + status + 'searchEmployee').val(null).trigger("change");
                }
                if (this.tables[status].orgUnitSearchText) {
                    const url = window.location.href;
                    let backendUrl = "Prefix";
                    if (url.includes("approve")) {
                        backendUrl = "Approver";
                    }

                    $.ajax({
                        type: 'GET',
                        url: "/reportCard/getOrgUnitsBy" + backendUrl + "/id?q=" + this.tables[status].orgUnitSearch,
                    }).then(function (data) {
                        // create the option and append to Select2
                        var option = new Option(data.text, data.id, true, true);
                        $('#' + status + 'searchOrgUnit').append(option).trigger('change');
                        // manually trigger the `select2:select` event
                        $('#' + status + 'searchOrgUnit').trigger({
                            type: 'select2:select',
                            params: {
                                data: data
                            }
                        });
                    });
                }
                else {
                    $("#" + status + "searchOrgUnit").val(null).trigger("change");
                }
            }
            this.updateTable(status, this.url);
        }
    }

    this.buildUrl = function (url, status) {
        var self = this;
        var fromDate = self.tables[status].fromDate;
        var toDate = self.tables[status].toDate;
        var parameters = '';
        var employee = self.tables[status].employeeSearch;
        var orgUnit = self.tables[status].orgUnitSearch;

        if (fromDate) {
            parameters += '&startDate=' + fromDate;
        }

        if (toDate) {
            parameters += '&endDate=' + toDate;
        }

        if (employee) {
            parameters += '&employeeSearch=' + employee;
        }

        if (orgUnit) {
            parameters += '&orgUnitSearch=' + orgUnit;
        }

        return '/rest' + url + '?status=' + status + parameters;

    }

    this.updateTable = function (status, url) {
        var self = this;
        var table = self.tables[status];
        var tableInstance = table.instance;

        var filters = {
            fromDate:               table.fromDate,
            toDate:                 table.toDate,
            employee:               table.employeeSearch,
            employeeSearchText:     table.employeeSearchText,
            orgUnit:                table.orgUnitSearch,
            orgUnitSearchText:      table.orgUnitSearchText,
        }
        localStorage.setItem(window.location.pathname + "_table_filter", JSON.stringify(filters));


        if (tableInstance) {
            tableInstance.ajax.url(self.buildUrl(url, status)).load();
        }
    }

    this.rejectAcceptedReport = function (obj) {
        swal({
                title: 'Afvis allerede godkendt indberetning',
                text: "Er du sikker? Denne indberetning er allerede overført til lønsystemet. Indberetningen vil blive overført til løn således at lønsystemet går i nul for denne indberetning.",
                showCancelButton: true,
                type: "input",
                inputPlaceholder: "Begrund afvisning",
                confirmButtonColor: "red",
                confirmButtonText: "Ja",
                cancelButtonText: "Nej",
                closeOnConfirm: false,
                closeOnCancel: true
            },
            function (inputValue) {
                if (inputValue) {
                    if (inputValue.length > 255) {  // Handle too long input
                        swal.showInputError("Du må maks angive 255 tegn");
                        return false;
                    }
                    $.ajax({
                        method: "POST",
                        url: "/rest/report/rejectInvoiced/" + obj,
                        data: inputValue,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        processData: false,
                        success: function (data, textStatus, jqXHR) {
                            reportListFragmentService.storeFilterInLocalStorage();
                            window.location.reload();
                        },
                        error: function (jqXHR, textStatus, errorThrown) {
                            errorResponse(jqXHR);
                        }
                    })
                }
            }
        );
    }

    this.openApproveModal = function (obj, singleApprove = true) {
        id = obj;
        approveMultiple = !singleApprove;

        if (approveMultiple) {
            $("#singleApproveheader").hide();
            $("#multipleApproveheader").show();
        }
        else {
            $("#singleApproveheader").show();
            $("#multipleApproveheader").hide();
        }

        //Clear all the values before displaying
        $('#inputAccount').val("");
        $("#inputType").val($("#inputType option:first").val());

        $('#approveModal').modal("show");

        $('#approveModal input').bind('keypress', function(e) {
            var code = e.keyCode || e.which;
            if(code == 13) {
                reportListFragmentService.approve(obj);
            }
        });

        $('#inputAccountDiv').hide();

        $('#inputType').on("change", function () {
            if ($('#inputType').val() != "Ingen afvigende kontering") {
                $('#inputAccountDiv').show();
                $('#inputAccount:input:enabled:visible:first').focus();
                $('#inputLabel').text($('#inputType option:selected').text() + ":");
            }
            else {
                $('#inputAccountDiv').hide();
            }
        });
    }

    this.closeApproveModal = function () {
        $('#approveModal').modal("hide");
    }

    this.edit = function (obj) {
        swal({
            title: 'Slet indberetning',
            text: "er du sikker?",
            showCancelButton: true,
            confirmButtonColor: "red",
            confirmButtonText: "Ja",
            cancelButtonText: "Nej",
            closeOnConfirm: true,
            closeOnCancel: true
        },
            function (isConfirm) {
                if (isConfirm) {
                    $.ajax({
                        method: "POST",
                        url: "/rest/report/delete",
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        processData: false,
                        data: $(obj).data("id"),
                        success: function (data, textStatus, jqXHR) {
                            reportListFragmentService.storeFilterInLocalStorage();
                            window.location.reload();
                        },
                        error: function (jqXHR, textStatus, errorThrown) {
                            errorResponse(jqXHR);
                        }
                    })
                }
            }
        );

    }

    this.del = function (obj) {
        swal({
            title: 'Slet indberetning',
            text: "er du sikker?",
            showCancelButton: true,
            confirmButtonColor: "red",
            confirmButtonText: "Ja",
            cancelButtonText: "Nej",
            closeOnConfirm: true,
            closeOnCancel: true
        },
            function (isConfirm) {
                if (isConfirm) {
                    $.ajax({
                        method: "POST",
                        url: "/rest/report/delete",
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        processData: false,
                        data: obj,
                        success: function (data, textStatus, jqXHR) {
                            reportListFragmentService.storeFilterInLocalStorage();
                            window.location.reload();
                        },
                        error: function (jqXHR, textStatus, errorThrown) {
                            toastr.warning("Der er sket en teknisk fejl");
                        }
                    })
                }
            }
        );

    }

    this.reject = function(id) {
        swal({
            title: 'Afvis indberetning',
            text: "Angiv begrundelse for afvisning",
            type: "input",
            inputPlaceholder: "Begrund afvisning",
            showCancelButton: true,
            confirmButtonColor: "red",
            confirmButtonText: "Afvis",
            cancelButtonText: "Annuller",
            closeOnConfirm: false,
            closeOnCancel: true
        },
        function (inputValue) {
            if (inputValue === false) return false;  // Handle cancel action

            if (inputValue === "") {  // Handle empty input
                swal.showInputError("Du skal angive en begrundelse!");
                return false;
            }
            if (inputValue.length > 255) {
                swal.showInputError("Din afvisninstekst skal må ikke være mere end 255 tegn")
                return false;
            }

            let data = {
                "reportId": id,
                "type": null,
                "value": inputValue
            }

            $.ajax({
                method: "POST",
                url: "/rest/report/reject",
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                processData: false,
                data: JSON.stringify(data),
                success: function (data, textStatus, jqXHR) {
                    toastr.success("Rapport afvist");
                    reportListFragmentService.storeFilterInLocalStorage();
                    window.location.reload();
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    errorResponse(jqXHR);
                }
            })

            swal.close();  // Close the alert manually
        });
    }

    this.clearValues = function (element) {
        const url = window.location.href;

        var status = $(element).data("status");

        $('#' + status + 'driveDateFrom').val("");
        this.tables[status].fromDate = undefined;

        $('#' + status + 'driveDateTo').val("");
        this.tables[status].endDate = undefined;
        this.tables[status].toDate = undefined;

        if (url.includes("admin") || url.includes("approve")) {
            $('#' + status + 'searchEmployee').val("").change();
            $('#' + status + 'searchOrgUnit').val("").change();
            this.tables[status].employeeSearch = undefined;
            this.tables[status].orgUnitSearch = undefined;
        }
        var table = this.tables[status].instance;
        table.ajax.url(this.buildUrl(this.url, status)).load();
    }

    // Returns url for select2
    function getOrgUnitsByPersonId(backendUrl) {
        var status = $("div.tab-pane.active > div.panel-body > div.report-list-panel").data("status");
        var personId = reportListFragmentService.tables[status].employeeSearch;
        if (personId === undefined) {
            personId = "";
        }
        return "/reportCard/getOrgUnitsBy" + backendUrl + "?personId=" + personId;
    }

    // Returns url for select2
    function getPersonsByOrgUnitId(backendUrl) {
        var status = $("div.tab-pane.active > div.panel-body > div.report-list-panel").data("status");
        var orgUnitId = reportListFragmentService.tables[status].orgUnitSearch;
        if (orgUnitId === undefined) {
            orgUnitId = "";
        }

        return "/reportCard/getPersonsBy" + backendUrl + "?orgUnitId=" + orgUnitId;
    }

    function initSelect2ListFragment(orgUnitElementId, personElementId) {
        const url = window.location.href;
        let backendUrl = "Prefix";
        if (url.includes("approve")) {
            backendUrl = "Approver";
        }

        $('#'+personElementId).select2({
            placeholder: "indtast navn",
            allowClear: true,
            minimumInputLength: 2,
            ajax: {
                url: () => getPersonsByOrgUnitId(backendUrl),
                dataType: "json",
                delay: 350 // wait a bit so we get the right input
            }
        });

        $('#'+orgUnitElementId).select2({
            placeholder: "indtast organisationsenhed",
            allowClear: true,
            minimumInputLength: 2,
            ajax: {
                url: () => getOrgUnitsByPersonId(backendUrl),
                dataType: "json",
                delay: 350 // wait a bit so we get the right input
            }
        });

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
                    $('#' +personElementId).find("option").each(function () {
                        if (!indexes.includes($(this).val())) {
                            $(this).hide();
                        }
                        else {
                            $('#'+ele).val($(this).val());
                        }
                    });
                }
            });
        }
    }

    this.approve = function (obj) {
        let select = $('#inputType').val();
        if (select == "COST_CENTER") {
            if ($('#inputAccount').val().length == 0 || $('#inputAccount').val().length > 10) {
                toastr.warning("For langt eller ingen input!");
                return;
            }
        }

        if (select == "PSP_ELEMENT") {
            if ($('#inputAccount').val().length != 19) {
                toastr.warning("Input skal være 19 cifre langt!");
                return;
            }
        }

        let data;
        if (approveMultiple) {
            var reports = [];

            $(".select-row-boxes").each(function() {
                if($(this).prop('checked')) {
                    var report = JSON.stringify({
                        "reportId": $(this)[0].dataset.id,
                        "type": select,
                        "value": $('#inputAccount').val()
                    });
                    reports.push(report);
                }
            });

            data = "[" + reports + "]";
        }
        else {
            data = JSON.stringify({
                "reportId": id,
                "type": select,
                "value": $('#inputAccount').val()
            });
        }

        $.ajax({
            method: "POST",
            url: approveMultiple ? "/rest/report/approveMultiple" : "/rest/report/approve" ,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: data,
            success: function (data, textStatus, jqXHR) {
                toastr.success(approveMultiple ? "Rapporter godkendt" : "Rapport godkendt");
                reportListFragmentService.storeFilterInLocalStorage();
                window.location.reload();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                errorResponse(jqXHR);
            }
        })
    }

    this.approveMultiple = function () {
        var reports = [];

        $(".select-row-boxes").each(function() {
            if($(this).prop('checked')) {
                var report = JSON.stringify({
                    "reportId": $(this)[0].dataset.id,
                    "type": "NONE",
                    "value": ""
                });
                reports.push(report);
            }
        });

        $.ajax({
            method: "POST",
            url: "/rest/report/approveMultiple",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            processData: false,
            data: "[" + reports + "]",
            success: function (data, textStatus, jqXHR) {
                toastr.success("Rapporter godkendt");
                reportListFragmentService.storeFilterInLocalStorage();
                window.location.reload();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                errorResponse(jqXHR);
            }
        });
    }
}
