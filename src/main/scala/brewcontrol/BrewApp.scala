package brewcontrol

import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AbstractBrewApp extends App {

  val temperatureConnection: TemperatureConnection

  println(s"Hi from ${this.getClass}")
  println(s"temperatureConnection: $temperatureConnection")

  val system = akka.actor.ActorSystem()

  val persistActor: ActorRef = system.actorOf(TemperaturePersistActor.props(temperatureConnection))

  system.scheduler.schedule(5 seconds, 5 seconds, persistActor, TemperaturePersistActor.Persist)

  println(s"Waiting until termination...")
  system.awaitTermination()
}

object BrewApp extends AbstractBrewApp {
  override lazy val temperatureConnection = new TemperatureConnection
}
