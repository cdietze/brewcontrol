import React from 'react';
import RaisedButton from 'material-ui/lib/raised-button';
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
                        <TargetTemperatureSelector />
                    </Paper>

                    <Paper className="panel">
                        <Paper style={relayStyle}>Kühlung</Paper>
                        <Paper style={relayStyleOn}>Heizung</Paper>
                        <Paper style={relayStyle}>Kessel</Paper>
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
                        <RaisedButton style={recipeButtonStyle} label="Schritt Überspringen" />
                        <RaisedButton style={recipeButtonStyle} label="Zurücksetzen" />
                        <RaisedButton style={recipeButtonStyle} label="Rezept Bearbeiten" />
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
                <RaisedButton label="Ändern" style={buttonStyle} secondary={true} onTouchTap={this.handleTouchTap} />
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