
var cmsEditService;
$(document).ready(function() {
    cmsEditService = new CMSEditService();
    cmsEditService.init();

});

function CMSEditService() {
    this.init = function () {
        // Init summernote input field
        if ($('#key').val() != 'cms.logo' && $('#key').val() != 'cms.help.email.deadline.subject' && $('#key').val() != 'cms.help.email.deadline.body') {
            $('#content').summernote({
                "height": 320,
                "toolbar": [
                    [ "font", [ "bold", "italic", "underline" ]],
                    [ "para", [ "ul", "ol", "style" ]],
                    [ "insert", ["link"]]
                ]
            });
        }
    }

    this.removeLogo = function() {
        document.getElementById("cmsLogoForm").reset();
        document.getElementById("cmsLogoForm").submit();
    }
}