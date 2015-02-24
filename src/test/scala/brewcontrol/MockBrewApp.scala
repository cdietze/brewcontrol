package brewcontrol

object MockBrewApp extends AbstractBrewApp {
  override def host = "localhost"
  override lazy val temperatureConnection = new MockTemperatureConnection
  override lazy val mongoConnection = new MongoConnection
}