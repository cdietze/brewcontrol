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
  implicit def gpio: Gpio
  def host = "0.0.0.0"
  def port = 8080
  def jdbcUrl = "jdbc:sqlite:/mnt/lfs/brewcontrol/data.sqlite"

  logger.info(s"Hi from BrewControl")
  sys.addShutdownHook(logger.info("Shutting down"))

  Class.forName("org.sqlite.JDBC")
  logger.info(s"Using Database $jdbcUrl")
  val database = Database.forURL(jdbcUrl)
  implicit val db = new DB(database)
  db.init()

  implicit val system = akka.actor.ActorSystem()
  implicit val scheduler = system.scheduler
  implicit val rxScheduler = new AkkaScheduler(system)
  implicit val clock = new Clock

  val temperatureReader = new TemperatureManagerImpl()

  var observers = List[Reactor[_]]()

  observers = persistTemperatureReadings() :: observers

  val relayController = new RelayManager()
  observers = persistRelayStates().toList ::: observers

  val pidController = new PidController(db.PropsDao.targetTemperature, temperatureReader.Cooler.temperature, 10 seconds)

  // We use some tolerance to create a deadband in which neither relay is turned on
  // This is to avoid switching too quickly between heating and cooling
  val temperatureTolerance = 0.5f

  observers = pidController.output.map { output =>
    relayController.Cooler.value() = db.PropsDao.coolerEnabled() && output < -temperatureTolerance
    relayController.Heater.value() = db.PropsDao.heaterEnabled() && output > temperatureTolerance
  } :: observers

  val recipe: Recipe = Recipe(List(
    HeatStep(61d),
    HoldStep,
    RestStep((10 minutes).toMillis),
    HeatStep(62d),
    RestStep((45 minutes).toMillis),
    HeatStep(71d),
    RestStep((30 minutes).toMillis),
    HeatStep(78d),
    HoldStep
  ))
  val mashControlActor = system.actorOf(MashControlActor.props(recipe, clock, relayController.PotHeater.value, temperatureReader.Pot.temperature))

  startWebServer()
  logger.info("Startup complete")
  // No need to do anything else - the daemon threads are loose!

  def persistTemperatureReadings(): Obs = {
    temperatureReader.currentReadings.foreach(
      _.foreach(reading => History.addItem(reading.name, "double", Item(reading.timestamp, reading.value)))
    )
  }

  def persistRelayStates(): Seq[Obs] = {
    relayController.relays.map(r =>
      r.value.foreach(v => History.addItem(r.name, "binary", Item(clock.now.toEpochMilli, if (v) 1 else 0)))
    )
  }

  def startWebServer() = {
    val webActorRef: ActorRef = system.actorOf(WebActor.props(temperatureReader, relayController, db, mashControlActor), "webActor")
    implicit val timeout = Timeout(5 seconds)
    IO(Http) ? Http.Bind(webActorRef, interface = host, port = port)
  }
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
  override lazy val gpio = new GpioImpl()(system.scheduler, global)
}
