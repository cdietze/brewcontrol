package brewcontrol

import brewcontrol.TemperatureReader.Readings
import rx.core.Var

class MockTemperatureReader extends TemperatureReader {
  val currentReadings = new Var[Readings](List())
}
