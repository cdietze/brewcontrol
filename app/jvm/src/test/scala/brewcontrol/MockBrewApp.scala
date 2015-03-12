package brewcontrol

object MockBrewApp extends AbstractBrewApp {
  override def host = "localhost"
  override def port = 8888
  override lazy val temperatureConnection = new MockTemperatureConnection
  override lazy val mongoConnection = new MockMongoConnection
  override lazy val gpio = new MockGpioConnection
}