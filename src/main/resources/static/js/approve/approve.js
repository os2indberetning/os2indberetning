var token = $("meta[name='_csrf']").attr("content");

var reportListFragmentService;
var approveService;
var notificationService;
$(document).ready(function() {
    reportListFragmentService = new ReportListFragmentService();
    approveService = new ApproveService();
    approveService.init();
    notificationService = new NotificationService();
    notificationService.getNotification();
    // This is the absolute minimum amount. If we want simple page loading to work
    setTimeout(() => {
        reportListFragmentService.transferFilterSettings();
    }, 100);});

function ApproveService() {
    this.init = function () {
        $(".report-list-panel").each( function(index) {
            reportListFragmentService.init("/approve/report/list", $(this));
        })

        // store the currently selected tab in the hash value
        $("ul.rememberPosition > li > a").on("shown.bs.tab", function(e) {
            reportListFragmentService.transferFilterSettings();
        });
    }
}



