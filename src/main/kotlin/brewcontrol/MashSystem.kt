package brewcontrol

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import react.Connection
import react.Value
import react.ValueView
import react.Values
import java.time.Instant
import java.util.concurrent.Future

/** A recipe can be serialized to and from JSON. Its uses are:
 * - Serialize into config DB
 * - Deserialize from config DB
 * - Serialize via HTTP resource
 * - Deserialize from HTTP request
 */
data class Recipe(
        /** The inactive of the currently active task if any.
         *
         * - `activeTaskIndex < 0` means the program has not started yet
         * - `activeTaskIndex == tasks.size` means the program is done
         */
        var activeTaskIndex: Int,
        val tasks: List<Task>)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = HeatTask::class, name = "HeatTask")
)
interface Task {

    class Context(val instant: Instant, val potTemperature: Double, val heater: Value<Boolean>)

    enum class StepResult {
        RUNNING, DONE
    }

    fun step(ctx: Context): StepResult
}

private val temperatureTolerance = 1.0

data class HeatTask(val temperature: Double, var startTime: Instant? = null) : Task {
    override fun step(ctx: Task.Context): Task.StepResult {
        if (startTime == null) startTime = ctx.instant
        val shouldHeat = ctx.potTemperature < temperature - temperatureTolerance
        ctx.heater.update(shouldHeat)
        return if (shouldHeat) Task.StepResult.RUNNING else Task.StepResult.DONE
    }
}

class MashSystem(
        val updateThread: UpdateThread,
        val potTemperature: ValueView<Double?>,
        val potHeater: Value<Boolean>,
        val clock: ValueView<Instant>,
        var recipe: Recipe = Recipe(-1, emptyList())) {

    var reactConnection: Connection? = null

    fun start(): Future<*> {
        return updateThread.runOnUpdateThread {
            if (reactConnection != null) {
                log.warn("Already running")
            } else {
                reactConnection = Values.join(clock, potTemperature).connectNotify { t1, t2 ->
                    log.info("Stepping, clock: {}, pot: {}", clock.get(), potTemperature.get())
                    step(clock.get())
                }
            }
        }
    }

    fun stop(): Future<*> {
        return updateThread.runOnUpdateThread {
            potHeater.update(false)
            reactConnection?.close()
            reactConnection = null
        }
    }

    fun isRunning(): Boolean {
        return updateThread.runOnUpdateThread {
            reactConnection != null
        }.get()
    }

    private fun step(instant: Instant) {
        val potTemp = potTemperature.get()
        if (potTemp == null) {
            log.error("Pot temperature not available while running recipe, will turn off heater")
            potHeater.update(false)
            return
        }
        val ctx = Task.Context(instant, potTemp, potHeater)
        // Start the recipe if it has not already
        if (recipe.activeTaskIndex < 0) recipe.activeTaskIndex = 0

        tailrec fun impl() {
            val currentTask = recipe.tasks.getOrElse(recipe.activeTaskIndex, {
                log.info("Recipe done")
                stop()
                return
            })

            when (currentTask.step(ctx)) {
                Task.StepResult.RUNNING -> {
                }
                Task.StepResult.DONE -> {
                    recipe.activeTaskIndex++
                    impl() // Run the following task immediately
                }
            }
        }
        impl()
    }
}
