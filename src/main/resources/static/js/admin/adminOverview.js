var token = $("meta[name='_csrf']").attr("content");

var emailNotificationService;
var adminOverviewService;
$(document).ready(function () {
    adminOverviewService = new AdminOverviewService();
    adminOverviewService.init();

    emailNotificationService = new EmailNotificationService();
});

function AdminOverviewService() {
    this.init = function () {
        this.loadFragment();
    }

    this.loadFragment = function () {
        $('#adminOverviewFragmentDiv').load("/admin/overview", this.initTable);
    }

    this.initTable = function () {
        $('.i-checks').iCheck({
            checkboxClass: 'icheckbox_square-blue',
            radioClass: 'iradio_square-blue',
        });
        var notificationTable = $('#adminOverviewTable').DataTable({
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
        $.each($('.input-filter', notificationTable.table().footer()), function() {
            var column = notificationTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }
}

function EmailNotificationService() {

    this.updateFlag = function (element) {
        var personId = $(element).data("id");
        $.ajax({
            method : "POST",
            url: "/rest/admin/updateEmailNotification/" + personId,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function() {
                toastr.success("Mail advisering ændret");
                adminOverviewService.init();
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en teknisk fejl.");
            }
        });
    }
}