package brewcontrol

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import react.Value
import java.time.Instant
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

class MashSystemTest {

    val updateThread = object : UpdateThread {
        override fun <T> runOnUpdateThread(f: () -> T): Future<T> {
            // I couldn't find an implementation for a completed future so we use this hack
            val t = FutureTask(f)
            t.run()
            return t
        }
    }

    @Test
    fun heatsUpTo10Degrees() {
        val potTemp = Value(0.0)
        val heater = Value(false)
        val startInstant = Instant.now()
        val clock = Value(startInstant)
        val recipe = Recipe(0, listOf(HeatTask(10.0)))
        val mash = MashSystem(updateThread, potTemp, heater, clock, recipe)

        mash.start().get()

        assertThat(heater.get()).isTrue()
        clock.update(startInstant.plusSeconds(5))
        assertThat(heater.get()).isTrue()
        potTemp.update(5.0)
        assertThat(mash.isRunning()).isTrue()
        assertThat(heater.get()).isTrue()
        potTemp.update(10.0)
        assertThat(mash.isRunning()).isFalse()
        assertThat(heater.get()).isFalse()

        // After the program stopped, nothing shall be updated
        potTemp.update(5.0)
        assertThat(mash.isRunning()).isFalse()
        assertThat(heater.get()).isFalse()
    }
}