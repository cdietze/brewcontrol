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
import react.Values
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotNull

val log = LoggerFactory.getLogger("brewcontrol")

fun main(args: Array<String>) {
    println("[${Date()}] Starting BrewControl")
    BrewApplication().run(*args)
}

class BrewConfiguration : Configuration() {
    @JsonProperty
    var gpioEnabled: Boolean = true
    @JsonProperty
    var mockTemperatures: Boolean = false

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

        val temperatureReader = if (configuration.mockTemperatures) RandomTemperatureReader() else RealTemperatureReader()

        temperatureSystem.startUpdateScheduler(temperatureReader)

        val factory = DBIFactory()
        val jdbi = factory.build(environment, configuration.database, "SQLite")
        val configDao = jdbi.onDemand(ConfigDao::class.java).apply {
            createTable()
        }
        val configSystem = ConfigSystem(configDao)

        val temperatureTolerance = 0.5
        val error = pidController(temperatureSystem.temperatureView(TemperatureSystem.Sensor.Cooler), configSystem.targetTemperature)
        Values.and(configSystem.coolerEnabled, error.map { it < -temperatureTolerance }).connectNotify(relaySystem.cooler.value.slot())
        Values.and(configSystem.heaterEnabled, error.map { it > temperatureTolerance }).connectNotify(relaySystem.heater.value.slot())

        environment.jersey().register(WebResource(temperatureSystem, relaySystem, configSystem))
    }
}
