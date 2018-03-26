angular.module("application").service('FileGenerationSchedule', ["$resource", function ($resource) {
    return $resource("/odata/FileGenerationSchedule(:id)", { id: "@id" }, {
        "get": { method: "GET", isArray: false, transformResponse: function(data) {
            var res = angular.fromJson(data);
            if (res.error == undefined) {
                return res.value[0];
            }

            var modalInstance = $modal.open({
                templateUrl: '/App/Services/Error/ServiceError.html',
                controller: "ServiceErrorController",
                backdrop: "static",
                resolve: {
                    errorMsg: function () {
                        return res.error.innererror.message;
                    }
                }
            });
            return res;
        }},
        "getAll": {
            method: "GET", isArray: false
        },
        "getWithEmailNotifications":{
            method: "GET",
            isArray: false,
            url: "/odata/FileGenerationSchedule?$filter=Id eq :id &$expand=MailNotificationSchedules",
            transformResponse: function(data) {
                var res = angular.fromJson(data);
                if (res.error == undefined) {
                    return res.value[0];
                }
            }
        },
        "patch": { method: "PATCH", isArray: false },
        "post": { method: "POST", isArray: false }
    });
}]);