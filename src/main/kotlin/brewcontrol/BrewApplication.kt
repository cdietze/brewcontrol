package brewcontrol

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application

import io.dropwizard.Configuration
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory
import javax.validation.Valid
import javax.validation.constraints.NotNull

val log = LoggerFactory.getLogger("brewcontrol")

fun main(args: Array<String>) {
    println("Starting BrewControl")
    BrewApplication().run(*args)
}

class BrewConfiguration : Configuration() {
    @JsonProperty
    var gpioEnabled: Boolean = true

    @Valid
    @NotNull
    var database = DataSourceFactory()
}

class BrewApplication : Application<BrewConfiguration>() {
    override fun run(configuration: BrewConfiguration, environment: Environment) {
        log.info("Running BrewApplication")
        val temperatureSystem = TemperatureSystem()
        val relaySystem = RelaySystem()
        if (configuration.gpioEnabled) relaySystem.wireToGpio(gpioImpl)
        temperatureSystem.startUpdateScheduler(RandomTemperatureReader())

        val factory = DBIFactory()
        val jdbi = factory.build(environment, configuration.database, "SQLite")
        val configDao = jdbi.onDemand(ConfigDao::class.java).apply {
            createTable()
        }
        val configSystem = ConfigSystem(configDao)

        val error = pidController(temperatureSystem.temperatureView(TemperatureSystem.Sensor.Cooler), configSystem.targetTemperature)
        error.connectNotify { e ->
            log.debug("Temperature error is $e")
            relaySystem.cooler.value.update(e > 0)
        }

        environment.jersey().register(WebResource(temperatureSystem, relaySystem, configSystem))
    }
}
