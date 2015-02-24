package brewcontrol

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import rx.ops.AkkaScheduler
import spray.can.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AbstractBrewApp extends App {

  implicit def temperatureConnection: TemperatureConnection

  implicit def mongoConnection: MongoConnection

  def host = "192.168.178.22"
  def port = 8080

  println(s"Hi from BrewControl")

  implicit val system = akka.actor.ActorSystem()
  implicit val scheduler = new AkkaScheduler(akka.actor.ActorSystem())
  implicit val clock = new Clock

  val temperatureReader = new TemperatureReader()

  val persistActorRef: ActorRef = system.actorOf(TemperaturePersistActor.props, "temperaturePersistActor")

  system.scheduler.schedule(10 seconds, 10 seconds, persistActorRef, TemperaturePersistActor.Persist)

  val webActorRef: ActorRef = system.actorOf(Props(classOf[WebActor], temperatureReader), "webActor")

  implicit val timeout = Timeout(5 seconds)

  IO(Http) ? Http.Bind(webActorRef, interface = host, port = port)

  println(s"Running...")
  system.awaitTermination()
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
  override lazy val mongoConnection = new MongoConnection
}
