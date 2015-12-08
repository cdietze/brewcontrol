package brewcontrol

import brewcontrol.TemperatureSystem.Sensor
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import react.Value
import react.ValueView
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TemperatureSystem {
    val log = LoggerFactory.getLogger(TemperatureSystem::class.java)

    enum class Sensor(val id: String, val label: String) {
        Bucket("28-031462078cff", "Gäreimer"),
        Cooler("28-0214638301ff", "Kühlschrank"),
        Outside("28-011463e799ff", "Außen"),
        Pot("28-02146345f4ff", "Kessel")
    }

    val temperatures = Value<Map<String, Double>>(Collections.emptyMap())

    fun temperatureView(sensorId: String): ValueView<Double?> {
        return temperatures.map({ it.get(sensorId) })
    }

    fun temperatureView(sensor: Sensor) = temperatureView(sensor.id)

    fun startUpdateScheduler(reader: TemperatureReader) {
        val tf = ThreadFactoryBuilder().setNameFormat("temperature-%d").build()
        log.info("Starting update scheduler")
        Executors.newSingleThreadScheduledExecutor(tf).scheduleWithFixedDelay({
            val readings = reader.readings()
            log.debug("Finished reading temperatures: ${readings}")
            UpdateThread.executor.submit {
                log.debug("Updating temperatures: ${readings}")
                temperatures.update(readings)
            }
        }, 0, 1, TimeUnit.SECONDS)
    }
}

interface TemperatureReader {
    fun readings(): Map<String, Double>
}

class RandomTemperatureReader : TemperatureReader {
    val random = Random()
    override fun readings(): Map<String, Double> {
        return mapOf(
                Sensor.Cooler.id to random.nextInt(50).toDouble(),
                Sensor.Outside.id to random.nextInt(50).toDouble())
    }
}