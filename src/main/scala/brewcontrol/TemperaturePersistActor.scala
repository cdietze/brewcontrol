package brewcontrol

import akka.actor.{Actor, Props}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import org.bson.types.ObjectId
import org.joda.time.DateTime

class TemperaturePersistActor(mongoConnection: MongoConnection) extends Actor with akka.actor.ActorLogging {

  import brewcontrol.TemperaturePersistActor._

  RegisterJodaTimeConversionHelpers()

  lazy val collection = mongoConnection.db.getCollection("temperature")

  var documentIdCache = Map[(String, DateTime), ObjectId]()

  def documentIdCached(sensorId: String, hour: DateTime): ObjectId = {
    val docId: ObjectId = documentIdCache.get((sensorId, hour)).getOrElse(
      documentId(sensorId, hour)
    )
    documentIdCache = documentIdCache.updated((sensorId, hour), docId)
    docId
  }

  def documentId(sensorId: String, hour: DateTime): ObjectId = {
    val o = MongoDBObject(
      "sensorId" -> sensorId,
      "timeStampHour" -> hour
    )
    Option(collection.findOne(o)) match {
      case Some(result) => result("_id").asInstanceOf[ObjectId]
      case None => collection.insert(o); o("_id").asInstanceOf[ObjectId]
    }
  }

  override def receive = {
    case Persist(reading) => {
      log.info(s"persisting $reading")
      val ts = reading.timestamp
      val hour = ts.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
      val minutes = ts.getMinuteOfHour
      val seconds = ts.getSecondOfMinute
      reading.values.foreach { case (sensorId, temperature) => {
        val query = MongoDBObject("_id" -> documentIdCached(sensorId, hour))
        val update = $set(s"temp" -> temperature)
        collection.update(
          MongoDBObject("_id" -> documentIdCached(sensorId, hour)),
          $set(s"secondlyValues.${minutes}.${seconds}" -> temperature)
        )
      }
      }
    }
    case m => sys.error(s"Unknown message received: $m")
  }
}

object TemperaturePersistActor {
  def props(implicit mongoConnection: MongoConnection) =
    Props(classOf[TemperaturePersistActor], mongoConnection)

  case class Persist(reading: TemperatureReader.Reading)

}
