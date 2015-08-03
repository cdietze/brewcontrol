package brewcontrol;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class BrewApplication extends Application<BrewConfiguration> {

  public static void main(String[] args) throws Exception {
    new BrewApplication().run(args);
  }

  @Override
  public String getName() {
    return "brewcontrol";
  }
  @Override
  public void run(BrewConfiguration config, Environment environment) {
    environment.jersey().register(RelayResource.class);
  }
}
