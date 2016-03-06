package brewcontrol

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.jackson.Jackson
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory
import react.Value
import react.Values
import java.time.Instant
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotNull

val log = LoggerFactory.getLogger("brewcontrol")

fun main(args: Array<String>) {
    println("[${Date()}] Starting BrewControl")
    BrewApplication().run(*args)
}

fun createObjectMapper(): ObjectMapper {
    val om = Jackson.newObjectMapper()
    om.registerModule(JavaTimeModule())
    om.registerModule(KotlinModule())
    return om
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
        bootstrap.objectMapper = createObjectMapper()
        bootstrap.addBundle(AssetsBundle("/assets/", "/", "index.html"))
    }

    override fun run(configuration: BrewConfiguration, environment: Environment) {
        log.info("Running BrewApplication")
        val updateThread = UpdateThreadImpl()
        val temperatureSystem = TemperatureSystem(updateThread)
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

        // TODO really update that clock
        val clock = Value(Instant.now())

        val mashSystem = MashSystem(
                potTemperature = temperatureSystem.temperatureView(TemperatureSystem.Sensor.Pot),
                potHeater = relaySystem.potHeater.value,
                clock = clock)

        val temperatureTolerance = 0.5
        val error = pidController(temperatureSystem.temperatureView(TemperatureSystem.Sensor.Cooler), configSystem.targetTemperature)
        Values.and(configSystem.coolerEnabled, error.map { it < -temperatureTolerance }).connectNotify(relaySystem.cooler.value.slot())
        Values.and(configSystem.heaterEnabled, error.map { it > temperatureTolerance }).connectNotify(relaySystem.heater.value.slot())

        environment.jersey().register(WebResource(updateThread, temperatureSystem, relaySystem, configSystem, mashSystem))
    }
}
