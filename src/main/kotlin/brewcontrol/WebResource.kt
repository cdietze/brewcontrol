package brewcontrol

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class WebResource(val temperatureSystem: TemperatureSystem) {

    @GET
    fun hi(): Map<String, Double>? {
        val t = UpdateThread.executor.run {
            return temperatureSystem.temperatures.get()
        }
        return t
    }
}
