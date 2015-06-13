package brewcontrol

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import rx._
import rx.core.Reactor
import rx.ops.{AkkaScheduler, _}
import spray.can.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AbstractBrewApp extends App with LazyLogging {

  implicit def temperatureConnection: TemperatureConnection
  implicit def mongoConnection: MongoConnection
  implicit def gpio: GpioConnection
  def host = "0.0.0.0"
  def port = 8080

  logger.info(s"Hi from BrewControl")
  sys.addShutdownHook(logger.info("Shutting down"))

  logger.info(s"Using MongoDB: ${mongoConnection.mongoClient.getAddress} / ${mongoConnection.db}")
  logger.debug(s"MongoDB details: ${mongoConnection.mongoClient.underlying}")

  implicit val system = akka.actor.ActorSystem()
  implicit val scheduler = system.scheduler
  implicit val rxScheduler = new AkkaScheduler(system)
  implicit val clock = new Clock
  implicit val config = new Config()

  val temperatureReader = new TemperatureReaderImpl()
  val temperatureStorage = new TemperatureStorage()

  var observers = List[Reactor[_]]()

  observers = startTemperaturePolling() :: observers

  val relayController = new RelayController()
  val relayStorage = new RelayStorage()
  observers = persistRelayStates().toList ::: observers

  val pidController = new PidController(config.targetTemperature, temperatureReader.Cooler.temperature, 10 seconds)

  // We use some tolerance to create a deadband in which neither relay is turned on
  // This is to avoid switching too quickly between heating and cooling
  val temperatureTolerance = 0.5f

  observers = pidController.output.map { output =>
    relayController.Heater.value() = output > temperatureTolerance
    relayController.Cooler.value() = output < -temperatureTolerance
  } :: observers

  observers = startPruneJob() :: observers

  startWebServer()
  logger.info("Startup complete")
  // No need to do anything else - the daemon threads are loose!

  def startTemperaturePolling(): Obs = {
    temperatureReader.currentReadings.foreach(
      _.foreach(reading => temperatureStorage.persist(reading.sensorId, reading.timestamp, reading.value))
    )
  }

  def persistRelayStates(): Seq[Obs] = {
    relayController.relays.map(r =>
      r.value.foreach(v => relayStorage.persist(r.name, clock.now.getMillis, if (v) 1f else 0f))
    )
  }

  // Repeatedly deletes all documents that are too old. The main reason is because the 32-Bit ARM version of
  // mongodb easily reaches its maximum capacity and crashes.
  def startPruneJob(): Obs = {
    val pruneInterval: FiniteDuration = 1 day

    Timer(pruneInterval).foreach { t =>
      val minTimestamp = clock.now.getMillis - pruneInterval.toMillis
      logger.debug(s"Checking if any documents expired before ${new DateTime(minTimestamp)}")
      temperatureStorage.deleteDocumentsOlderThan(minTimestamp)
      relayStorage.deleteDocumentsOlderThan(minTimestamp)
    }
  }

  def startWebServer() = {
    val webActorRef: ActorRef = system.actorOf(Props(classOf[WebActor], temperatureReader, temperatureStorage, relayController, relayStorage, config), "webActor")
    implicit val timeout = Timeout(5 seconds)
    IO(Http) ? Http.Bind(webActorRef, interface = host, port = port)
  }
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
  override lazy val mongoConnection = new MongoConnection
  override lazy val gpio = new GpioConnectionImpl()(system.scheduler, global)
}
