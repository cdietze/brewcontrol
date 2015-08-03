package brewcontrol;

import io.dropwizard.Application;
import io.dropwizard.jdbi.DBIFactory;
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
  public void run(BrewConfiguration config, Environment environment) {
    environment.jersey().register(RelayResource.class);
    final DBIFactory factory = new DBIFactory();
    final DBI jdbi = factory.build(environment, config.database, "sqlite");
    StorageDao dao = jdbi.onDemand(StorageDao.class);
    dao.createStorageTable();
    environment.healthChecks().register(DbHealthCheck.class.getName(), new DbHealthCheck(dao));
  }
}
