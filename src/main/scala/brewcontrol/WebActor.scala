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
            val reading = temperatureReader.currentReading()
            <html>
              <body>
                <h1>BrewControl</h1>
                <p>Current time is
                  {DateTime.now.toString(dateFormatter)}
                </p>
                <h3>Temperatures</h3>
                <p>Reading from
                  {reading.timestamp.toString(dateFormatter)}
                </p>{reading.values.map { case (sensorId, temp) => <p>
                {temperatureReader.sensorName(sensorId)}
                :
                {temp}
                °C</p>
              }}<h3>Relays</h3>{relayController.relayMap.map { case (name, relay) => <p>
                {name}
                :
                {relay.value.now}
              </p>
              }}
              </body>
            </html>
          }
        }
      }
    }
}
