package brewcontrol;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/relays")
@Produces(MediaType.APPLICATION_JSON)
public class RelayResource {

  @GET
  public String hi() {
    return "Hi from relays";
  }
}
