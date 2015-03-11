package brewcontrol

import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest

class WebTest extends FlatSpec with Matchers with ScalatestRouteTest with BrewHttpService {
  def actorRefFactory = system

  implicit val mongoConnection = new MockMongoConnection
  implicit val gpio = new MockGpioConnection

  lazy val temperatureReader: TemperatureReader = new MockTemperatureReader()
  lazy val relayController: RelayController = new RelayController()

  "GET /" should "return OK" in {
    Get("/") ~> myRoute ~> check {
      status should equal(OK)
    }
  }
}