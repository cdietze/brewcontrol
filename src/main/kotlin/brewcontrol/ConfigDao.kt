package brewcontrol

import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import react.Value

/**
 * SQLite allows to have values of different types in the same column.
 * So we use that to store different types of data (instead of serialization).
 */
interface ConfigDao {
    @SqlUpdate("create table if not exists config (key string primary key, value blob)")
    fun createTable(): Unit

    @SqlUpdate("insert or replace into config (key,value) values (:key, :value)")
    fun<T> put(@Bind("key") key: String, @Bind("value") value: T): Unit

    @SqlQuery("select value from config where key = :key")
    fun getString(@Bind("key") key: String): String?

    @SqlQuery("select value from config where key = :key")
    fun getDouble(@Bind("key") key: String): Double?

    @SqlQuery("select value from config where key = :key")
    fun getBoolean(@Bind("key") key: String): Boolean?

    companion object {
        fun booleanValue(dao: ConfigDao, key: String, init: Boolean = false): Value<Boolean> {
            return Value(dao.getBoolean(key) ?: init).apply {
                connect { v: Boolean -> dao.put(key, v) }
            }
        }

        fun doubleValue(dao: ConfigDao, key: String, init: Double): Value<Double> {
            return Value(dao.getDouble(key) ?: init).apply {
                connect { v: Double -> dao.put(key, v) }
            }
        }
    }
}

class ConfigSystem(val dao: ConfigDao) {
    val coolerEnabled = ConfigDao.booleanValue(dao, "coolerEnabled")
    val heaterEnabled = ConfigDao.booleanValue(dao, "heaterEnabled")
    val targetTemperature = ConfigDao.doubleValue(dao, "targetTemperature", 10.0)
}
