package brewcontrol

import scala.util.Try

class MockTemperatureConnection extends TemperatureConnection {
  override def sensorIds() = Try(Set("SensorA", "SensorB"))

  override def temperature(sensorId: String) = Try(24.5f)
}
