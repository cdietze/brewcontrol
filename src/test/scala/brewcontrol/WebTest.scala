package brewcontrol

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest

class WebTest extends FlatSpec with Matchers with ScalatestRouteTest with BrewHttpService with TemperatureService with LazyLogging {
  def actorRefFactory = system

  implicit val mongoConnection = new MockMongoConnection
  implicit val gpio = new MockGpioConnection
  implicit val temperatureStorage = new TemperatureStorage

  lazy val temperatureReader: TemperatureReader = new MockTemperatureReader()
  lazy val relayController: RelayController = new RelayController()

  "GET /" should "return OK" in {
    Get("/") ~> indexHtmlRoute ~> check {
      status should equal(OK)
    }
  }

  "/temperatures route" should "for path / return OK" in {
    Get("/temperatures") ~> temperaturesRoute ~> check {
      status should equal(OK)
    }
  }
  it should "for path /sensorId return OK" in {
    Get("/temperatures/XYZ") ~> temperaturesRoute ~> check {
      logger.info(s"response: ${response.entity}")
      status should equal(OK)
    }
  }
}