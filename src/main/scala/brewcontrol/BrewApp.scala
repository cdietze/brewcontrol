package brewcontrol

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AbstractBrewApp extends App {

  implicit def temperatureConnection: TemperatureConnection

  implicit def mongoConnection: MongoConnection

  println(s"Hi from BrewControl")

  implicit val system = akka.actor.ActorSystem()
  implicit val clock = new Clock

  val persistActorRef: ActorRef = system.actorOf(TemperaturePersistActor.props, "temperaturePersistActor")

  system.scheduler.schedule(10 seconds, 10 seconds, persistActorRef, TemperaturePersistActor.Persist)

  val webActorRef: ActorRef = system.actorOf(Props[WebActor], "webActor")

  implicit val timeout = Timeout(5 seconds)

  IO(Http) ? Http.Bind(webActorRef, interface = "192.168.178.22", port = 8080)

  println(s"Running...")
  system.awaitTermination()
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
  override lazy val mongoConnection = new MongoConnection
}
