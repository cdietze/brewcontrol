package brewcontrol

class MockGpioConnection extends GpioConnection {
  override def outPin(pinNumber: Int): OutPin = {
    new OutPin(pinNumber)
  }
}
