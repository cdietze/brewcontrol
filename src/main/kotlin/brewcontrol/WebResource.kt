package brewcontrol

import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class WebResource(
        val temperatureSystem: TemperatureSystem,
        val relaySystem: RelaySystem) {

    data class StateResponse(
            val temperatures: Map<String, Double>,
            val relays: Map<String, Boolean>)

    @GET
    @Path("state")
    fun state(): StateResponse {
        val f: Future<StateResponse> = UpdateThread.executor.submit(Callable { ->
            val t = temperatureSystem.temperatures.get()
            val r = relaySystem.relays.toMap({ it.label }, { it.value.get() })
            StateResponse(t, r)
        })
        return f.get(30, TimeUnit.SECONDS)
    }
}
