angular
    .module('brewControl', ['ngMaterial'])
    .config(function ($mdThemingProvider) {

        $mdThemingProvider.theme('default')
            .primaryPalette('brown')
            .accentPalette('red');

    });
angular
    .module('brewControl')
    .controller('FridgeCtrl', function ($scope, $http) {
        $scope.data = {}

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

        $http.get("/temperatures").then(function (response) {
            $scope.temperatures = response.data;
        });

        $http.get("/relays").then(function (response) {
            $scope.relays = response.data;
        });
    });

angular
    .module('brewControl')
    .controller('GraphCtrl', function ($scope, $http) {

        $http.get("/history").then(function (response) {
            var series = [];
            var palette = new Rickshaw.Color.Palette();
            _(response.data).each(function (value, key) {
                var s = {
                    color: palette.color(),
                    name: value.name
                };
                s.data = _(value.data).map(function (e) {
                    // The timestamps in x are longs which are represented as strings -> convert them to double
                    var p = {x: Number(e.x) / 1000, y: e.y};
                    return p;
                });
                series.push(s);
            });

            var graph = new Rickshaw.Graph({
                element: document.querySelector("#chart"),
                height: 300,
                renderer: 'line',
                series: series
            });


            var xAxis = new Rickshaw.Graph.Axis.Time({
                graph: graph,
                timeFixture: new Rickshaw.Fixtures.Time.Local()
            });

            var yAxis = new Rickshaw.Graph.Axis.Y({
                graph: graph,
                orientation: 'left',
                tickFormat: Rickshaw.Fixtures.Number.formatKMBT(),
                // ticks: 2,
                element: document.getElementById('yAxis')
            });


            graph.render();

            var legend = new Rickshaw.Graph.Legend({
                element: document.querySelector('#legend'),
                graph: graph
            });
        });

    });

