package brewcontrol

import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App {

  println(s"Hi from ${this.getClass}")

  val system = akka.actor.ActorSystem()

  val persistActor: ActorRef = system.actorOf(TemperaturePersistActor.props(new TemperatureConnection))

  system.scheduler.schedule(5 seconds, 5 seconds, persistActor, TemperaturePersistActor.Persist)

  println(s"Waiting until termination...")
  Thread.sleep(15000)
  system.shutdown()
}




