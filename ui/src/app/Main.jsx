import React from 'react';
import { Router, Route, IndexRoute, Link, hashHistory, browserHistory } from 'react-router';
import RaisedButton from 'material-ui/lib/raised-button';
import AppBar from 'material-ui/lib/app-bar';
import Toggle from 'material-ui/lib/toggle';
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
        paddingTop: 200
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
            <Router history={hashHistory}>
                <Route path="/" component={MainScene}/>
                <Route path="/editRecipe" component={EditRecipeScene}/>
            </Router>
        );
    }
}

class MainScene extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.handleRequestClose = this.handleRequestClose.bind(this);
        this.handleTouchTap = this.handleTouchTap.bind(this);

        this.state = {
            open: false
        };
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
        const relayStyle = {display: 'inline-block', padding: '10px'};
        const relayStyleOn = Object.assign({}, relayStyle, {'backgroundColor': '#ffaaaa'});

        const recipeStepStyle = {padding: '10px'};
        const recipeStepActiveStyle = Object.assign({}, recipeStepStyle, {'backgroundColor': '#ffaaaa'});

        const recipeButtonStyle = {margin: '10px'};

        return (
            <MuiThemeProvider muiTheme={muiTheme}>
                <div style={styles.container}>

                    <Paper className="panel">
                        <div>12.50°C Kühlschrank</div>
                        <div>2.25°C Außen</div>
                    </Paper>

                    <Paper className="panel">
                        <Paper style={relayStyle}>Kühlung</Paper>
                        <Paper style={relayStyleOn}>Heizung</Paper>
                        <Paper style={relayStyle}>Kessel</Paper>
                    </Paper>

                    <Paper className="panel">
                        <TargetTemperatureSelector />
                        <div style={{textAlign: 'center'}}>
                            <div style={{maxWidth: 250, display: 'inline-block'}}>
                                <Toggle label="Heizung freigegeben" />
                                <Toggle label="Kühlung freigegeben" />
                            </div>
                        </div>
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
                        <Link to="/editRecipe">
                            <RaisedButton style={recipeButtonStyle} label="Rezept bearbeiten" />
                        </Link>
                    </Paper>
                </div>
            </MuiThemeProvider>
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
            <div>
                <AppBar
                    title={<span>Rezept bearbeiten</span>}
                    iconElementLeft={<Link to="/">
                        <IconButton>
                            <NavigationArrowBack />
                        </IconButton>
                    </Link>}
                    iconElementRight={<FlatButton label="Speichern" />}
                />
                TODO: allow to add / remove / edit / move recipe steps
            </div>
        );
    }
});
