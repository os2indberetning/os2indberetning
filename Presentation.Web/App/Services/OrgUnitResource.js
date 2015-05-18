﻿angular.module("application").service('OrgUnit', ["$resource", function ($resource) {
    return $resource("/odata/OrgUnits(:id)", { id: "@id" }, {
        "get": { method: "GET", isArray: false },
        "patch": { method: "PATCH", isArray: false },
        "getWhereUserIsLeader": {
            method: "GET",
            isArray: true,
            url: "/odata/Person(:id)/Employments?$expand=OrgUnit&$filter=IsLeader eq true&$select=OrgUnit",
            transformResponse: function (data) {
                debugger;
                var res = [];
                angular.forEach(angular.fromJson(data).value, function (value, key) {
                    res.push(value.OrgUnit);
                });
                return res;
            }
        }
    });
}]);

