import com.pi4j.io.gpio.{GpioFactory, RaspiPin}

object Main extends App {

  println("Hello")

  val gpio = GpioFactory.getInstance()

  val input = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00)

  while (true) {
    println(s"input value: ${input.getState.isHigh}")
    Thread.sleep(1000)
  }
}
