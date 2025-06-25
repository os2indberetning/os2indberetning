var token = $("meta[name='_csrf']").attr("content");

var reportListFragmentService;
var showRouteService;
var addressService;
var map;

$(document).ready(function() {
    reportListFragmentService = new ReportListFragmentService();
    showRouteService = new ShowRouteService();
    addressService = new AddressService();
});

function ShowRouteService() {
    var routeId = 0;

    this.loadFragment = function() {
        $('#showRouteDiv').load('/approve/mapFragment/' + showRouteService.routeId, showRouteService.initFrag);
    }

    this.initFrag = function() {
        map = addressService.mapInit([$("#map").data('lat'), $("#map").data('lng')]);
        map.invalidateSize();
        var routeCoordinates = JSON.parse($("#mapFragRoute").val())
        var routeLine = L.geoJSON(routeCoordinates.geometry).addTo(map);
        map.fitBounds(routeLine.getBounds());
        $("#interestmarkers").children().each(function() {
            L.marker([this.dataset.lat, this.dataset.lng], { title: this.dataset.address}).addTo(map);
        });
    }

    this.close = function() {
        $("#mapFragModal").modal("hide");
    }

    this.open = function(element) {
        showRouteService.routeId = element.dataset.id;
        this.loadFragment();
        $("#mapFragModal").modal("show");
    }
}