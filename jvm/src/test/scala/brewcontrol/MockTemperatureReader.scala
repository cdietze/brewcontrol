package brewcontrol

import java.time.Instant

import rx.core.Var

class MockTemperatureReader extends TemperatureReader {
  val mockSensorId = "SensorA"
  val currentReadings = new Var[Readings](List(Reading(Instant.now.toEpochMilli, mockSensorId, s"$mockSensorId name", 123.4f)))
}
