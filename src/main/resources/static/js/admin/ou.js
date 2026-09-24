var token = $("meta[name='_csrf']").attr("content");
var ouService;
var addressService;

$(document).ready(function() {
    ouService = new OuService();
    addressService = new AddressService();
    ouService.init();
});

function OuService() {

    var rule;
    var id;

    this.init = function () {
        this.loadFragment();
    }

    this.loadFragment = function () {
        $('#ouFragmentDiv').load('/admin/ouFragment', this.initFragment);
    }

    this.openModal = function (obj) {
        $('#orgUnitModal input').bind('keypress', function(e) {
            var code = e.keyCode || e.which;
            if(code == 27) {
                ouService.closeModal();
            }
        });
        // values from orgunit
        rule = $(obj).data("rule");
        id = $(obj).data("orgid");
        let rate = $(obj).data("rate");
        let type = $(obj).data("type");

        // adjust the default-values
        $('#calculationTypeSelect').val(type);

        // Set rate type - handle null/undefined
        if (rate) {
            $('#rateTypeSelectOuEdit').val(rate);
        } else {
            $('#rateTypeSelectOuEdit').prop('selectedIndex', 0);
        }

        if (rule) {
            $('#kmRuleCheck').iCheck('check');
        }
        else {
            $('#kmRuleCheck').iCheck('uncheck');
        }

        $("#orgUnitModal").modal("show");
    }

    this.closeModal = function () {
        $("#orgUnitModal").modal("hide");
        this.loadFragment();
    }

    this.initFragment = function () {
        ouService.initDatatable();

        $('.i-checks').iCheck({
            checkboxClass: 'icheckbox_square-blue',
            radioClass: 'iradio_square-blue',
        });
    }

    this.initDatatable = function () {
        var ouTable = $('#ouTable').DataTable({
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
            }
        });
        $.each($('.input-filter', ouTable.table().footer()), function () {
            var column = ouTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.saveChanges = function () {
        var rule = $('#kmRuleCheck').is(":checked");
        var type = $('#calculationTypeSelect option:selected').val();
        var rate = $('#rateTypeSelectOuEdit option:selected').val();

        var data = {
            orgId: id,
            kmRule: rule,
            calculationType: type,
            rateTypeId: (rate && rate !== '') ? parseInt(rate) : null
        }

        $.ajax({
            method : "POST",
            url: "admin/orgunit-edit",
            contentType: 'application/json',
            headers: {
                'X-CSRF-TOKEN': token
            },
            data:JSON.stringify(data),

            success: function(data, textStatus, jqXHR){
                ouService.closeModal();
                toastr.success("Afdeling redigeret...");
                ouService.loadFragment();
            },
            error: function(jqXHR, textStatus, errorThrown) {
                errorResponse(jqXHR);
            }
        });
    }
}
