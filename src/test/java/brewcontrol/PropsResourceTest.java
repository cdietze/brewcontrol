package brewcontrol;

import io.dropwizard.jdbi.OptionalContainerFactory;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.assertj.core.data.Offset;
import org.junit.ClassRule;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.sqlite.SQLiteDataSource;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class PropsResourceTest {

  private static PropsDao inMemoryStorageDao() {
    SQLiteDataSource ds = new SQLiteDataSource();
    ds.setUrl("jdbc:sqlite:unittest.db");
    DBI jdbi = new DBI(ds);
    jdbi.registerContainerFactory(new OptionalContainerFactory());

    PropsDao propsDao = jdbi.onDemand(PropsDao.class);
    propsDao.createStorageTable();
    return propsDao;
  }

  private static final PropsDao propsDao = inMemoryStorageDao();

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
      .addResource(new PropsResource(propsDao))
      .build();

  @Test
  public void wayne() {
    Response response = resources.client().target("/props/targetTemperature").request().post(Entity.text("10.0"));
    assertThat(response.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);
    String s = resources.client().target("/props/targetTemperature").request().get().readEntity(String.class);
    assertThat(Double.parseDouble(s)).isCloseTo(10.0d, Offset.offset(0.01d));
  }
}
