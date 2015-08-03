package brewcontrol;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class BrewConfiguration extends Configuration {

  @Valid
  @NotNull
  @JsonProperty("database")
  public DataSourceFactory database = new DataSourceFactory();
}
