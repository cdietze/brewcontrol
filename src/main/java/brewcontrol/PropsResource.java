package brewcontrol;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("props")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class PropsResource {

  private final PropsDao propsDao;
  public PropsResource(PropsDao propsDao) {
    this.propsDao = propsDao;
  }

  @Path("targetTemperature")
  @GET
  public double getTargetTemperature() {
    return Double.parseDouble(propsDao.get("targetTemperature").or("10.0"));
  }

  @Path("targetTemperature")
  @POST
  public void setTargetTemperature(String value) {
    double v = Double.parseDouble(value);
    propsDao.set("targetTemperature", Double.toString(v));
  }

  @Path("heaterEnabled")
  @GET
  public boolean getHeaterEnabled() {
    return Boolean.parseBoolean(propsDao.get("heaterEnabled").or("false"));
  }

  @Path("heaterEnabled")
  @POST
  public void setHeaterEnabled(boolean value) {
    propsDao.set("heaterEnabled", Boolean.toString(value));
  }
}
