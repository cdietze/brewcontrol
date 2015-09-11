package brewcontrol

import scala.util.Try

class MockTemperatureConnection extends TemperatureConnection {
  override def sensorIds() =  Try(Set("28-031462078cff", "28-0214638301ff", "28-011463e799ff", "28-02146345f4ff"))

  override def temperature(sensorId: String) = Try(24.5f)
}
