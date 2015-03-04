package brewcontrol

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import brewcontrol.TemperaturePersistActor.Persist
import rx.core.Obs
import rx.ops.{AkkaScheduler, _}
import spray.can.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AbstractBrewApp extends App {

  implicit def temperatureConnection: TemperatureConnection

  implicit def mongoConnection: MongoConnection

  implicit def gpio: GpioConnection

  def host = "192.168.178.22"

  def port = 8080

  println(s"Hi from BrewControl")

  implicit val system = akka.actor.ActorSystem()
  implicit val scheduler = new AkkaScheduler(akka.actor.ActorSystem())
  implicit val clock = new Clock

  val temperatureReader = new TemperatureReader()
  val obs1 = startTemperaturePolling()
  val obs2 = startPinDemo()
  startWebServer()

  println(s"Running...")
  system.awaitTermination()

  def startTemperaturePolling(): Obs = {
    val persistActorRef: ActorRef = system.actorOf(TemperaturePersistActor.props, "temperaturePersistActor")
    temperatureReader.current.foreach(reading => persistActorRef ! Persist(reading))
  }

  def startPinDemo(): Obs = {
    val pin = gpio.outPin(2)
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
