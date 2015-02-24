package brewcontrol

import akka.actor.Actor
import org.joda.time.format.DateTimeFormat
import spray.http.MediaTypes._
import spray.routing._

class WebActor(val temperatureReader: TemperatureReader) extends Actor with BrewHttpService {

  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

trait BrewHttpService extends HttpService {

  def temperatureReader: TemperatureReader

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            val dateFormatter = DateTimeFormat.mediumDateTime()
            val reading = temperatureReader.current()
            <html>
              <body>
                <h1>BrewControl</h1>
                <h3>Time</h3>
                <p>
                  {reading.timestamp.toString(dateFormatter)}
                </p>
                <h3>Temperatures</h3>{reading.values.map { case (sensorId, temp) => <p>
                {sensorId}
                :
                {temp}
                °C</p>
              }}
              </body>
            </html>
          }
        }
      }
    }
}
