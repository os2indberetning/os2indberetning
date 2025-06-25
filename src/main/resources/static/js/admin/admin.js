var token = $("meta[name='_csrf']").attr("content");

var cmsService;
var reportListFragmentService;
var reportService;
var personalService;
var notificationService;
$(document).ready(function () {
    reportListFragmentService = new ReportListFragmentService();
    reportService = new ReportService();
    reportService.init();
    notificationService = new NotificationService();
    notificationService.getNotification();
    cmsService = new CmsService();
    cmsService.init();
    // This is the absolute minimum amount. If we want simple page loading to work
    setTimeout(() => {
        reportListFragmentService.transferFilterSettings();
    }, 100);

    personalService = new PersonalService();
    personalService.init();
});

function ReportService() {
    this.init = function () {
        $(".report-list-panel").each( function(index) {
            reportListFragmentService.init("/admin/report/list", $(this));
        })

        // store the currently selected tab in the hash value
        $("div > div.panel-body > div.tabs-container > ul.nav.nav-tabs > li > a").on("shown.bs.tab", function(e) {
            reportListFragmentService.transferFilterSettings();
        });
    }
}

function PersonalService() {
    this.init = function () {
        setTimeout(() => {
            let existing = localStorage.getItem("ADMIN/activeTab");
            if (existing) {
                $('#' + existing).tab('show');
            } else {
                $('.nav-tabs a:first').tab('show');
            }

            $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
                if ($(e.target).attr("class").includes("rememberInner")) {
                    const activeTabId = $(e.target).attr('href').substring(1).split("status-")[1];
                    localStorage.setItem('ADMIN/activeTab', activeTabId);
                }
            });
        }, 50) // There are init issues, this is the lowest i can go with the timeout
    }
}

function CmsService() {
    this.init = function () {
        var table = $('#cmsTable').DataTable({
            "bSort": false,
            "paging": false,
            "responsive": true,
            "dom": "<'row'<'col-sm-12'tr>>",
            "language": {
                "search":	   "Søg",
                "lengthMenu":   "_MENU_ rækker per side",
                "info":		 "Viser _START_ til _END_ af _TOTAL_ rækker",
                "zeroRecords":  "Ingen data...",
                "infoEmpty":	"Henter data...",
                "infoFiltered": "(ud af _MAX_ rækker)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                }
            }
        });
        $.each($('.input-filter', table.table().footer()), function() {
            var column = table.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }
    
    this.delete = function (element) {
        swal({
                title: 'Slet Hjælpetekst',
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
                        url: "rest/cms/delete/" + element.dataset.id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            toastr.success("CMS slettet");
                            window.location.reload();
                        }
                    });
                }
            }
        )
    }
}



