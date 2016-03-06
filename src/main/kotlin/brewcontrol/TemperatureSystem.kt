package brewcontrol

import brewcontrol.TemperatureSystem.Sensor
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import react.Value
import react.ValueView
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TemperatureSystem(val updateThread: UpdateThread) {
    val log = LoggerFactory.getLogger(TemperatureSystem::class.java)

    enum class Sensor(val id: String, val label: String) {
        Bucket("28-031462078cff", "Gäreimer"),
        Cooler("28-0214638301ff", "Kühlschrank"),
        Outside("28-011463e799ff", "Außen"),
        Pot("28-02146345f4ff", "Kessel")
    }

    val temperatures = Value<Map<String, Double>>(Collections.emptyMap())

    fun getLabel(sensorId: String): String = Sensor.values().find({ it.id == sensorId })?.label ?: sensorId

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
            updateThread.runOnUpdateThread {
                log.trace("Updating temperatures: ${readings}")
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
                Sensor.Cooler.id to random.nextInt(20).toDouble(),
                Sensor.Outside.id to random.nextInt(40).toDouble(),
                Sensor.Pot.id to random.nextInt(80).toDouble())
    }
}

/** Implementation that really reads from the Pi's temperature sensors */
class RealTemperatureReader : TemperatureReader {
    private object Paths {
        val w1MasterSlaves: String = "/sys/bus/w1/devices/w1_bus_master1/w1_master_slaves"
        fun w1Slave(sensorId: String): String = "/sys/bus/w1/devices/$sensorId/w1_slave"
    }

    override fun readings(): Map<String, Double> {
        return sensorIds().map { it -> Pair(it, temperature(it)) }.toMap()
    }

    private fun sensorIds(): List<String> {
        return File(Paths.w1MasterSlaves).readLines().filter {
            // Filter out any devices that don't have w1_slave files. Whatever they are, they aren't the sensors we seek
            id ->
            File(Paths.w1Slave(id)).exists()
        }
    }

    private fun temperature(sensorId: String): Double {
        return parse(File(Paths.w1Slave(sensorId)).readLines())
    }

    private fun parse(lines: List<String>): Double {
        return lines.map { line -> "t=(-?\\d+)".toRegex().find(line)?.groups?.get(1)?.value?.toInt() }
                .filterNotNull()
                .map { it / 1000.0 }
                .first()
    }
}
