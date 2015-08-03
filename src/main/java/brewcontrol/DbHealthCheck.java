package brewcontrol;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Preconditions;

public class DbHealthCheck extends HealthCheck {
  private final StorageDao dao;
  public DbHealthCheck(StorageDao dao) {this.dao = dao;}
  @Override
  protected Result check() throws Exception {
    String value = String.valueOf(System.currentTimeMillis());
    dao.insert("healthCheck", value);
    String value2 = dao.get("healthCheck");
    Preconditions.checkState(value.equals(value2), "Expected %s, found %s", value, value2);
    return Result.healthy();
  }
}
