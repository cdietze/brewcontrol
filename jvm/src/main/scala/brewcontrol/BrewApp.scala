package brewcontrol

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import brewcontrol.History.Item
import com.typesafe.scalalogging.LazyLogging
import rx._
import rx.core.Reactor
import rx.ops.{AkkaScheduler, _}
import slick.jdbc.JdbcBackend.Database
import spray.can.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AbstractBrewApp extends App with LazyLogging {

  implicit def temperatureConnection: TemperatureConnection
  implicit def gpio: GpioConnection
  def host = "0.0.0.0"
  def port = 8080

  logger.info(s"Hi from BrewControl")
  sys.addShutdownHook(logger.info("Shutting down"))

  Class.forName("org.sqlite.JDBC")
  val database = Database.forURL("jdbc:sqlite:data.sqlite")
  implicit val db = new DB(database)
  db.init()

  implicit val system = akka.actor.ActorSystem()
  implicit val scheduler = system.scheduler
  implicit val rxScheduler = new AkkaScheduler(system)
  implicit val clock = new Clock
  implicit val config = new Config()

  val temperatureReader = new TemperatureReaderImpl()

  var observers = List[Reactor[_]]()

  observers = persistTemperatureReadings() :: observers

  val relayController = new RelayController()
  observers = persistRelayStates().toList ::: observers

  val pidController = new PidController(config.targetTemperature, temperatureReader.Cooler.temperature, 10 seconds)

  // We use some tolerance to create a deadband in which neither relay is turned on
  // This is to avoid switching too quickly between heating and cooling
  val temperatureTolerance = 0.5f

  observers = pidController.output.map { output =>
    relayController.Cooler.value() = config.coolerEnabled() && output < -temperatureTolerance
    relayController.Heater.value() = config.heaterEnabled() && output > temperatureTolerance
  } :: observers

  startWebServer()
  logger.info("Startup complete")
  // No need to do anything else - the daemon threads are loose!

  def persistTemperatureReadings(): Obs = {
    temperatureReader.currentReadings.foreach(
      _.foreach(reading => History.addItem(reading.sensorId, "double", Item(reading.timestamp, reading.value)))
    )
  }

  def persistRelayStates(): Seq[Obs] = {
    relayController.relays.map(r =>
      r.value.foreach(v => History.addItem(r.name, "binary", Item(clock.now.getMillis, if (v) 1 else 0)))
    )
  }

  def startWebServer() = {
    val webActorRef: ActorRef = system.actorOf(Props(classOf[WebActor], temperatureReader, relayController, config), "webActor")
    implicit val timeout = Timeout(5 seconds)
    IO(Http) ? Http.Bind(webActorRef, interface = host, port = port)
  }
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
  override lazy val gpio = new GpioConnectionImpl()(system.scheduler, global)
}
