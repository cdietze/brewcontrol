package brewcontrol

import scala.util.Try

class MockTemperatureConnection extends TemperatureConnection {
  override def sensorIds() = Try(Set("SensorA"))

  override def temperature(sensorId: String) = Try(24.5f)
}
