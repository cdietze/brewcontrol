package brewcontrol

class TemperatureStorage()(implicit mongoConnection: MongoConnection) extends TimeSeriesStorage(mongoConnection.db("temperatures")) {
  def persist(reading: Reading): Unit = persist(reading.sensorId, reading.timestamp, reading.value)
}
class RelayStorage()(implicit mongoConnection: MongoConnection, clock: Clock) extends TimeSeriesStorage(mongoConnection.db("relays"))
