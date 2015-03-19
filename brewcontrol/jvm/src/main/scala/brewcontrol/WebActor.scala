package brewcontrol

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import spray.http.MediaTypes._
import spray.http.{HttpEntity, MediaTypes}
import spray.routing._

import scalatags.Text.all._

class WebActor(
                val temperatureReader: TemperatureReader,
                val temperatureStorage: TemperatureStorage,
                val relayController: RelayController)
  extends Actor with BrewHttpService with TemperatureService with RelayService {

  def actorRefFactory = context

  def receive = runRoute(temperaturesRoute ~ relayRoute ~ staticContentRoute)
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
        script(src := "https://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.3/jquery.min.js"),
        script(src := "https://cdnjs.cloudflare.com/ajax/libs/flot/0.8.3/jquery.flot.min.js"),
        script(src := "https://cdnjs.cloudflare.com/ajax/libs/flot/0.8.3/jquery.flot.time.js"),
        script(src := "/brewcontrol-fastopt.js")
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
        pathPrefix(Segment) { sensorId =>
          pathEnd {
            complete {
              temperatureReader.currentReading(sensorId).map(upickle.write(_)).toOption
            }
          } ~
            pathPrefix("hour") {
              pathEnd {
                respondWithMediaType(`application/json`) {
                  complete {
                    temperatureStorage.getLatestDocument(sensorId).map(upickle.write(_))
                  }
                }
              } ~
                path(LongNumber) { hourTimestamp =>
                  respondWithMediaType(`application/json`) {
                    complete {
                      temperatureStorage.getDocument(sensorId, hourTimestamp).map(upickle.write(_))
                    }
                  }
                }
            }
        }
    }
}

trait RelayService extends HttpService with LazyLogging {

  def relayController: RelayController

  val relayRoute: Route =
    pathPrefix("relays") {
      pathEnd {
        get {
          complete {
            upickle.write(relayController.relays.map(r => RelayState(r.name, r.value.now)))
          }
        }
      }
    }
}
