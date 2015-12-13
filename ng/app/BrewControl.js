(function () {

    var pollFactory = function ($scope, $interval, pollFunc, progressFunc) {
        var totalTime = 3000;
        var totalSteps = 30;

        var currentPromise = undefined;
        var step = 0;
        var progress = function () {
            step += 1;
            progressFunc(Math.round((100 * step) / totalSteps));
            if (step === totalSteps) {
                step = 0;
                update();
            }
        };

        var update = function () {
            if (currentPromise) {
                $interval.cancel(currentPromise);
            }
            pollFunc().then(function () {
                currentPromise = $interval(progress, totalTime / totalSteps, totalSteps);
            });
        };
        $scope.$on('$destroy', function () {
            if (currentPromise) {
                $interval.cancel(currentPromise);
            }
        });
        return update;
    };

    angular
        .module('brewControl', ['ngMaterial'])
        .config(function ($mdThemingProvider) {

            $mdThemingProvider.theme('default')
                .primaryPalette('indigo')
                .accentPalette('red');

        });

    angular
        .module('brewControl')
        .controller('FridgeCtrl', function ($scope, $http, $interval) {
            $scope.data = {
                showEditTargetTemperature: false
            };

            pollFactory($scope, $interval, function () {
                return $http.get("/api/state").then(function (response) {
                    $scope.state = response.data;
                    if (!$scope.config) {
                        // Should make sure that config values have not changed server side ...
                        $scope.config = angular.copy($scope.state.config);
                        watchConfig();
                    }
                });
            }, function (progress) {
                console.log("progress is: " + progress)
                $scope.data.pollProgress = progress;
            })();

            /* Watch all config values and post if anything is changed */
            var watchConfig = function () {
                _.each(['coolerEnabled', 'heaterEnabled', 'targetTemperature'], function (key) {
                    var url = "/api/config/" + key;
                    $scope.$watch('config.' + key, function (newValue, oldValue) {
                        if (newValue === oldValue) return;
                        $http.put(url, newValue);
                    });
                });
            };
        });

    angular
        .module('brewControl')
        .controller('MashCtrl', function ($scope, $http, $timeout, $filter) {

            var updateState = pollFactory($scope, $timeout, function () {
                return $http.get('/mash/state').then(function (response) {
                    $scope.state = response.data;
                });
            });

            $scope.tryPrintTaskEndTime = function (task) {
                if (task.startTime && task.startTime[0] && task.durationInMillis) {
                    return " (bis " + $filter('date')(task.startTime[0] + task.durationInMillis, 'mediumTime') + ")";
                }
                return "";
            };

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