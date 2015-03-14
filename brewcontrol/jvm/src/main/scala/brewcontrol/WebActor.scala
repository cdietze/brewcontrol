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
        script(src := "/brewcontrol-fastopt.js")
      ),
      body(
        onload := "brewcontrol.Client().main(document.getElementById('contents'))",
        div(id := "contents")
      )
    )
}

trait TemperatureService extends HttpService with LazyLogging {

  import TimeSeriesStorage._

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
              val o = temperatureReader.currentReading(sensorId).get
              upickle.write(o)
            }
          } ~
            path("hour") {
              respondWithMediaType(`application/json`) {
                complete {
                  val doc : HourTimeData = temperatureStorage.getLatestDocument(sensorId)
                  upickle.write(doc)
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
