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

export default
class Main extends React.Component {
    render() {
        return (
            <MuiThemeProvider muiTheme={muiTheme}>
                <div style={styles.container}>
                    <Router history={hashHistory}>
                        <Route path="/" component={TabComponent}>
                            <IndexRoute component={MainScene} tab="main" />
                            <Route path="recipe" component={RecipeScene} tab="recipe" />
                            <Route path="/recipe/edit" component={EditRecipeScene} />
                        </Route>

                    </Router>
                </div>
            </MuiThemeProvider>
        );
    }
}

class MainScene extends React.Component {
    render() {
        const relayStyle = {display: 'inline-block', padding: '10px'};
        const relayStyleOn = Object.assign({}, relayStyle, {'backgroundColor': '#ffaaaa'});

        return (
            <div>
                <Paper className="panel">
                    <div>12.50°C Kühlschrank</div>
                    <div>2.25°C Außen</div>
                    <Paper style={relayStyle}>Kühlung</Paper>
                    <Paper style={relayStyleOn}>Heizung</Paper>
                </Paper>

                <Paper className="panel">
                    <TargetTemperatureSelector />
                    <div style={{maxWidth: 250}}>
                        <Toggle label="Heizung freigegeben" />
                        <Toggle label="Kühlung freigegeben" />
                    </div>
                </Paper>
            </div>
        );
    }
}

class RecipeScene extends React.Component {
    render() {
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
                    <RaisedButton style={recipeButtonStyle} label="Starten" primary={true} />
                    <RaisedButton style={recipeButtonStyle} label="Schritt überspringen" />
                    <RaisedButton style={recipeButtonStyle} label="Zurücksetzen" />
                    <Link to="/recipe/edit">
                        <RaisedButton style={recipeButtonStyle} label="Rezept bearbeiten" />
                    </Link>
                </Paper>
            </div>
        );
    }
}


class TabComponent extends React.Component {

    goMain() {
        hashHistory.push('/');
    }

    goRecipe() {
        hashHistory.push('/recipe');
    }

    render() {
        return (
            <div>
                <Tabs value={this.props.tab}>
                    <Tab value="main" label="Kühlschrank" onActive={this.goMain}>
                    </Tab>
                    <Tab value="recipe" label="Maischen" onActive={this.goRecipe}>
                    </Tab>
                </Tabs>
        {this.props.children}
            </div>
        );
    }
}

class TargetTemperatureSelector extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.handleRequestClose = this.handleRequestClose.bind(this);
        this.handleTouchTap = this.handleTouchTap.bind(this);
        this.state = {
            open: false
        };
    }

    componentDidMount() {
        console.log("componentDidMount: TargetTemperatureSelector")
    }

    handleRequestClose() {
        this.setState({
            open: false
        });
    }

    handleTouchTap() {
        this.setState({
            open: true
        });
    }

    render() {
        const standardActions = (
            <FlatButton
                label="OK"
                secondary={true}
                onTouchTap={this.handleRequestClose}
            />
        );
        const buttonStyle = {
            marginLeft: 10
        };
        return (
            <div>
                <span>Zieltemperatur: 13°C</span>
                <RaisedButton label="Ändern" style={buttonStyle} onTouchTap={this.handleTouchTap} />
                <Dialog open={this.state.open}
                    title="Zieltemperatur setzen"
                    actions={standardActions}
                    onRequestClose={this.handleRequestClose}
                >
                    <Slider step={1} min={-5} max={25} defaultValue={10} />
                </Dialog>
            </div>
        );
    }
}

const EditRecipeScene = React.createClass({
    render() {
        return (
            <Paper className="panel">
                <h3>Rezept bearbeiten</h3>
                <p>TODO: allow to add / remove / edit / move recipe steps</p>
                <div style={{textAlign: "right"}}>
                    <FlatButton label="Speichern" secondary={true} />
                </div>
            </Paper>
        );
    }
});
