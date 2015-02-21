package brewcontrol

import akka.actor.{Actor, Props}

import scala.util.Try

class TemperaturePersistActor(connection: TemperatureConnection) extends Actor with akka.actor.ActorLogging {
  import brewcontrol.TemperaturePersistActor._

  override def receive = {
    case Persist => {
      val x = connection.sensorIds().flatMap(l => Try(l.map(id => id -> connection.temperature(id).get).toMap))
      log.info(s"Temperatures: ${x}")
    }
  }
}

object TemperaturePersistActor {
  def props(connection: TemperatureConnection) = Props(classOf[TemperaturePersistActor], connection)

  case object Persist

}
