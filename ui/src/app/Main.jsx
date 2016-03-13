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
import SelectField from 'material-ui/lib/select-field';
import TextField from 'material-ui/lib/text-field';
import MenuItem from 'material-ui/lib/menus/menu-item';
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
                            <IndexRoute component={MainScene}/>
                            <Route path="recipe" component={RecipeScene}/>
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
        const style = {
            tabs: {
                marginBottom: 10
            }
        };
        // I couldn't find a simpler way to detect the current tab index... I blame react-router
        const tabIndex = this.props.location.pathname.startsWith("/recipe") ? 1 : 0;
        return (
            <div>
                <Tabs style={style.tabs} initialSelectedIndex={tabIndex}>
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

const ProgressBeer = mobxReact.observer(React.createClass({
    render() {
        const style = {
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
        return <FontIcon style={style.progressOuter} className="fa fa-beer">
            <FontIcon style={style.progressInner} className="fa fa-beer"/>
        </FontIcon>;
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

                    <ProgressBeer />

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

        const style = {
            taskPanel: {
                marginTop: 10
            }
        };
        const recipeButtonStyle = {margin: '10px'};

        const kesselOn = serverState.relays.Kessel === true;
        const kesselLabel = serverState.temperatures.Kessel ? serverState.temperatures.Kessel.toFixed(2) + '°C ' : '?? ';

        const recipeProcess = serverState.recipeProcess;
        const tasks = [];
        for (let i = 0; i < recipeProcess.tasks.length; i++) {
            const task = recipeProcess.tasks[i];
            tasks.push(<RecipeTask key={i} active={i === recipeProcess.activeTaskIndex} index={i}
                                   task={task}/>);
        }

        const isRunning = recipeProcess.activeTaskIndex >= 0;
        const isDone = recipeProcess.activeTaskIndex >= recipeProcess.tasks.length;

        return (
            <div>
                <Paper style={kesselOn ? globalStyles.relayStyleOn: globalStyles.relayStyleOff}>{kesselLabel}
                    Kessel</Paper>

                <ProgressBeer />
                
                <div style={style.taskPanel}>
                    {tasks}
                    <RaisedButton style={recipeButtonStyle} label="Starten" secondary={true}
                                  onTouchTap={this.createAction("start")} disabled={isRunning}/>
                    <RaisedButton style={recipeButtonStyle} label="Schritt überspringen" secondary={true}
                                  onTouchTap={this.createAction("skipTask")} disabled={!isRunning || isDone}/>
                    <RaisedButton style={recipeButtonStyle} label="Zurücksetzen" secondary={true}
                                  onTouchTap={this.createAction("reset")} disabled={!isRunning}/>
                    <Link to="/recipe/edit">
                        <RaisedButton style={recipeButtonStyle} label="Rezept bearbeiten" secondary={true}/>
                    </Link>
                </div>
            </div>
        );
    }
}));

const RecipeTask = React.createClass({

    buildLabel(task) {
        let label = "" + (this.props.index + 1) + ". ";
        if (task.step.type === "Heat") {
            label += "Aufheizen auf " + task.step.temperature + "°C";
        } else if (task.step.type === "Rest") {
            label += "Rasten für " + (task.step.duration / 60).toFixed(0) + " Minuten";
        } else if (task.step.type === "Hold") {
            label += "Halten auf " + task.temperature + "°C bis Überspringen";
        } else {
            throw new Error("unknown RecipeTask type: " + task.step.type);
        }
        return label;
    },

    render() {
        const style = {
            inactiveTask: {padding: 10},
            activeTask: {
                padding: 10,
                backgroundColor: Colors.red200
            }
        };
        let content = [];

        const task = this.props.task;
        content.push(<div key="summary">{this.buildLabel(task)}</div>);
        if (task.startTime) {
            const startTime = new Date(task.startTime * 1000);
            content.push(<div>Start: {startTime.toLocaleTimeString()}</div>);
            // Render more details if active
            if (this.props.active && task.step.duration) {
                const endTime = new Date((task.startTime + task.step.duration) * 1000);
                content.push(<div>Ende: {endTime.toLocaleTimeString()}</div>);

                const remainingMinutes = (endTime.getTime() - new Date().getTime()) / 1000 / 60;
                if (remainingMinutes > 0) content.push(<div>Verbleibend: {remainingMinutes.toFixed(1)} Minuten</div>);
            }
        }
        return <Paper style={this.props.active ? style.activeTask : style.inactiveTask}>
            {content}
        </Paper>;
    }
});

const EditRecipeScene = React.createClass({
    getInitialState() {
        return {};
    },
    componentWillMount() {
        if (store.serverState.notReady) return;
        this.state.recipe = store.serverState.recipe;
    },
    handleStepChange(stepIndex, prop, value) {
        // This is simple and not efficient
        // But just updating that one field is insanely difficult
        this.state.recipe.steps[stepIndex][prop] = value;
        this.forceUpdate();
    },
    removeStep(stepIndex) {
        this.state.recipe.steps.splice(stepIndex, 1);
        this.forceUpdate();
    },
    addStep() {
        this.state.recipe.steps.push({});
        this.forceUpdate();
    },
    save() {
        fetch("/api/recipe", {
            method: "put",
            body: JSON.stringify(this.state.recipe),
            headers: new Headers({
                'Content-Type': "application/json"
            })
        }).then(updateTimer.forceUpdate).then(function () {
            hashHistory.push('/recipe');
        });
    },
    render() {
        if (store.serverState.notReady) return null;
        // console.log("rendering recipe: " + JSON.stringify(this.state.recipe));
        const recipe = this.state.recipe;
        const self = this;
        return (
            <div>
                <h3>Rezept bearbeiten</h3>
                {recipe.steps.map(function (step, index) {
                    return <Paper>
                        <span style={{textAlign:"left", display: "inline-block", width: "80%"}}>
                            <span>{index + 1}. </span>
                            <EditRecipeStep key={index} step={step}
                                            onStepChange={self.handleStepChange.bind(null, index)}/>
                        </span>
                        <span style={{width: "20%"}}>
                            <FlatButton label="Entfernen" secondary={true}
                                        onTouchTap={self.removeStep.bind(null, index)}/>
                        </span>

                    </Paper>;
                })}
                <span>
                    <FlatButton label="Schritt hinzufügen" secondary={true}
                                onTouchTap={this.addStep}/>
                </span>

                <div style={{textAlign: "right"}}>
                    <FlatButton label="Speichern" secondary={true} onTouchTap={this.save}/>
                </div>
            </div>);
    }
});

const EditRecipeStep = React.createClass({
    propTypes: {
        step: React.PropTypes.object.isRequired,
        onStepChange: React.PropTypes.func.isRequired
    },
    getInitialState() {
        const state = {
            temperature: this.props.step.temperature
        };
        if (this.props.step.duration !== undefined) state.duration = this.props.step.duration / 60;
        return state;
    },
    handleChange(prop, event, itemIndex, value) {
        this.props.onStepChange(prop, value);
    },
    handleTemperatureChange(event) {
        const value = parseInt(event.target.value, 10);
        this.setState({temperature: value});
        this.props.onStepChange("temperature", value);
    },
    handleDurationChange(event) {
        const value = parseInt(event.target.value, 10);
        this.setState({duration: value});
        this.props.onStepChange("duration", value * 60);
    },
    render() {
        const style = {
            type: {width: "8em", textAlign: "right"},
            temperature: {width: "3em"},
            temperatureInput: {textAlign: "right"},
            duration: {width: "3em"},
            durationInput: {textAlign: "right"}
        };
        const step = this.props.step;
        const content = [];
        content.push(<SelectField style={style.type} value={step.type}
                                  onChange={this.handleChange.bind(null, "type")}>
            <MenuItem value={"Heat"} primaryText="Aufheizen"/>
            <MenuItem value={"Rest"} primaryText="Rasten"/>
            <MenuItem value={"Hold"} primaryText="Halten"/>
        </SelectField>);
        if (step.type === "Heat") {
            content.push(<span> auf <TextField style={style.temperature}
                                               inputStyle={style.temperatureInput}
                                               value={this.state.temperature}
                                               type="number"
                                               onChange={this.handleTemperatureChange}/>°C</span>);
        } else if (step.type === "Rest") {
            content.push(<span> für <TextField
                style={style.duration}
                inputStyle={style.durationInput}
                value={this.state.duration}
                type="number"
                onChange={this.handleDurationChange}/> Minuten</span>);
        }
        return <span>{content}</span>;
    }
});
