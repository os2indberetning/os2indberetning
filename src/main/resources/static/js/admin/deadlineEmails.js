$(document).ready(function() {
    loadDeadlineFragment();
})

var token = $("meta[name='_csrf']").attr("content");


function loadDeadlineFragment() {
    $('#deadlineFragmentDiv').load('/admin/deadlineFragment', function() {
        var today = new Date().toISOString().split('T')[0];
        $("#deadline")[0].setAttribute('min', today);
        $("#first")[0].setAttribute('min', today);
    });

}

function showDeadlineModal() {
    $("#deadlineId").val(0);
    $("#msg").val($("#defaultMessage").text());
    $("#subject").val($("#defaultSubject").val());
    $("#first").val("");
    $("#deadline").val("");
    $("#repeat1").prop( "checked", false);
    $("#repeat1").iCheck("update");
    $('#createDeadlineNotificationModal').modal('show');
}

function editDeadline(obj) {
    obj = $(obj);
    let msg = obj.data("msg").replaceAll("<br>", "\n");
    $("#deadlineId").val(obj.data("id"));
    $("#msg").val(msg);
    $("#subject").val(obj.data("subject"));
    $("#first").val(obj.data("next"));
    $("#deadline").val(obj.data("deadline"));
    $("#repeat1").prop("checked", obj.data("repeat"));
    $("#repeat1").iCheck("update");
    $('#createDeadlineNotificationModal').modal('show');
}

function closeDeadlineModal() {
    $('#createDeadlineNotificationModal').modal('hide');
}

function submitDeadlineModal() {
 data = {
        "deadlineId": $("#deadlineId").val(),
        "subject": $("#subject").val(),
        "msg": $("#msg").val(),
        "deadline": $("#deadline").val(),
        "first": $("#first").val(),
        "repeat": $("#repeat1").is(":checked")
    }

    $.ajax({
            method : "POST",
            url: "/rest/deadline/create",
            data: JSON.stringify(data),
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function(data, textStatus, jqXHR) {
                $('#createDeadlineNotificationModal').modal('hide');
                loadDeadlineFragment();
                toastr.success("Emailadvisering oprettet");
            },
            error: function(jqXHR, textStatus, errorThrown) {
                if(jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.defaultMessage) {
                    toastr.error(jqXHR.responseJSON.defaultMessage);
                }
                else {
                    toastr.error("Fejl. Emailadvisering blev ikke oprettet.")
                }
            }
        });
    return false;
}

function deleteDeadlineNotification(id) {
    $.ajax({
        method : "POST",
        url: "rest/deadline/delete/" + id,
        headers: {
            "content-type": "application/json",
            'X-CSRF-TOKEN': token
        },
        success: function(data, textStatus, jqXHR) {
            toastr.success("Emailadvisering blev slettet.")
            loadDeadlineFragment();
        },
        error: function(jqXHR, textStatus, errorThrown) {
            toastr.error("Fejl. emailadvisering blev ikke slettet.")
        }
    })
}