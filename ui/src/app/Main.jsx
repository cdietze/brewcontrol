import React from 'react';
import { Router, Route, IndexRoute, Link, hashHistory, browserHistory } from 'react-router';
import RaisedButton from 'material-ui/lib/raised-button';
import AppBar from 'material-ui/lib/app-bar';
import Toggle from 'material-ui/lib/toggle';
import {Tabs, Tab} from 'material-ui/lib/tabs';
import IconButton from 'material-ui/lib/icon-button';
import NavigationArrowBack from 'material-ui/lib/svg-icons/navigation/arrow-back';
import Dialog from 'material-ui/lib/dialog';
import Paper from 'material-ui/lib/paper';
import Slider from 'material-ui/lib/slider';
import {deepOrange500} from 'material-ui/lib/styles/colors';
import FlatButton from 'material-ui/lib/flat-button';
import getMuiTheme from 'material-ui/lib/styles/getMuiTheme';
import MuiThemeProvider from 'material-ui/lib/MuiThemeProvider';
import RefreshIndicator from 'material-ui/lib/refresh-indicator';
import CircularProgress from 'material-ui/lib/circular-progress';
import mobx from 'mobx';
import mobxReact from 'mobx-react';

const styles = {
    container: {
        textAlign: 'center',
        paddingTop: 0,
        maxWidth: 768,
        margin: 'auto'
    }
};

const muiTheme = getMuiTheme({
    palette: {
        accent1Color: deepOrange500
    }
});

/* Example for serverState:
 {
 "temperatures": {"Kühlschrank": 13.0, "Außen": 26.0},
 "relays": {"Kühlung": false, "Heizung": false, "Kessel": false},
 "config": {"coolerEnabled": false, "heaterEnabled": false, "targetTemperature": 10.0}
 }
 */
const store = mobx.observable({serverState: {notReady: true}});

const updateProgress = mobx.observable(0);
let updateCount = 0;

const updateTimer = function () {
    let self = {};
    let timeoutHandle = undefined;
    const updateInterval = 3000;
    const steps = 100;
    const waitTime = updateInterval / steps;
    let currentStep = 0;

    const progress = () => {
        updateProgress.set(currentStep / steps);
        currentStep++;
        if (currentStep > steps) {
            currentStep = 0;
            updateCount++;

            updateState().then(progress);
        } else {
            timeoutHandle = setTimeout(progress, waitTime);
        }
    };

    function updateState() {
        if (timeoutHandle) {
            clearTimeout(timeoutHandle);
            timeoutHandle = undefined;
        }
        return fetch("/api/state").then(response => {
            response.json().then(json => {
                store.serverState = json;
            }).then(() => {
                currentStep = 0;
                updateCount++;
                progress();
            });
        });
    }

    self.forceUpdate = updateState;
    self.start = progress;
    return self;
}();

updateTimer.start();

export default class Main extends React.Component {
    render() {
        return (
            <MuiThemeProvider muiTheme={muiTheme}>
                <div style={styles.container}>
                    <LoadingComponent />
                    <Router history={hashHistory}>
                        <Route path="/" component={TabComponent}>
                            <IndexRoute component={MainScene} tab="main"/>
                            <Route path="recipe" component={RecipeScene} tab="recipe"/>
                            <Route path="/recipe/edit" component={EditRecipeScene}/>
                        </Route>

                    </Router>
                </div>
            </MuiThemeProvider>
        );
    }
}

const LoadingComponent = mobxReact.observer(React.createClass({
    render() {
        const style = {
            container: {
                display: 'inline-block',
                position: 'relative'
            },
            refresh: {
                marginTop: 10,
                marginBottom: 10,
                display: 'block',
                position: 'relative'
            }
        };

        if (store.serverState.notReady) return <div>
            <div style={style.container}>
                <RefreshIndicator
                    size={40}
                    left={10}
                    top={0}
                    status="loading"
                    style={style.refresh}
                />
                <div>Loading...</div>
            </div>
        </div>;
        return null;
    }
}));

const TabComponent = mobxReact.observer(React.createClass({
    goMain() {
        hashHistory.push('/');
    },
    goRecipe() {
        hashHistory.push('/recipe');
    },
    render() {
        if (store.serverState.notReady) return null;
        return (
            <div>
                <Tabs value={this.props.tab}>
                    <Tab value="main" label="Übersicht" onActive={this.goMain}>
                    </Tab>
                    <Tab value="recipe" label="Maischen" onActive={this.goRecipe}>
                    </Tab>
                </Tabs>
                {this.props.children}
            </div>
        );
    }
}));

const MainScene = mobxReact.observer(React.createClass({
    createConfigToggler(key) {
        return (event, value) => {
            fetch("/api/config/" + key, {
                method: "put",
                body: value.toString()
            }).then(updateTimer.forceUpdate);
        };
    },
    render() {
        if (store.serverState.notReady) return null;
        const serverState = store.serverState;
        const relayStyleOff = {display: 'inline-block', padding: '10px'};
        const relayStyleOn = Object.assign({}, relayStyleOff, {'backgroundColor': '#ffaaaa'});
        const style = {
            tempContainer: {
                marginBottom: 10,
                display: 'inline-block',
                textAlign: 'left'
            },
            tempNumber: {
                minWidth: 65,
                display: 'inline-block',
                textAlign: 'right'
            },
            toggleContainer: {
                maxWidth: 250,
                display: 'inline-block'
            },
            toggle: {
                marginTop: 20
            }
        };
        return (
            <div>
                <Paper className="panel">
                    <div style={style.tempContainer}>
                        {Object.keys(serverState.temperatures).map(sensor => {
                            return <div key={sensor}>
                                <span style={style.tempNumber}>{serverState.temperatures[sensor].toFixed(2)}°C</span>
                                <span> {sensor}</span>
                            </div>;
                        })}
                    </div>
                    <CircularProgress key={updateCount} mode="determinate" value={updateProgress.get()} min={0} max={1}
                                      size={0.3}/>

                    <div>
                        {Object.keys(serverState.relays).map(relay => {
                            return <Paper key={relay}
                                          style={serverState.relays[relay] ? relayStyleOn : relayStyleOff}>{relay}</Paper>
                        })}
                    </div>
                </Paper>

                <Paper className="panel">
                    <TargetTemperatureComponent targetTemp={serverState.config.targetTemperature}/>
                    <div style={style.toggleContainer}>
                        <Toggle label="Heizung freigegeben" style={style.toggle}
                                disabled={serverState.config.heaterEnabled === undefined}
                                defaultToggled={serverState.config.heaterEnabled}
                                onToggle={this.createConfigToggler("heaterEnabled")}/>
                        <Toggle label="Kühlung freigegeben" style={style.toggle}
                                disabled={serverState.config.coolerEnabled === undefined}
                                defaultToggled={serverState.config.coolerEnabled}
                                onToggle={this.createConfigToggler("coolerEnabled")}/>
                    </div>
                </Paper>
            </div>
        );
    }
}));

const TargetTemperatureComponent = React.createClass({
    render() {
        return (
            <div>
                <span>Zieltemperatur: {this.props.targetTemp}°C</span>
                <SelectTargetTemperatureButton oldTargetTemp={this.props.targetTemp}/>
            </div>
        );
    }
});

const SelectTargetTemperatureButton = React.createClass({
    getInitialState() {
        return {
            open: false,
            targetTemp: undefined
        };
    },
    open() {
        this.setState({
            open: true,
            targetTemp: this.props.oldTargetTemp
        });
    },
    close() {
        this.setState({
            open: false
        });
    },
    saveAndClose() {
        fetch("/api/config/targetTemperature", {
            method: "put",
            body: this.state.targetTemp.toString()
        }).then(updateTimer.forceUpdate);
        this.setState({
            open: false
        });
    },
    render() {
        const standardActions = (
            <FlatButton
                label="OK"
                secondary={true}
                onTouchTap={this.saveAndClose}
            />
        );
        const buttonStyle = {
            marginLeft: 10
        };
        const onChange = (event, value) => {
            this.setState({
                targetTemp: value
            });
        };

        return (
            <span>
                <RaisedButton label="Ändern" style={buttonStyle} onTouchTap={this.open}/>
                <Dialog open={this.state.open}
                        title={"Zieltemperatur auf " + this.state.targetTemp + "°C setzen"}
                        actions={standardActions}
                        onRequestClose={this.close}
                >
                    <Slider step={1} min={-5} max={25} defaultValue={this.state.targetTemp} onChange={onChange}/>
                </Dialog>
            </span>
        );
    }
});

const RecipeScene = React.createClass({
    render() {
        if (store.serverState.notReady) return null;
        const relayStyle = {display: 'inline-block', padding: '10px'};
        const relayStyleOn = Object.assign({}, relayStyle, {'backgroundColor': '#ffaaaa'});

        const recipeStepStyle = {padding: '10px'};
        const recipeStepActiveStyle = Object.assign({}, recipeStepStyle, {'backgroundColor': '#ffaaaa'});

        const recipeButtonStyle = {margin: '10px'};

        return (
            <div>
                <Paper className="panel">
                    <Paper style={relayStyle}>45.25°C Kessel</Paper>
                </Paper>

                <Paper className="panel">
                    <Paper style={recipeStepStyle}>
                        1. Heizen auf 64°C
                    </Paper>
                    <Paper style={recipeStepActiveStyle}>
                        2. Halten für 15 Minuten
                    </Paper>
                    <Paper style={recipeStepStyle}>
                        3. Heizen auf 72°C
                    </Paper>
                    <RaisedButton style={recipeButtonStyle} label="Starten" primary={true}/>
                    <RaisedButton style={recipeButtonStyle} label="Schritt überspringen"/>
                    <RaisedButton style={recipeButtonStyle} label="Zurücksetzen"/>
                    <Link to="/recipe/edit">
                        <RaisedButton style={recipeButtonStyle} label="Rezept bearbeiten"/>
                    </Link>
                </Paper>
            </div>
        );
    }
});

const EditRecipeScene = React.createClass({
    render() {
        return (
            <Paper className="panel">
                <h3>Rezept bearbeiten</h3>
                <p>TODO: allow to add / remove / edit / move recipe steps</p>
                <div style={{textAlign: "right"}}>
                    <FlatButton label="Speichern" secondary={true}/>
                </div>
            </Paper>
        );
    }
});
