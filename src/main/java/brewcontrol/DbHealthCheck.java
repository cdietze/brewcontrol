package brewcontrol;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Preconditions;

public class DbHealthCheck extends HealthCheck {
  private final PropsDao dao;
  public DbHealthCheck(PropsDao dao) {this.dao = dao;}
  @Override
  protected Result check() throws Exception {
    String value = String.valueOf(System.currentTimeMillis());
    dao.set("healthCheck", value);
    String value2 = dao.get("healthCheck").get();
    Preconditions.checkState(value.equals(value2), "Expected %s, found %s", value, value2);
    return Result.healthy();
  }
}
