(function () {

    var pollFactory = function ($scope, $timeout, pollFunc) {
        var currentTimeout = undefined;
        var update = function () {
            if (currentTimeout) {
                $timeout.cancel(currentTimeout);
            }
            pollFunc().then(function () {
                currentTimeout = $timeout(update, 5000);
            });
        };
        $scope.$on('$destroy', function () {
            if (currentTimeout) {
                $timeout.cancel(currentTimeout);
            }
        });
        return update;
    };

    angular
        .module('brewControl', ['ngMaterial'])
        .config(function ($mdThemingProvider) {

            $mdThemingProvider.theme('default')
                .primaryPalette('brown')
                .accentPalette('red');

        });

    angular
        .module('brewControl')
        .controller('FridgeCtrl', function ($scope, $http, $timeout) {
            $scope.data = {
                showEditTargetTemperature: false
            }

            _.each(['coolerEnabled', 'heaterEnabled', 'targetTemperature'], function (e) {
                var url = "/" + e;
                $http.get(url).then(function (response) {
                    $scope.data[e] = eval(response.data);

                    $scope.$watch('data.' + e, function (newValue, oldValue) {
                        if (newValue === oldValue) return;
                        $http.post(url, newValue);
                    });
                });
            });
            //
            //$http.get("/relays").then(function (response) {
            //    $scope.relays = response.data;
            //});

            pollFactory($scope, $timeout, function () {
                return $http.get("/temperatures").then(function (response) {
                    $scope.temperatures = response.data;
                });
            })();

            pollFactory($scope, $timeout, function () {
                return $http.get("/relays").then(function (response) {
                    $scope.relays = response.data;
                });
            })();

        });

    angular
        .module('brewControl')
        .controller('MashCtrl', function ($scope, $http, $timeout, $filter) {

            var updateState = pollFactory($scope, $timeout, function () {
                return $http.get('/mash/state').then(function (response) {
                    $scope.state = response.data;
                });
            });

            $scope.tryPrintTaskEndTime = function(task) {
                if(task.startTime && task.startTime[0] && task.durationInMillis) {
                    return " (bis " + $filter('date')(task.startTime[0]  + task.durationInMillis, 'mediumTime')+")";
                }
                return "";
            }

            $scope.start = function () {
                $http.post('/mash/start').then(function () {
                    updateState();
                });
            };
            $scope.skip = function () {
                $http.post('/mash/skip').then(function () {
                    updateState();
                });
            };
            $scope.reset = function () {
                $http.post('/mash/reset').then(function () {
                    updateState();
                });
            };

            updateState();
        });
})();