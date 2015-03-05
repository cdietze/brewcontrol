package brewcontrol

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration._

class RelayController(gpio: GpioConnection) extends LazyLogging {

  val relay1 = Await.result(gpio.outPin(2), 5 seconds)

  val relayMap: Map[String, OutPin] = Map("Kühlung" -> relay1)

  logger.debug(s"Initialized $this")
}
