package brewcontrol

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application

import io.dropwizard.Configuration
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.setup.Bootstrap
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
    override fun initialize(bootstrap: Bootstrap<BrewConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets/", "/", "index.html"))
    }

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

        val temperatureTolerance = 0.5
        val error = pidController(temperatureSystem.temperatureView(TemperatureSystem.Sensor.Cooler), configSystem.targetTemperature)
        error.connectNotify { e ->
            log.debug("Temperature error is $e")
            relaySystem.cooler.value.update(configSystem.coolerEnabled.get() && e < -temperatureTolerance)
            relaySystem.heater.value.update(configSystem.heaterEnabled.get() && e > temperatureTolerance)
        }

        environment.jersey().register(WebResource(temperatureSystem, relaySystem, configSystem))
    }
}
