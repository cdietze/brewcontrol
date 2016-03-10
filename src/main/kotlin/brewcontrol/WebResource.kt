package brewcontrol

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class WebResource(
        val objectMapper: ObjectMapper,
        val updateThread: UpdateThread,
        val temperatureSystem: TemperatureSystem,
        val relaySystem: RelaySystem,
        val configSystem: ConfigSystem,
        val syncMashSystem: SynchronizedMashSystem) {

    data class StateResponse(
            val temperatures: Map<String, Double>,
            val relays: Map<String, Boolean>,
            val recipe: Recipe,
            val recipeProcess: RecipeProcess,
            val config: StateResponse.Config) {
        data class Config(
                val coolerEnabled: Boolean,
                val heaterEnabled: Boolean,
                val targetTemperature: Double) {
            constructor(configSystem: ConfigSystem) : this(
                    configSystem.coolerEnabled.get(),
                    configSystem.heaterEnabled.get(),
                    configSystem.targetTemperature.get())
        }
    }

    @GET
    @Path("state")
    fun state(): StateResponse {
        val f: Future<StateResponse> = updateThread.runOnUpdateThread {
            val temperatures = temperatureSystem.temperatures.get().mapKeys { it -> temperatureSystem.getLabel(it.key) }
            val relays = relaySystem.relays.map { it -> Pair(it.label, it.value.get()) }.toMap()
            val recipe = syncMashSystem.mashSystem.recipeProcess.recipe
            val recipeProcess = syncMashSystem.mashSystem.recipeProcess
            val config = StateResponse.Config(configSystem)
            StateResponse(temperatures, relays, recipe, recipeProcess, config)
        }
        return f.get(30, TimeUnit.SECONDS)
    }

    @PUT
    @Path("config/{key}")
    fun putConfig(@PathParam("key") key: String, value: String) {
        when (key) {
            "coolerEnabled" -> updateThread.runOnUpdateThread {
                configSystem.coolerEnabled.update(value.toBoolean())
            }
            "heaterEnabled" -> updateThread.runOnUpdateThread {
                configSystem.heaterEnabled.update(value.toBoolean())
            }
            "targetTemperature" -> {
                val t = try {
                    value.toDouble()
                } catch (e: NumberFormatException) {
                    throw WebApplicationException("Malformed double: '$value'", Response.Status.BAD_REQUEST)
                }
                updateThread.runOnUpdateThread {
                    configSystem.targetTemperature.update(t)
                }
            }
        }
    }

    @PUT
    @Path("recipe")
    fun updateRecipe(recipe: Recipe) {
        log.info("Update recipe requested, recipe: $recipe")
        configSystem.recipe.update(recipe)
        syncMashSystem.setRecipe(recipe)
    }

    @POST
    @Path("recipe/start")
    fun startRecipe() {
        log.info("Start recipe requested")
        syncMashSystem.start()
    }

    @POST
    @Path("recipe/skipTask")
    fun skipRecipeTask() {
        log.info("Skip recipe task requested")
        syncMashSystem.skipTask()
    }

    @POST
    @Path("recipe/reset")
    fun resetRecipe() {
        log.info("Reset recipe requested")
        syncMashSystem.reset()
    }
}
