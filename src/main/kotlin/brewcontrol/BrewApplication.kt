package brewcontrol

import io.dropwizard.Application

import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory
import react.Value

val log = LoggerFactory.getLogger("brewcontrol")

fun main(args: Array<String>) {
    println("Starting BrewControl")
    BrewApplication().run(*args)
}

class BrewConfiguration : Configuration()

class BrewApplication : Application<BrewConfiguration>() {
    override fun run(configuration: BrewConfiguration?, environment: Environment?) {
        log.info("Running BrewApplication")
        val t = TemperatureSystem()
        val r = RelaySystem()
        t.startUpdateScheduler(RandomTemperatureReader())

        val targetTemperature = Value(30.0)
        val error = pidController(t.temperatureView(TemperatureSystem.Sensor.Cooler), targetTemperature)
        error.connectNotify { e -> log.debug("Temperature error is $e") }
        //error.map { it > .5 }.connectNotify(r.relayView(RelaySystem.Relay.Cooler)

        checkNotNull(environment).jersey().register(WebResource(t))
    }
}
