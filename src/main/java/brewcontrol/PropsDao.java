package brewcontrol;

import com.google.common.base.Optional;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

/** Dao for a simple String based key value table */
public abstract class PropsDao {

  @SqlUpdate("create table if not exists props (key TEXT primary key, value TEXT)")
  public abstract void createStorageTable();

  @SqlUpdate("insert or replace into props (key, value) values (:key, :value)")
  public abstract void set(@Bind("key") String key, @Bind("value") String value);

  @SqlQuery("select value from props where key = :key")
  @SingleValueResult(String.class)
  public abstract Optional<String> get(@Bind("key") String key);
}
