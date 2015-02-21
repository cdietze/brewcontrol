package brewcontrol

import akka.testkit.TestActorRef
import utest.framework.TestSuite

object PersistTest extends TestSuite {
  val tests = TestSuite {

    implicit val system = akka.actor.ActorSystem()
    val persistActor = TestActorRef(new TemperaturePersistActor(MockTemperatureConnection))

    persistActor ! TemperaturePersistActor.Persist

  }
}