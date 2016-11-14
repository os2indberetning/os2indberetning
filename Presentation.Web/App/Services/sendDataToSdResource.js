angular.module("application").service('sendDataToSd', ["$resource", function ($resource) {
    return $resource("/api/sendDataToSd", { id: "@id" }, {
        "sendDataToSd": { method: "GET" },
    });
}]);