package brewcontrol

import java.time.Instant

import brewcontrol.History.Item

import scala.util.{Random, Try}

object MockBrewApp extends AbstractBrewApp {

  val startTime = Instant.now().toEpochMilli
  History.addItem("Heizung", "binary", Item(startTime + 10000, 0))
  History.addItem("Heizung", "binary", Item(startTime + 12000, 1))
  History.addItem("Heizung", "binary", Item(startTime + 14000, 0))
  History.addItem("Heizung", "binary", Item(startTime + 20000, 1))

  override def port = 8888
  override def jdbcUrl = "jdbc:sqlite:data.sqlite"

  override lazy val temperatureConnection = new MockTemperatureConnection {
    override def temperature(sensorId: String) = Try(Random.nextFloat() * 10f + 10f)
  }
  override lazy val gpio = new MockGpioConnection
}