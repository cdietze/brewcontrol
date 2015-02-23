package brewcontrol

import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AbstractBrewApp extends App {

  implicit def temperatureConnection: TemperatureConnection

  implicit def mongoConnection: MongoConnection

  println(s"Hi from BrewControl")

  val system = akka.actor.ActorSystem()
  implicit val clock = new Clock

  val persistActor: ActorRef = system.actorOf(TemperaturePersistActor.props)

  system.scheduler.schedule(10 seconds, 10 seconds, persistActor, TemperaturePersistActor.Persist)

  println(s"Running...")
  system.awaitTermination()
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
  override lazy val mongoConnection = new MongoConnection
}
