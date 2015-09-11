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
            var binaryScale = d3.scale.linear().domain([0, 1]).range([1, 24]);
            var temperatureScale = d3.scale.linear().domain([0, 25]).range([0, 25]);

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
                if (value.kind === "binary") {
                    s.scale = binaryScale;
                    s.renderer = 'lineplot';
                } else {
                    s.scale = temperatureScale;
                    s.renderer = 'line';
                }
                series.push(s);
            });

            var graph = new Rickshaw.Graph({
                element: document.querySelector("#chart"),
                height: 300,
                renderer: 'multi',
                series: series,
                interpolation: 'step-after'
            });

            var xAxis = new Rickshaw.Graph.Axis.Time({
                graph: graph,
                timeFixture: new Rickshaw.Fixtures.Time.Local()
            });

            var yAxis = new Rickshaw.Graph.Axis.Y({
                graph: graph,
                orientation: 'left',
                tickFormat: Rickshaw.Fixtures.Number.formatKMBT(),
                element: document.getElementById('yAxis'),
                scale: temperatureScale
            });

            graph.render();

            var legend = new Rickshaw.Graph.Legend({
                element: document.querySelector('#legend'),
                graph: graph
            });
        });
    });

angular
    .module('brewControl')
    .controller('MashCtrl', function ($scope, $http) {
        $http.get('/mash/recipe').then(function(response) {
           $scope.recipe = response.data;
        });
        $http.get('/mash/state').then(function(response) {
            $scope.state = response.data;
        });
        $scope.start = function() {
            $http.post('/mash/start');
        }
    });