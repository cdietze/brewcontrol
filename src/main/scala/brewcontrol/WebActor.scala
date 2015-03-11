package brewcontrol

import java.util.Locale

import akka.actor.Actor
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spray.http.MediaTypes._
import spray.routing._

class WebActor(val temperatureReader: TemperatureReader, val relayController: RelayController) extends Actor with BrewHttpService {

  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

trait BrewHttpService extends HttpService {

  def temperatureReader: TemperatureReader

  def relayController: RelayController

  val myRoute =
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
