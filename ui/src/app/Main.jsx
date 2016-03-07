import React from 'react';
import { Router, Route, IndexRoute, Link, hashHistory, browserHistory } from 'react-router';
import RaisedButton from 'material-ui/lib/raised-button';
import FlatButton from 'material-ui/lib/flat-button';
import FloatingActionButton from 'material-ui/lib/floating-action-button';
import Toggle from 'material-ui/lib/toggle';
import {Tabs, Tab} from 'material-ui/lib/tabs';
import FontIcon from 'material-ui/lib/font-icon';
import Dialog from 'material-ui/lib/dialog';
import Paper from 'material-ui/lib/paper';
import RefreshIndicator from 'material-ui/lib/refresh-indicator';
import ContentAdd from 'material-ui/lib/svg-icons/content/add';
import ContentRemove from 'material-ui/lib/svg-icons/content/remove';
import MuiThemeProvider from 'material-ui/lib/MuiThemeProvider';
import getMuiTheme from 'material-ui/lib/styles/getMuiTheme';
import Colors from 'material-ui/lib/styles/colors';
import mobx from 'mobx';
import mobxReact from 'mobx-react';

const globalStyles = {
    container: {
        textAlign: 'center',
        paddingTop: 0,
        maxWidth: 768,
        margin: 'auto'
    },
    relayStyleOff: {display: 'inline-block', padding: '10px'},
    relayStyleOn: {display: 'inline-block', padding: '10px', 'backgroundColor': Colors.red200}
};

const muiTheme = getMuiTheme({
    palette: {
        accent1Color: Colors.red500
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
    const steps = 10;
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
    self.start = updateState;
    return self;
}();

updateTimer.start();

export default class Main extends React.Component {
    render() {
        return (
            <MuiThemeProvider muiTheme={muiTheme}>
                <div style={globalStyles.container}>
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
            },
            progressOuter: {
                marginLeft: 10,
                color: Colors.black
            },
            progressInner: {
                position: 'absolute',
                left: 0,
                top: 0,
                height: (100 - updateProgress.get() * 100).toString() + '%',
                overflow: 'hidden',
                color: Colors.grey200
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

                    <FontIcon style={style.progressOuter} className="fa fa-beer">
                        <FontIcon style={style.progressInner} className="fa fa-beer"/>
                    </FontIcon>

                    <div>
                        {Object.keys(serverState.relays).map(relay => {
                            return <Paper key={relay}
                                          style={serverState.relays[relay] ? globalStyles.relayStyleOn : globalStyles.relayStyleOff}>{relay}</Paper>
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
    changeFunc(amount) {
        return () => {
            let newTemp = this.state.targetTemp + amount;
            newTemp = Math.max(newTemp, -5);
            newTemp = Math.min(newTemp, 25);
            this.setState({targetTemp: newTemp});
        };
    },

    render() {
        const standardActions = (
            <FlatButton
                label="OK"
                secondary={true}
                onTouchTap={this.saveAndClose}
            />
        );
        const style = {
            content: {
                textAlign: 'center'
            },
            button: {
                marginLeft: 10,
                marginRight: 10
            }
        };

        return (
            <span>
                <FlatButton label="Ändern" secondary={true} onTouchTap={this.open}/>
                <Dialog open={this.state.open}
                        title={"Zieltemperatur auf " + this.state.targetTemp + "°C setzen"}
                        actions={standardActions}
                        onRequestClose={this.close}
                        contentStyle={style.content}
                >
                    <FloatingActionButton style={style.button} secondary={true} mini={true}
                                          onTouchTap={this.changeFunc(-1)}>
                        <ContentRemove />
                    </FloatingActionButton>
                    <FloatingActionButton style={style.button} secondary={true} mini={true}
                                          onTouchTap={this.changeFunc(+1)}>
                        <ContentAdd />
                    </FloatingActionButton>
                </Dialog>
            </span>
        );
    }
});

const RecipeScene = mobxReact.observer(React.createClass({
    createAction(action) {
        return (event, value) => {
            fetch("/api/recipe/" + action, {
                method: "post"
            }).then(updateTimer.forceUpdate);
        };
    },

    render() {
        if (store.serverState.notReady) return null;
        const serverState = store.serverState;

        const recipeButtonStyle = {margin: '10px'};

        const kesselOn = serverState.relays.Kessel === true;
        const kesselLabel = serverState.temperatures.Kessel ? serverState.temperatures.Kessel.toFixed(2) + '°C ' : '?? ';

        const tasks = [];
        for (let i = 0; i < serverState.recipeProcess.tasks.length; i++) {
            const task = serverState.recipeProcess.tasks[i];
            console.log("task " + i + ": " + JSON.stringify(task));
            tasks.push(<RecipeTask key={i} active={i === serverState.recipeProcess.activeTaskIndex} index={i}
                                   task={task}/>);
        }

        return (
            <div>
                <Paper className="panel">
                    <Paper style={kesselOn ? globalStyles.relayStyleOn: globalStyles.relayStyleOff}>{kesselLabel}
                        Kessel</Paper>
                </Paper>

                <Paper className="panel">
                    {tasks}
                    <RaisedButton style={recipeButtonStyle} label="Starten" primary={true}
                                  onTouchTap={this.createAction("start")}/>
                    <RaisedButton style={recipeButtonStyle} label="Schritt überspringen"
                                  onTouchTap={this.createAction("skipTask")}/>
                    <RaisedButton style={recipeButtonStyle} label="Zurücksetzen"
                                  onTouchTap={this.createAction("reset")}/>
                    <Link to="/recipe/edit">
                        <RaisedButton style={recipeButtonStyle} label="Rezept bearbeiten"/>
                    </Link>
                </Paper>
            </div>
        );
    }
}));

const RecipeTask = React.createClass({

    render() {
        const style = {
            inactiveTask: {padding: 10},
            activeTask: {
                padding: 10,
                backgroundColor: Colors.red200
            }
        };
        return <Paper style={this.props.active ? style.activeTask : style.inactiveTask}>
            {this.props.index + 1}. {JSON.stringify(this.props.task)}
        </Paper>;
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
