package brewcontrol

import com.typesafe.scalalogging.LazyLogging
import rx.core.Var
import rx.ops._

import scala.concurrent.Await
import scala.concurrent.duration._

class RelayController()(implicit gpio: GpioConnection) extends LazyLogging {
  sealed class Relay(pinNumber: Int) {
    val outPin = Await.result(gpio.outPin(pinNumber), 5 seconds)
    val value = inversePin(outPin)
    value.update(false)
  }

  case object Cooler extends Relay(7)
  case object Heater extends Relay(8)
  case object Relay3 extends Relay(25)
  case object Relay4 extends Relay(24)

  val relayMap: Map[String, Relay] = Map("Kühlung" -> Cooler, "Heizung" -> Heater)

  logger.debug(s"Initialized $this")

  /** Return a inverse Var of a pin. Updating this Var will update the pin state. This is useful when pins and relays have opposite meanings for 'on' */
  private def inversePin(outPin: OutPin): Var[Boolean] = {
    new Var[Boolean](!outPin.now) {
      val o = this.map(v => outPin.update(!v))
    }
  }
}
