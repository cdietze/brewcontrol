package brewcontrol

import scala.concurrent.Future

class MockGpio extends Gpio {
  override def outPin(pinNumber: Int): Future[GpioOutPin] = {
    Future.successful(new GpioOutPin(pinNumber))
  }
}
