package brewcontrol;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.OptionalContainerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.skife.jdbi.v2.DBI;

public class BrewApplication extends Application<BrewConfiguration> {

  public static void main(String[] args) throws Exception {
    new BrewApplication().run(args);
  }

  @Override
  public String getName() {
    return "brewcontrol";
  }

  @Override
  public void initialize(Bootstrap<BrewConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/assets/", "/"));
  }

  @Override
  public void run(BrewConfiguration config, Environment environment) {
    final DBIFactory factory = new DBIFactory();
    final DBI jdbi = factory.build(environment, config.database, "sqlite");
    jdbi.registerContainerFactory(new OptionalContainerFactory());
    PropsDao propsDao = jdbi.onDemand(PropsDao.class);
    propsDao.createStorageTable();
    environment.healthChecks().register(DbHealthCheck.class.getName(), new DbHealthCheck(propsDao));

    environment.jersey().register(new RelayResource());
    environment.jersey().register(new PropsResource(propsDao));
  }
}
