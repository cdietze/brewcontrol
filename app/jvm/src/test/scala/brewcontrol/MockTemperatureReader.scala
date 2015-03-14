package brewcontrol

import org.joda.time.DateTime
import rx.core.Var

class MockTemperatureReader extends TemperatureReader {
  val mockSensorId = "SensorA"
  val currentReadings = new Var[Readings](List(Reading(DateTime.now, mockSensorId, 123.4f)))
}
