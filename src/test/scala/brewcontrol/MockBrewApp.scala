package brewcontrol

object MockBrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = MockTemperatureConnection
}