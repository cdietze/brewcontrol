package brewcontrol;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

/** Dao for a simple String based key value table */
public abstract class StorageDao {

  @SqlUpdate("create table if not exists storage (key TEXT primary key, value TEXT)")
  public abstract void createStorageTable();

  @SqlUpdate("insert or replace into storage (key, value) values (:key, :value)")
  public abstract void insert(@Bind("key") String key, @Bind("value") String value);

  @SqlQuery("select value from storage where key = :key")
  public abstract String get(@Bind("key") String key);
}
