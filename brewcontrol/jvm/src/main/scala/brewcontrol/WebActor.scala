package brewcontrol

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import rx._
import spray.http.MediaTypes._
import spray.http.{HttpEntity, MediaTypes}
import spray.routing._

import scalatags.Text.all._

class WebActor(
                val temperatureReader: TemperatureReader,
                val temperatureStorage: TemperatureStorage,
                val relayController: RelayController,
                val relayStorage: RelayStorage,
                val config: Config)
  extends Actor with BrewHttpService with TemperatureService with RelayService with HistoryService with ConfigService {

  def actorRefFactory = context

  def receive = runRoute(temperaturesRoute ~ relayRoute ~ historyRoute ~ staticContentRoute ~ configRoute)
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

trait ConfigService extends HttpService with LazyLogging {

  def config: Config

  val configRoute: Route =
    path("targetTemperature") {
      doubleVarWithGetAndPost(config.targetTemperature)
    } ~
      path("heaterEnabled") {
        booleanVarWithGetAndPost(config.heaterEnabled)
      } ~
      path("coolerEnabled") {
        booleanVarWithGetAndPost(config.coolerEnabled)
      }

  def doubleVarWithGetAndPost(rx: Var[Double]) = {
    get {
      complete {
        upickle.write(rx())
      }
    } ~
      post {
        entity(as[String]) { valueString =>
          val value = upickle.read[Double](valueString)
          rx() = value
          complete(s"Updated value to $value")
        }
      }
  }
  def booleanVarWithGetAndPost(rx: Var[Boolean]) = {
    get {
      complete {
        upickle.write(rx())
      }
    } ~
      post {
        entity(as[String]) { valueString =>
          val value = upickle.read[Boolean](valueString)
          rx() = value
          complete(s"Updated value to $value")
        }
      }
  }
}

trait HistoryService extends HttpService with LazyLogging {
  def temperatureStorage: TemperatureStorage
  def temperatureReader: TemperatureReader
  def relayStorage: RelayStorage

  val historyRoute: Route =
    pathPrefix("history") {
      pathPrefix("hour") {
        path(LongNumber) { hourTimestamp =>
          respondWithMediaType(`application/json`) {
            complete {
              val temperatures = temperatureStorage.getDocumentsByHour(hourTimestamp).toSeq.map(e => SeriesData(temperatureReader.sensorName(e._1), Temperature, e._2))
              val relays = relayStorage.getDocumentsByHour(hourTimestamp).toSeq.map(e => SeriesData(e._1, Relay, e._2))
              upickle.write(temperatures ++ relays)
            }
          }
        }
      }
    }
}

trait TemperatureService extends HttpService with LazyLogging {

  def temperatureReader: TemperatureReader

  val temperaturesRoute: Route =
    pathPrefix("temperatures") {
      pathEnd {
        get {
          complete {
            upickle.write(temperatureReader.currentReadings.now)
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
