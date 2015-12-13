package brewcontrol

import org.slf4j.LoggerFactory
import react.Value
import java.io.File
import java.io.IOException
import kotlin.test.fail

class RelaySystem {
    val log = LoggerFactory.getLogger(RelaySystem::class.java)

    data class Relay(val pinNumber: Int, val label: String, val value: Value<Boolean> = Value<Boolean>(false)) {
        init {
            value.connect { v -> log.trace("Updated relay state: $this") }
        }
    }

    val cooler = Relay(7, "Kühlung")
    val heater = Relay(8, "Heizung")
    val potHeater = Relay(25, "Kessel");

    val relays: List<Relay> = listOf(cooler, heater, potHeater)

    fun wireToGpio(gpio: Gpio): RelaySystem {
        relays.forEach { relay ->
            log.debug("Wiring up $relay")
            val pin = gpio.createOutputPin(relay.pinNumber)
            relay.value.connectNotify({ v -> pin.set(v) })
        }
        return this
    }
}

interface Gpio {
    interface OutputPin {
        fun set(value: Boolean): Unit
    }

    fun createOutputPin(pinNumber: Int): OutputPin
}

object gpioImpl : Gpio {
    val log = LoggerFactory.getLogger(Gpio::class.java)

    override fun createOutputPin(pinNumber: Int): Gpio.OutputPin = object : Gpio.OutputPin {
        init {
            export(pinNumber)
            Paths.direction(pinNumber).writeText("out")
        }

        override fun set(value: Boolean) {
            // Do note that up (1) is off an down (0) is on
            Paths.value(pinNumber).writeText(if (value) "0" else "1")
        }
    }

    private object Paths {
        val baseDir = File("/sys/class/gpio")
        val export = File(baseDir, "export")
        val unexport = File(baseDir, "unexport")
        fun pinDir(pinNumber: Int) = File(baseDir, "gpio$pinNumber")
        fun direction(pinNumber: Int) = File(pinDir(pinNumber), "direction")
        fun value(pinNumber: Int) = File(pinDir(pinNumber), "value")
    }

    private fun export(pinNumber: Int) {
        try {
            log.debug("Exporting pin $pinNumber")
            Paths.export.writeText(pinNumber.toString())
        } catch (e: IOException) {
            log.debug("Ignoring exception while exporting pin $pinNumber, maybe it is already exported, exception: $e")
        }
        var maxTries = 10
        // Wait until the pin has become writable
        while (!Paths.direction(pinNumber).canWrite()) {
            if (--maxTries < 0) fail("Failed to export pin $pinNumber, ${Paths.direction(pinNumber)} never became writable")
            log.debug("Waiting for pin $pinNumber to become writable")
            Thread.sleep(500)
        }
        Runtime.getRuntime().addShutdownHook(Thread({
            unexportOnShutdown(pinNumber)
        }))
    }

    private fun unexportOnShutdown(pinNumber: Int) {
        // Logging is not available here so we print to stdout
        try {
            Paths.unexport.writeText(pinNumber.toString())
            println("Unexported pin $pinNumber")
        } catch (e: Exception) {
            println("Exception while unexporting pin $pinNumber, maybe it is already unexported, exception: $e")
        }
    }
}