package brewcontrol

import com.typesafe.scalalogging.LazyLogging
import rx.core.Var
import rx.ops._
import sbt._

class OutPin(pinNumber: Int) extends Var[Boolean](false)

class GpioConnection extends LazyLogging {

  def outPin(pinNumber: Int): OutPin = {

    if (!Paths.direction(pinNumber).exists()) {
      IO.write(Paths.export, pinNumber.toString)
      assert(Paths.direction(pinNumber).exists())
    }
    IO.write(Paths.direction(pinNumber), "out")

    new OutPin(pinNumber) {
      val obs = this.foreach {
        value: Boolean => {
          logger.debug(s"Writing $value to pin $pinNumber")
          IO.write(Paths.value(pinNumber), if (value) "1" else "0")
        }
      }
    }
  }

  object Paths {
    val gpioPath = Path("/sys/class/gpio")
    val export = gpioPath / "export"
    def direction(pinNumber: Int) = gpioPath / s"/gpio$pinNumber/direction"
    def value(pinNumber: Int) = gpioPath / s"/gpio$pinNumber/value"
  }
}

