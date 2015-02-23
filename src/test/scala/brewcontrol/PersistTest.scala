package brewcontrol

import akka.testkit.TestActorRef
import org.joda.time.DateTime
import utest._
import utest.framework.TestSuite

object PersistTest extends TestSuite {
  val tests = TestSuite {
    'TemperaturePersist{
      implicit val system = akka.actor.ActorSystem()
      implicit val clock = new Clock {
        override def now = mockNow

        var mockNow = new DateTime(0)
      }
      implicit val mongoConnection = new MockMongoConnection
      implicit val temperatureConnection = new MockTemperatureConnection
      val persistActor = TestActorRef(new TemperaturePersistActor(temperatureConnection, mongoConnection, clock))

      var docCount = persistActor.underlyingActor.collection.count()
      assert(docCount == 0)

      persistActor ! TemperaturePersistActor.Persist
      docCount = persistActor.underlyingActor.collection.count()
      assert(docCount == 1)

      persistActor ! TemperaturePersistActor.Persist
      docCount = persistActor.underlyingActor.collection.count()
      assert(docCount == 1)

      clock.mockNow = clock.mockNow.plusMinutes(1).plusSeconds(1)
      persistActor ! TemperaturePersistActor.Persist
      assert(persistActor.underlyingActor.collection.count() == 1)

      clock.mockNow = clock.mockNow.plusHours(1)

      persistActor ! TemperaturePersistActor.Persist
      assert(persistActor.underlyingActor.collection.count() == 2)
    }
  }
}
