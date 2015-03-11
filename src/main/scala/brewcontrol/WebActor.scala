package brewcontrol

import java.util.Locale

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing._

class WebActor(val temperatureReader: TemperatureReader, val relayController: RelayController) extends Actor with BrewHttpService with TemperatureService {

  def actorRefFactory = context

  def receive = runRoute(indexHtmlRoute ~ temperaturesRoute)
}

trait BrewHttpService extends HttpService {

  def temperatureReader: TemperatureReader

  def relayController: RelayController

  val indexHtmlRoute: Route =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            val dateFormatter = DateTimeFormat.mediumDateTime().withLocale(Locale.GERMANY)
            val readings = temperatureReader.currentReadings()
            <html>
              <body>
                <h1>BrewControl</h1>
                <p>Current time is
                  {DateTime.now.toString(dateFormatter)}
                </p>
                <h3>Temperatures</h3>{readings.map(reading => <p>
                {temperatureReader.sensorName(reading.sensorId)}
                :
                {reading.value}
                °C</p>)}<h3>Relays</h3>{relayController.relayMap.map { case (name, relay) => <p>
                {name}
                :
                {if (relay.value.now) "on" else "off"}
              </p>
              }}
              </body>
            </html>
          }
        }
      }
    }
}

import spray.httpx.marshalling._

trait TemperatureService extends HttpService with SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  def temperatureReader: TemperatureReader

  val temperaturesRoute: Route =
    pathPrefix("temperatures") {
      pathEnd {
        get {
          complete {
            marshal(temperatureReader.currentReadings.now.map(_.sensorId))
          }
        }
      } ~
        path(Segment) { sensorId =>
          complete {

            marshal(Map("sensorId" ->sensorId))
          }
        }
    }
}