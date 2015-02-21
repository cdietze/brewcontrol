package brewcontrol

import scala.util.Try

object MockTemperatureConnection extends TemperatureConnection {
  override def sensorIds() = Try(Set("SensorA", "SensorB"))

  override def temperature(sensorId: String) = Try(24.5f)
}
