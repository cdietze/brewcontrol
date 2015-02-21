import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.pi4j.io.gpio.{GpioFactory, PinPullResistance, RaspiPin}
import rx._
import rx.core.Var
import rx.ops.{Scheduler, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Try

class Gpio(implicit scheduler: Scheduler, ex: ExecutionContext) {

  private val gpio = GpioFactory.getInstance()

  private lazy val pollTimer = Timer(1500 millis)

  def inputPinRx(pinNumber: Int): Rx[Boolean] = {
    val result = Var(false)
    val input = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_UP)
    input.addListener(new GpioPinListenerDigital {
      override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent) = {
        result() = event.getState.isHigh
      }
    })
    result
  }

  def temperatureSensor(name: String): Rx[Float] = {
    def read(): Try[Float] = {
      val path = s"/sys/bus/w1/devices/$name/w1_slave"
      Try((for {
        line <- Source.fromFile(path).getLines()
        s <- "t=(\\d+)".r.findFirstMatchIn(line).map(_.group(1))
        f = s.toInt / 1000f
      } yield f).toSeq.head)
    }
    new rx.Var[Float](read().get) {
      // Make the observer a field of this new instance so that they get garbage collected together
      val o = pollTimer.foreach(_ => update(read().get))
    }
  }
}
