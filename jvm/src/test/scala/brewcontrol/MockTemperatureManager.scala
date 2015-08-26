package brewcontrol

import java.time.Instant

import rx.core.Var

class MockTemperatureManager extends TemperatureManager {
  val mockSensorId = "SensorA"
  val currentReadings = new Var[Readings](List(TemperatureReading(Instant.now.toEpochMilli, mockSensorId, s"$mockSensorId name", 123.4f)))
}
