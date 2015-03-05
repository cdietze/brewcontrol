package brewcontrol

import scala.concurrent.Future

class MockGpioConnection extends GpioConnection {
  override def export(pinNumber: Int) = Future.successful()
  override def unexport(pinNumber: Int) = Future.successful()
  override def outPin(pinNumber: Int): Future[OutPin] = {
    Future.successful(new OutPin(pinNumber))
  }
}
