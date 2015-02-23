package brewcontrol

import akka.actor.{Actor, Props}
import brewcontrol.TemperaturePersistActor._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import org.bson.types.ObjectId
import org.joda.time.DateTime

import scala.util.Try

class TemperaturePersistActor(temperatureConnection: TemperatureConnection, mongoConnection: MongoConnection, clock: Clock) extends Actor with akka.actor.ActorLogging {

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
    case Persist => {
      val now = clock.now
      val hour = now.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
      val minutes = now.getMinuteOfHour
      val seconds = now.getSecondOfMinute
      val x = temperatureConnection.sensorIds().flatMap(l => Try(l.map(id => id -> temperatureConnection.temperature(id).get).toMap))
      x.foreach(_.foreach { case (sensorId, temperature) => {
        val query = MongoDBObject("_id" -> documentIdCached(sensorId, hour))
        val update = $set(s"temp" -> temperature)
        collection.update(
          MongoDBObject("_id" -> documentIdCached(sensorId, hour)),
          $set(s"secondlyValues.${minutes}.${seconds}" -> temperature)
        )
      }
      })
    }
  }
}

object TemperaturePersistActor {
  def props(implicit connection: TemperatureConnection, mongoConnection: MongoConnection, clock: Clock) =
    Props(classOf[TemperaturePersistActor], connection, mongoConnection, clock)

  case object Persist

}
