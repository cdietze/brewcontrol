package brewcontrol

import com.typesafe.scalalogging.LazyLogging
import rx.core.Var
import rx.ops._

import scala.concurrent.Await
import scala.concurrent.duration._

class RelayController(gpio: GpioConnection) extends LazyLogging {

  val relay1 = inversePin(Await.result(gpio.outPin(2), 5 seconds))

  val relayMap: Map[String, Var[Boolean]] = Map("Kühlung" -> relay1)

  logger.debug(s"Initialized $this")

  /** Return a inverse Var of a pin. Updating this Var will update the pin state. This is useful when pins and relays have opposite meanings for 'on' */
  private def inversePin(outPin: OutPin): Var[Boolean] = {
    new Var[Boolean](!outPin.now) {
      val o = this.map(v => outPin.update(!v))
    }
  }
}
