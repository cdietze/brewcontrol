package brewcontrol

import brewcontrol.TemperatureReader.{Reading, Readings}
import org.joda.time.DateTime
import rx.core.Var

class MockTemperatureReader extends TemperatureReader {
  val currentReadings = new Var[Readings](List(Reading(DateTime.now, "SensorA", 123.4f)))
}
