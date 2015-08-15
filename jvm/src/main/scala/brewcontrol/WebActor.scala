package brewcontrol

import java.io.File

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import rx._
import spray.http.MediaTypes._
import spray.routing._
import upickle.default._

class WebActor(
                val temperatureReader: TemperatureReader,
                val relayController: RelayController,
                val config: Config)
  extends Actor with BrewHttpService with TemperatureService with RelayService with HistoryService with ConfigService {

  def actorRefFactory = context

  def receive = runRoute(temperaturesRoute ~ relayRoute ~ historyRoute ~ configRoute ~ staticContentRoute)
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

  val staticContentRoute: Route =
    pathSingleSlash {
      println(". is: " + new File(".").getAbsolutePath)
      getFromResource("ng/index.html")
    } ~
      getFromResourceDirectory("ng/")
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