package brewcontrol

import akka.actor.Actor
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
            <html>
              <body>
                <h1>Say hello to
                  <i>spray-routing</i>
                  on
                  <i>spray-can</i>
                  !</h1>

                <h2>Temperatures:</h2>
                <p>
                  {temperatureReader.current()}
                </p>
              </body>
            </html>
          }
        }
      }
    }
}
