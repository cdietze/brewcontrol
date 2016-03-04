package brewcontrol

import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class WebResource(
        val temperatureSystem: TemperatureSystem,
        val relaySystem: RelaySystem,
        val configSystem: ConfigSystem) {

    data class StateResponse(
            val temperatures: Map<String, Double>,
            val relays: Map<String, Boolean>,
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
        val f: Future<StateResponse> = UpdateThread.executor.submit(Callable { ->
            val t = temperatureSystem.temperatures.get().mapKeys { it -> temperatureSystem.getLabel(it.key) }
            val r = relaySystem.relays.map{it -> Pair(it.label, it.value.get())}.toMap()
            val c = StateResponse.Config(configSystem)
            StateResponse(t, r, c)
        })
        return f.get(30, TimeUnit.SECONDS)
    }

    @PUT
    @Path("config/{key}")
    fun putConfig(@PathParam("key") key: String, value: String) {
        when (key) {
            "coolerEnabled" -> UpdateThread.executor.submit {
                configSystem.coolerEnabled.update(value.toBoolean())
            }
            "heaterEnabled" -> UpdateThread.executor.submit {
                configSystem.heaterEnabled.update(value.toBoolean())
            }
            "targetTemperature" -> {
                val t = try {
                    value.toDouble()
                } catch (e: NumberFormatException) {
                    throw WebApplicationException("Malformed double: '$value'", Response.Status.BAD_REQUEST)
                }
                UpdateThread.executor.submit {
                    configSystem.targetTemperature.update(t)
                }
            }
        }
    }
}
