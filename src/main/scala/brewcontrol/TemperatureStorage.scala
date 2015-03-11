package brewcontrol

import brewcontrol.TemperatureReader.Reading

class TemperatureStorage()(implicit mongoConnection: MongoConnection) extends TimeSeriesStorage(mongoConnection.db("temperatures")) {

  def persist(reading: Reading): Unit = persist(reading.sensorId, reading.timestamp, reading.value)
}
