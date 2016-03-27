package brewcontrol

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.validation.Valid
import javax.validation.constraints.NotNull

val log = LoggerFactory.getLogger("brewcontrol")

fun main(args: Array<String>) {
    println("[${Date()}] Starting BrewControl")
    BrewApplication().run(*args)
}

private fun createObjectMapper(): ObjectMapper {
    val om = Jackson.newObjectMapper()
    om.registerModule(JavaTimeModule())
    om.registerModule(KotlinModule())
    // Be lax about unknown properties so the UI needs to do less sanitizing
    om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return om
}

/** Global [ObjectMapper] instance. I guess global is ok here, since it does not change and it is thread-safe. */
val objectMapper = createObjectMapper()

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
        bootstrap.objectMapper = objectMapper
        bootstrap.addBundle(AssetsBundle("/assets/", "/", "index.html"))
    }

    override fun run(configuration: BrewConfiguration, environment: Environment) {
        log.info("Running BrewApplication")
        val updateThread = UpdateThreadImpl()
        val temperatureSystem = TemperatureSystem(updateThread)
        val relaySystem = RelaySystem()
        if (configuration.gpioEnabled) relaySystem.wireToGpio(gpioImpl)

        val temperatureReader = if (configuration.mockTemperatures) MockTemperatureReader(relaySystem) else RealTemperatureReader()

        temperatureSystem.startUpdateScheduler(temperatureReader)

        val factory = DBIFactory()
        val jdbi = factory.build(environment, configuration.database, "SQLite")
        val configDao = jdbi.onDemand(ConfigDao::class.java).apply {
            createTable()
        }
        val configSystem = ConfigSystem(configDao)


        val clock = Value(Instant.now())
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            clock.update(Instant.now())
            log.debug("Updated clock: {}", clock.get())
        }, 0, 5, TimeUnit.SECONDS);

        val mashSystem = MashSystem(
                potTemperature = temperatureSystem.temperatureView(TemperatureSystem.Sensor.Pot),
                potHeater = relaySystem.potHeater.value,
                clock = clock)

        mashSystem.setRecipe(configSystem.recipe.get())

        val syncMashSystem = SynchronizedMashSystem(mashSystem, updateThread)

        val temperatureTolerance = 0.5
        val error = pidController(temperatureSystem.temperatureView(TemperatureSystem.Sensor.Cooler), configSystem.targetTemperature)
        Values.and(configSystem.coolerEnabled, error.map { it < -temperatureTolerance }).connectNotify(relaySystem.cooler.value.slot())
        Values.and(configSystem.heaterEnabled, error.map { it > temperatureTolerance }).connectNotify(relaySystem.heater.value.slot())

        environment.jersey().register(WebResource(updateThread, temperatureSystem, relaySystem, configSystem, syncMashSystem))
    }
}
