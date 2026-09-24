var notificationService;
var token = $("meta[name='_csrf']").attr("content");

$(document).ready(function() {
    notificationService = new NotificationService();
    notificationService.getNotification();
});

function NotificationService() {
    this.getNotification = function () {
        let notification = localStorage.getItem("notification");
        $.ajax({
            method : "POST",
            url: "/rest/notification/available",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function(data, textStatus, jqXHR) {
                if (notification === data || data.length === 0) {
                    return;
                }
                swal({
                        title: "System besked",
                        html: true,
                        text: data,
                        confirmButtonColor: "#1ab394",
                        confirmButtonText: "Forstået",
                        closeOnConfirm: true,
                        closeOnCancel: true
                    },
                    function(isConfirm) {
                        if(isConfirm) {
                            localStorage.setItem("notification", data);
                        }
                    }
                );
            }
        })
    }
}