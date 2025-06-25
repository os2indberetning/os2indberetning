var token = $("meta[name='_csrf']").attr("content");

var reportListFragmentService;
var personalService;
var reportCardService;
var notificationService;

$(document).ready(function() {
    reportListFragmentService = new ReportListFragmentService();
    personalService = new PersonalService();
    personalService.init();
    notificationService = new NotificationService();
    notificationService.getNotification();
});

function PersonalService() {
    this.init = function () {
        $(".report-list-panel").each( function(index) {
            reportListFragmentService.init("/personal/report/list", $(this));
        });

        // store the currently selected tab in the hash value
        $("ul.rememberPosition > li > a").on("shown.bs.tab", function(e) {
            reportListFragmentService.transferFilterSettings();
        });
    }
}
