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

const store = mobx.observable({serverState: {notReady: true}});

setInterval(() => {
    /* Example for state response:
     {
     "temperatures": {"Kühlschrank": 13.0, "Außen": 26.0},
     "relays": {"Kühlung": false, "Heizung": false, "Kessel": false},
     "config": {"coolerEnabled": false, "heaterEnabled": false, "targetTemperature": 10.0}
     }
     */
    fetch("/api/state").then(response => {
        response.json().then(json => {
            store.serverState = json;
        });
    });
}, 3000);

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
        if (store.serverState.notReady) return <div>Loading...</div>;
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
            });
        };
    },
    render() {
        if (store.serverState.notReady) return null;
        const serverState = store.serverState;
        const relayStyleOff = {display: 'inline-block', padding: '10px'};
        const relayStyleOn = Object.assign({}, relayStyleOff, {'backgroundColor': '#ffaaaa'});
        return (
            <div>
                <Paper className="panel">
                    {Object.keys(serverState.temperatures).map(sensor => {
                        return <div key={sensor}>{serverState.temperatures[sensor].toFixed(2)}°C {sensor}</div>;
                    })}

                    {Object.keys(serverState.relays).map(relay => {
                        return <Paper key={relay}
                                      style={serverState.relays[relay] ? relayStyleOn : relayStyleOff}>{relay}</Paper>
                    })}
                </Paper>

                <Paper className="panel">
                    <TargetTemperatureComponent targetTemp={serverState.config.targetTemperature}/>
                    <div style={{maxWidth: 250}}>
                        <Toggle label="Heizung freigegeben"
                                disabled={serverState.config.heaterEnabled === undefined}
                                defaultToggled={serverState.config.heaterEnabled}
                                onToggle={this.createConfigToggler("heaterEnabled")}/>
                        <Toggle label="Kühlung freigegeben"
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
        console.log("TODO: PUT targetTemp: " + this.state.targetTemp);
        fetch("/api/config/targetTemperature", {
            method: "put",
            body: this.state.targetTemp.toString()
        });
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
            <div>
                <RaisedButton label="Ändern" style={buttonStyle} onTouchTap={this.open}/>
                <Dialog open={this.state.open}
                        title={"Zieltemperatur auf " + this.state.targetTemp + "°C setzen"}
                        actions={standardActions}
                        onRequestClose={this.close}
                >
                    <Slider step={1} min={-5} max={25} defaultValue={this.state.targetTemp} onChange={onChange}/>
                </Dialog>
            </div>
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
