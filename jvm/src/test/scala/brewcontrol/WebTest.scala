package brewcontrol

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest

class WebTest extends FlatSpec with Matchers with ScalatestRouteTest with BrewHttpService with TemperatureService with LazyLogging {
  def actorRefFactory = system

  implicit val gpio = new MockGpioConnection

  lazy val temperatureReader = new MockTemperatureReader()
  lazy val relayController: RelayController = new RelayController()

  "GET /" should "return OK" in {
    Get("/") ~> staticContentRoute ~> check {
      status should equal(OK)
    }
  }

  "/temperatures route" should "for path / return OK" in {
    Get("/temperatures") ~> temperaturesRoute ~> check {
      status should equal(OK)
    }
  }
  "/temperatures hour route" should "return OK" in {
//    var reading = Reading(new DateTime(0), temperatureReader.mockSensorId, "Sensor name", 24.5f)
//    temperatureStorage.persist(reading)
//    Get("/temperatures/${temperatureReader.mockSensorId}/hour") ~> temperaturesRoute ~> check {
//      status should equal(OK)
//    }
  }
}