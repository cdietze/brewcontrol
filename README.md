# BrewControl

[![Build Status](https://travis-ci.org/cdietze/brewcontrol.svg?branch=master)](https://travis-ci.org/cdietze/brewcontrol)

My hobby project to automate some tasks when brewing beer. Currently, this project solves two tasks:

- Regulate a fridge to a certain temperature. The fridge can both cool and heat. I use this for fermenting or just for cooling.
- Run a mash program that controls the heater of the mashing pot.

*Beware that this is just the implementation I hacked up to run in my setup.* Also the UI is in German.

## Build and Run offline (no Raspberry Pi needed)

- Run `make run-server` to start the server (Maven, Dropwizard, Kotlin, SQLite)
- In another shell run `(cd ui && npm install)`
- Run `make run-ui` to fire up the dev web-server for the UI (npm, react.js, material-ui)
- Go to `http://localhost:3000`

## Prerequisites for real use

- Raspberry Pi with
    - 1-wire temperature sensors (DS1820)
    - relays attached (I use ones from SainSmart)
- A fridge that can cool and heat

## Screenshots

![Overview page](https://github.com/cdietze/brewcontrol/raw/master/images/overview.png "Overview page")
![Mash page](https://github.com/cdietze/brewcontrol/raw/master/images/mash.png "Mash page")
