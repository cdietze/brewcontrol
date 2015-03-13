package brewcontrol

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import spray.http.{HttpEntity, MediaTypes}
import spray.routing._
import upickle.Js

import scalatags.Text.all._

class WebActor(val temperatureReader: TemperatureReader, val temperatureStorage: TemperatureStorage, val relayController: RelayController) extends Actor with BrewHttpService with TemperatureService {

  def actorRefFactory = context

  def receive = runRoute(temperaturesRoute ~ staticContentRoute)
}

trait BrewHttpService extends HttpService {

  def temperatureReader: TemperatureReader

  def relayController: RelayController

  val staticContentRoute: Route =
    pathSingleSlash {
      get {
        complete {
          HttpEntity(
            MediaTypes.`text/html`,
            Page.content.render
          )
        }
      }
    } ~ getFromResourceDirectory("")
}

object Page {
  val content =
    html(
      head(
        script(src := "/app-fastopt.js")
      ),
      body(
        onload := "brewcontrol.Client().main(document.getElementById('contents'))",
        div(id := "contents")
      )
    )
}

trait TemperatureService extends HttpService with LazyLogging {

  def temperatureReader: TemperatureReader

  def temperatureStorage: TemperatureStorage

  val temperaturesRoute: Route =
    pathPrefix("temperatures") {
      pathEnd {
        get {
          complete {
            upickle.write(temperatureReader.currentReadings.now)
          }
        }
      } ~
        path(Segment) { sensorId =>
          complete {
            val o = temperatureReader.currentReading(sensorId).get
            upickle.write(o)
          }
        }
    }
}
