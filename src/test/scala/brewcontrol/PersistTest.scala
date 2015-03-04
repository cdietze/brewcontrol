package brewcontrol

import brewcontrol.TemperatureReader.Reading
import org.joda.time.DateTime
import utest._
import utest.framework.TestSuite

object PersistTest extends TestSuite {
  val tests = TestSuite {
    'TemperaturePersist {
      implicit val system = akka.actor.ActorSystem()
      implicit val mongoConnection = new MockMongoConnection
      implicit val temperatureConnection = new MockTemperatureConnection

      val temperatureStorage = new TemperatureStorage(mongoConnection)

      var docCount = temperatureStorage.collection.count()
      assert(docCount == 0)

      var reading = Reading(new DateTime(0), Map("SensorA" -> 24.5f))

      temperatureStorage.persist(reading)
      docCount = temperatureStorage.collection.count()
      assert(docCount == 1)

      temperatureStorage.persist(reading)
      docCount = temperatureStorage.collection.count()
      assert(docCount == 1)

      // After 1 minute there is still 1 document
      reading = reading.copy(timestamp = reading.timestamp.plusMinutes(1))
      temperatureStorage.persist(reading)
      docCount = temperatureStorage.collection.count()
      assert(docCount == 1)

      // After 1 hour, there are 2 documents
      reading = reading.copy(timestamp = reading.timestamp.plusHours(1))
      temperatureStorage.persist(reading)
      docCount = temperatureStorage.collection.count()
      assert(docCount == 2)
    }
  }
}
