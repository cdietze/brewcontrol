package brewcontrol

import org.junit.Test
import react.Value
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat

class MashSystemTest {

    @Test
    fun heatsUpTo10Degrees() {
        val potTemp = Value(0.0)
        val heater = Value(false)
        val startInstant = Instant.now()
        val clock = Value(startInstant)
        val recipe = Recipe(0, listOf(HeatTask(10.0)))
        val mash = MashSystem(potTemp, heater, clock, recipe)

        mash.start()

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