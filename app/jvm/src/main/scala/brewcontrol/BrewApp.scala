package brewcontrol

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import rx._
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
  implicit val scheduler = system.scheduler
  implicit val rxScheduler = new AkkaScheduler(system)
  implicit val clock = new Clock

  val temperatureReader = new TemperatureReaderImpl()
  val temperatureStorage = new TemperatureStorage()
  val obs1 = startTemperaturePolling()
  val relayController = new RelayController()

  val pidController = new PidController(Var(20f), temperatureReader.Cooler.temperature, 10 seconds)

  val obs2 = pidController.output.map { output =>
    relayController.Heater.value() = output > 0f
  }

  startWebServer()
  logger.info("Startup complete")
  // No need to do anything else - the daemon threads are loose!

  def startTemperaturePolling(): Obs = {
    temperatureReader.currentReadings.foreach(
      _.foreach(reading => temperatureStorage.persist(reading.sensorId, reading.timestamp, reading.value))
    )
  }

  def startWebServer() = {
    val webActorRef: ActorRef = system.actorOf(Props(classOf[WebActor], temperatureReader, temperatureStorage, relayController), "webActor")
    implicit val timeout = Timeout(5 seconds)
    IO(Http) ? Http.Bind(webActorRef, interface = host, port = port)
  }
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
  override lazy val mongoConnection = new MongoConnection
  override lazy val gpio = new GpioConnectionImpl()(system.scheduler, global)
}
