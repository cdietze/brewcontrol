package brewcontrol

import akka.testkit.TestActorRef
import utest.framework.TestSuite

import scala.util.Try

object PersistTest extends TestSuite {
  val tests = TestSuite {

    implicit val system = akka.actor.ActorSystem()
    val persistActor = TestActorRef(new TemperaturePersistActor(MockTemperatureConnection))

    persistActor ! TemperaturePersistActor.Persist

  }
}

object MockTemperatureConnection extends TemperatureConnection {
  override def sensorIds() = Try(Set("SensorA", "SensorB"))

  override def temperature(sensorId: String) = Try(24.5f)
}