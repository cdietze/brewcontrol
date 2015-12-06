package brewcontrol

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class WebResource {

    @GET
    fun hi(): String {
        return "Hi from Brewcontrol"
    }
}
