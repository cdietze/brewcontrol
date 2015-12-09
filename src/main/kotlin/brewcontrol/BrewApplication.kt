package brewcontrol

import com.fasterxml.jackson.annotation.JsonProperty
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

class BrewConfiguration : Configuration() {
    @JsonProperty
    var gpioEnabled: Boolean = true
}

class BrewApplication : Application<BrewConfiguration>() {
    override fun run(configuration: BrewConfiguration, environment: Environment) {
        log.info("Running BrewApplication")
        val temperatureSystem = TemperatureSystem()
        val relaySystem = RelaySystem()
        if (configuration.gpioEnabled) relaySystem.wireToGpio(gpioImpl)
        temperatureSystem.startUpdateScheduler(RandomTemperatureReader())

        val targetTemperature = Value(30.0)
        val error = pidController(temperatureSystem.temperatureView(TemperatureSystem.Sensor.Cooler), targetTemperature)
        error.connectNotify { e ->
            log.debug("Temperature error is $e")
            relaySystem.cooler.value.update(e > 0)
        }
        environment.jersey().register(WebResource(temperatureSystem, relaySystem))
    }
}
