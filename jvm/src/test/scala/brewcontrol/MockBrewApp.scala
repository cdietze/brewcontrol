package brewcontrol

import scala.util.{Random, Try}

object MockBrewApp extends AbstractBrewApp {
  override def port = 8888
  override def jdbcUrl = "jdbc:sqlite:data.sqlite"

  override lazy val temperatureConnection = new MockTemperatureConnection {
    override def temperature(sensorId: String) = Try(Random.nextFloat() * 10f + 10f)
  }
  override lazy val gpio = new MockGpioConnection
}