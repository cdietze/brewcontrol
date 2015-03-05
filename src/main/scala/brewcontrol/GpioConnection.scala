package brewcontrol

import java.io.IOException
import java.util.concurrent.TimeoutException

import akka.actor
import com.typesafe.scalalogging.LazyLogging
import rx._
import rx.ops._
import sbt.Path._
import sbt._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

class OutPin(pinNumber: Int) extends Var[Boolean](false)

trait GpioConnection {
  def outPin(pinNumber: Int): Future[OutPin]
}

class GpioConnectionImpl(implicit scheduler: actor.Scheduler, ec: ExecutionContext) extends GpioConnection with LazyLogging {

  object Paths {
    val gpioPath = Path("/sys/class/gpio")
    val export = gpioPath / "export"
    val unexport = gpioPath / "unexport"
    def pinPath(pinNumber: Int) = gpioPath / s"gpio$pinNumber"
    def direction(pinNumber: Int) = pinPath(pinNumber) / "direction"
    def value(pinNumber: Int) = pinPath(pinNumber) / "value"
  }

  private def export(pinNumber: Int): Future[Unit] = {
    try {
      logger.debug(s"Exporting pin $pinNumber")
      IO.write(Paths.export, pinNumber.toString)
    } catch {
      // Ignore the "IOException: Device or resource busy" when a pin is already exported
      case e: IOException => {
        logger.debug(s"Ignoring exception while exporting pin $pinNumber: $e")
      }
    }
    sys.addShutdownHook(unexport(pinNumber))
    waitUntil(Paths.pinPath(pinNumber).exists(), 1 second)
  }

  private def unexport(pinNumber: Int): Unit = {
    try {
      logger.debug(s"Unexporting pin $pinNumber")
      IO.write(Paths.unexport, pinNumber.toString)
    } catch {
      // Ignore the "IOException: Invalid argument" when a pin is already unexported
      case e: IOException => logger.debug(s"Ignoring exception while unexporting pin $pinNumber: $e")
    }
  }

  def outPin(pinNumber: Int): Future[OutPin] = {
    export(pinNumber).map[OutPin] { _ =>
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
  }

  // Utility function that waits until the condition is true and completes the returned future accordingly
  private def waitUntil(condition: => Boolean, timeout: FiniteDuration): Future[Unit] = {
    if (condition) Future.successful()
    else {
      val p: Promise[Unit] = Promise()
      scheduler.scheduleOnce(timeout)(
        p complete (
          if (condition) Success() else Failure(new TimeoutException(s"Timeout while waiting for condition"))
          )
      )
      p.future
    }
  }
}
