package brewcontrol

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import rx._
import spray.http.MediaTypes._
import spray.http.{HttpEntity, MediaTypes}
import spray.routing._

import scalatags.Text.all._

import upickle.default._

class WebActor(
                val temperatureReader: TemperatureReader,
                val relayController: RelayController,
                val config: Config)
  extends Actor with BrewHttpService with TemperatureService with RelayService with HistoryService with ConfigService {

  def actorRefFactory = context

  def receive = runRoute(temperaturesRoute ~ relayRoute ~ historyRoute ~ staticContentRoute ~ configRoute)
}

object SprayUtils {

  import Directives._

  def modifiableVar[T](rx: Var[T])(implicit rw: ReadWriter[T]) = {
    get {
      complete {
        write(rx())
      }
    } ~ post {
      entity(as[String]) { valueString =>
        val value = read[T](valueString)
        rx() = value
        complete(s"Updated value to $value")
      }
    }
  }
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
      SprayUtils.modifiableVar(config.targetTemperature)
    } ~
      path("heaterEnabled") {
        SprayUtils.modifiableVar(config.heaterEnabled)
      } ~
      path("coolerEnabled") {
        SprayUtils.modifiableVar(config.coolerEnabled)
      }
}

trait HistoryService extends HttpService with LazyLogging {
  def temperatureReader: TemperatureReader

  val historyRoute: Route =
    pathPrefix("history") {
      respondWithMediaType(`application/json`) {
        complete {
          write(History.get())
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
            write(temperatureReader.currentReadings.now)
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
            write(relayController.relays.map(r => RelayState(r.name, r.value.now)))
          }
        }
      }
    }
}
