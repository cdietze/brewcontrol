/**
 * In this file, we create a React component
 * which incorporates components providedby material-ui.
 */

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
        const standardActions = (
            <FlatButton
                label="Okey"
                secondary={true}
                onTouchTap={this.handleRequestClose}
            />
        );

        const relayStyle = {display: 'inline-block', margin: '10px', padding: '10px'};
        const relayStyleOn = Object.assign({}, relayStyle, {'backgroundColor': 'red'});

        return (
            <MuiThemeProvider muiTheme={muiTheme}>
                <div style={styles.container}>

                    <Paper>
                        <p>12.50°C Kühlschrank</p>
                        <p>2.25°C Außen</p>
                    </Paper>

                    <Paper>
                        <Paper style={relayStyle}>Kühlung</Paper>
                        <Paper style={relayStyleOn}>Heizung</Paper>
                        <Paper style={relayStyle}>Kessel</Paper>
                    </Paper>

                    <Paper>
                        <TargetTemperatureSelector />
                    </Paper>

                    <Dialog
                        open={this.state.open}
                        title="Super Secret Password"
                        actions={standardActions}
                        onRequestClose={this.handleRequestClose}
                    >
                        1-2-3-4-5
                    </Dialog>
                    <h1>material-ui</h1>
                    <h2>example project</h2>
                    <RaisedButton
                        label="Super Secret Password"
                        primary={true}
                        onTouchTap={this.handleTouchTap}
                    />
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
                label="Okey"
                secondary={true}
                onTouchTap={this.handleRequestClose}
            />
        );
        return (
            <div>
                <Paper>
                    <span>Zieltemperatur: 13°C</span>
                    <RaisedButton label="Ändern" primary={true} onTouchTap={this.handleTouchTap} />
                </Paper>
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