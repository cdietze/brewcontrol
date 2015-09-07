package brewcontrol

import java.io.File

import akka.actor.{Props, ActorRef, Actor}
import com.typesafe.scalalogging.LazyLogging
import rx._
import spray.http.MediaTypes._
import spray.routing._
import upickle.default._

class WebActor(
                val temperatureReader: TemperatureManager,
                val relayController: RelayManager,
                val db: DB,
                val mashActor: ActorRef)
  extends Actor with TemperatureService with RelayService with HistoryService with ConfigService with MashService with StaticContentService {

  def actorRefFactory = context

  def receive = runRoute(temperaturesRoute ~ relayRoute ~ historyRoute ~ configRoute ~ mashRoute ~ staticContentRoute)
}

object WebActor {
  def props(temperatureReader: TemperatureManager, relayController: RelayManager, db: DB, mashActor: ActorRef): Props =
    Props(classOf[WebActor], temperatureReader, relayController, db, mashActor)
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

trait StaticContentService extends HttpService {

  val staticContentRoute: Route =
    pathSingleSlash {
      println(". is: " + new File(".").getAbsolutePath)
      getFromResource("ng/index.html")
    } ~
      getFromResourceDirectory("ng/")
}

trait ConfigService extends HttpService with LazyLogging {

  def db: DB

  val configRoute: Route =
    path("targetTemperature") {
      SprayUtils.modifiableVar(db.PropsDao.targetTemperature)
    } ~
      path("heaterEnabled") {
        SprayUtils.modifiableVar(db.PropsDao.heaterEnabled)
      } ~
      path("coolerEnabled") {
        SprayUtils.modifiableVar(db.PropsDao.coolerEnabled)
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

  def temperatureReader: TemperatureManager

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

  def relayController: RelayManager

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

trait MashService extends HttpService {
  def mashActor: ActorRef

  val mashRoute: Route = pathPrefix("mash") {
    complete {
      "TODO not implemented"
    }
  }
}