package brewcontrol

import akka.testkit.TestActorRef
import brewcontrol.TemperatureReader.Reading
import org.joda.time.DateTime
import utest._
import utest.framework.TestSuite

import scala.concurrent.ExecutionContext.Implicits.global

object PersistTest extends TestSuite {
  val tests = TestSuite {
    'TemperaturePersist {
      implicit val system = akka.actor.ActorSystem()
      implicit val mongoConnection = new MockMongoConnection
      implicit val temperatureConnection = new MockTemperatureConnection

      val persistActor = TestActorRef(new TemperaturePersistActor(mongoConnection))

      var docCount = persistActor.underlyingActor.collection.count()
      assert(docCount == 0)

      var reading = Reading(new DateTime(0), Map("SensorA" -> 24.5f))

      persistActor ! TemperaturePersistActor.Persist(reading)
      docCount = persistActor.underlyingActor.collection.count()
      assert(docCount == 1)

      persistActor ! TemperaturePersistActor.Persist(reading)
      docCount = persistActor.underlyingActor.collection.count()
      assert(docCount == 1)

      // After 1 minute there is still 1 document
      reading = reading.copy(timestamp = reading.timestamp.plusMinutes(1))
      persistActor ! TemperaturePersistActor.Persist(reading)
      docCount = persistActor.underlyingActor.collection.count()
      assert(docCount == 1)

      // After 1 hour, there are 2 documents
      reading = reading.copy(timestamp = reading.timestamp.plusHours(1))
      persistActor ! TemperaturePersistActor.Persist(reading)
      docCount = persistActor.underlyingActor.collection.count()
      assert(docCount == 2)
    }
  }
}
