package brewcontrol

object MockBrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new MockTemperatureConnection
  override lazy val mongoConnection = new MongoConnection
}