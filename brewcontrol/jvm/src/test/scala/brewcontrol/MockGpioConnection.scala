package brewcontrol

import scala.concurrent.Future

class MockGpioConnection extends GpioConnection {
  override def outPin(pinNumber: Int): Future[OutPin] = {
    Future.successful(new OutPin(pinNumber))
  }
}
