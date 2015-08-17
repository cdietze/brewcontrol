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
                height: 200,
                renderer: 'line',
                series: series
            });

            graph.render();
            var axes = new Rickshaw.Graph.Axis.Time({graph: graph});
            axes.render();
            var legend = new Rickshaw.Graph.Legend( {
                element: document.querySelector('#legend'),
                graph: graph
            } );
        });


        //var data = [ { x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }, { x: -1262304000, y: 123202660 }, { x: -946771200, y: 132165129 }, { x: -631152000, y: 151325798 }, { x: -315619200, y: 179323175 }, { x: 0, y: 203211926 }, { x: 315532800, y: 226545805 }, { x: 631152000, y: 248709873 }, { x: 946684800, y: 281421906 }, { x: 1262304000, y: 308745538 } ];
        //
        //var graph = new Rickshaw.Graph( {
        //    element: document.querySelector("#chart"),
        //    width: 580,
        //    height: 250,
        //    series: [ {
        //        color: 'steelblue',
        //        data: data
        //    } ]
        //} );
        //
        //var axes = new Rickshaw.Graph.Axis.Time( { graph: graph } );
        //
        //graph.render();
    });

