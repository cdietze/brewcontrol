import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.pi4j.io.gpio.{GpioFactory, PinPullResistance, RaspiPin}
import rx._
import rx.core.Var

object Main extends App {

  println("Hello")

  val gpio = GpioFactory.getInstance()

  val rxInput0 = inputPinRx(0)

  val msg = Rx {
    s"The heat is ${if (rxInput0()) "on" else "off"}"
  }

  val o = Obs(msg) {
    println(msg())
  }

  while (true) {
    println(s"waiting...")
    Thread.sleep(2000)
  }

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
}
