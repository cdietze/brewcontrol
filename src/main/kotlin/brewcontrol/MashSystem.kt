package brewcontrol

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import react.Connection
import react.Value
import react.ValueView
import react.Values
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future


/** A recipe is a immutable list of steps.
 */
data class Recipe(val steps: List<Step> = emptyList())

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = HeatStep::class, name = "Heat"),
        JsonSubTypes.Type(value = RestStep::class, name = "Rest"),
        JsonSubTypes.Type(value = HoldStep::class, name = "Hold")
)
interface Step {
}

data class HeatStep(val temperature: Double) : Step
data class RestStep(val duration: Duration) : Step

object HoldStep : Step

/** A [RecipeProcess] is the run-time representation of [Recipe] */
@JsonIgnoreProperties("recipe")
class RecipeProcess(val recipe: Recipe = Recipe()) {
    /** The index of the currently active task if any.
     *
     * - `activeTaskIndex < 0` means the program has not started yet
     * - `activeTaskIndex == tasks.size` means the program is done
     */
    var activeTaskIndex: Int = -1
    val tasks: List<Task> = recipeToTasks(recipe)
}

fun recipeToTasks(recipe: Recipe): List<Task> {
    var lastTemperature: Double? = null
    return recipe.steps.flatMap<Step, Task> { step ->
        when (step) {
            is HeatStep -> {
                lastTemperature = step.temperature
                listOf(HeatTask(step))
            }
            is RestStep -> listOf(RestTask(step, lastTemperature))
            is HoldStep -> listOf(HoldTask(step, lastTemperature))
            else -> error("Unknown step type: $step")
        }
    }
}

interface Task {

    class Context(val instant: Instant, val potTemperature: Double, val heater: Value<Boolean>)

    enum class StepResult {
        RUNNING, DONE
    }

    fun step(ctx: Context): StepResult
}

private val temperatureTolerance = 1.0

data class HeatTask(val step: HeatStep, var startTime: Instant? = null) : Task {
    override fun step(ctx: Task.Context): Task.StepResult {
        if (startTime == null) startTime = ctx.instant
        val shouldHeat = ctx.potTemperature < (step.temperature - temperatureTolerance)
        ctx.heater.update(shouldHeat)
        return if (shouldHeat) Task.StepResult.RUNNING else Task.StepResult.DONE
    }
}

data class RestTask(val step: RestStep, val temperature: Double?, var startTime: Instant? = null) : Task {
    override fun step(ctx: Task.Context): Task.StepResult {
        if (startTime == null) startTime = ctx.instant
        val shouldHeat = temperature != null && ctx.potTemperature < (temperature - temperatureTolerance)
        ctx.heater.update(shouldHeat)
        return if (Duration.between(startTime!!, ctx.instant) > step.duration) Task.StepResult.DONE else Task.StepResult.RUNNING
    }
}

data class HoldTask(val step: HoldStep, val temperature: Double?, var startTime: Instant? = null) : Task {
    override fun step(ctx: Task.Context): Task.StepResult {
        if (startTime == null) startTime = ctx.instant
        val shouldHeat = temperature != null && ctx.potTemperature < (temperature - temperatureTolerance)
        ctx.heater.update(shouldHeat)
        return Task.StepResult.RUNNING
    }
}

class MashSystem(
        val potTemperature: ValueView<Double?>,
        val potHeater: Value<Boolean>,
        val clock: ValueView<Instant>
) {
    val recipe: Recipe get() = recipeProcess.recipe

    var recipeProcess: RecipeProcess = RecipeProcess()

    fun setRecipe(recipe: Recipe) {
        stop()
        recipeProcess = RecipeProcess(recipe)
    }

    var reactConnection: Connection? = null

    fun start() {
        if (reactConnection != null) {
            log.warn("Already running")
        } else {
            reactConnection = Values.join(clock, potTemperature).connectNotify { t1, t2 ->
                log.info("Stepping, clock: {}, pot: {}", clock.get(), potTemperature.get())
                step()
            }
        }
    }

    private fun stop() {
        potHeater.update(false)
        reactConnection?.close()
        reactConnection = null
    }

    fun reset() {
        stop()
        recipeProcess = RecipeProcess(recipeProcess.recipe)
    }

    fun skipTask() {
        if (recipeProcess.activeTaskIndex < recipeProcess.tasks.size) recipeProcess.activeTaskIndex++
        step()
    }

    fun isRunning(): Boolean {
        return reactConnection != null
    }

    private fun step() {
        val potTemp = potTemperature.get()
        if (potTemp == null) {
            log.error("Pot temperature not available while running recipe, will turn off heater")
            potHeater.update(false)
            return
        }
        val ctx = Task.Context(clock.get(), potTemp, potHeater)
        // Start the recipe if it has not already
        if (recipeProcess.activeTaskIndex < 0) recipeProcess.activeTaskIndex = 0

        tailrec fun impl() {
            val currentTask = recipeProcess.tasks.getOrElse(recipeProcess.activeTaskIndex, {
                log.info("Recipe done")
                stop()
                return
            })

            when (currentTask.step(ctx)) {
                Task.StepResult.RUNNING -> {
                }
                Task.StepResult.DONE -> {
                    recipeProcess.activeTaskIndex++
                    impl() // Run the following task immediately
                }
            }
        }
        impl()
    }
}

/** Wrapper around [MashSystem] that runs all methods on the [UpdateThread] */
class SynchronizedMashSystem(val mashSystem: MashSystem, val updateThread: UpdateThread) {
    fun start(): Future<*> {
        return updateThread.runOnUpdateThread { mashSystem.start() }
    }

    fun reset(): Future<*> {
        return updateThread.runOnUpdateThread { mashSystem.reset() }
    }

    fun skipTask(): Future<*> {
        return updateThread.runOnUpdateThread { mashSystem.skipTask() }
    }

    fun setRecipe(recipe: Recipe): Future<*> {
        return updateThread.runOnUpdateThread { mashSystem.setRecipe(recipe) }
    }
}
