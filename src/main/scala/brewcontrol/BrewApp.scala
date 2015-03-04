package brewcontrol

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import rx.core.Obs
import rx.ops.{AkkaScheduler, _}
import spray.can.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AbstractBrewApp extends App with LazyLogging {

  implicit def temperatureConnection: TemperatureConnection
  implicit def mongoConnection: MongoConnection
  implicit def gpio: GpioConnection
  def host = "192.168.178.22"
  def port = 8080

  logger.info(s"Hi from BrewControl")
  sys.addShutdownHook(logger.info("Shutting down"))

  logger.info(s"Using MongoDB: ${mongoConnection.mongoClient.getAddress} / ${mongoConnection.db}")
  logger.debug(s"MongoDB details: ${mongoConnection.mongoClient.underlying}")

  implicit val system = akka.actor.ActorSystem()
  implicit val scheduler = new AkkaScheduler(akka.actor.ActorSystem())
  implicit val clock = new Clock

  val temperatureReader = new TemperatureReader()
  val obs1 = startTemperaturePolling()
  val obs2 = startPinDemo()
  startWebServer()

  logger.info("Startup complete")
  // No need to do anything else - the daemon threads are loose!

  def startTemperaturePolling(): Obs = {
    val temperatureStorage = new TemperatureStorage(mongoConnection)
    temperatureReader.current.foreach(reading => temperatureStorage.persist(reading))
  }

  def startPinDemo(): Obs = {
    val pin = gpio.outPin(2)
    sys.addShutdownHook(gpio.unexport(2))
    val timer = Timer(10 seconds)
    timer.foreach { t => {
      pin.update(t % 2 == 0)
    }
    }
  }

  def startWebServer() = {
    val webActorRef: ActorRef = system.actorOf(Props(classOf[WebActor], temperatureReader), "webActor")
    implicit val timeout = Timeout(5 seconds)
    IO(Http) ? Http.Bind(webActorRef, interface = host, port = port)
  }
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
  override lazy val mongoConnection = new MongoConnection
  override lazy val gpio = new GpioConnection
}
