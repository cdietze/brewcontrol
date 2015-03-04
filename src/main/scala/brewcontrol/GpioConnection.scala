package brewcontrol

import java.io.IOException

import com.typesafe.scalalogging.LazyLogging
import rx._
import rx.ops._
import sbt.Path._
import sbt._

class OutPin(pinNumber: Int) extends Var[Boolean](false)

class GpioConnection extends LazyLogging {

  def unexport(pinNumber: Int): Unit = {
    try {
      IO.write(Paths.unexport, pinNumber.toString)
    } catch {
      // Ignore the "IOException: Invalid argument" when a pin is already unexported
      case e: IOException =>
    }
  }

  def export(pinNumber: Int): Unit = {
    try {
      IO.write(Paths.export, pinNumber.toString)
    } catch {
      // Ignore the "IOException: Device or resource busy" when a pin is already exported
      case e: IOException =>
    }
  }

  def outPin(pinNumber: Int): OutPin = {
    export(pinNumber)
    IO.write(Paths.direction(pinNumber), "out")
    new OutPin(pinNumber) {
      val obs: Obs = this.foreach {
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
    val unexport = gpioPath / "unexport"
    def pinPath(pinNumber: Int) = gpioPath / s"gpio$pinNumber"
    def direction(pinNumber: Int) = pinPath(pinNumber) / "direction"
    def value(pinNumber: Int) = pinPath(pinNumber) / "value"
  }
}

