package brewcontrol

import brewcontrol.TemperatureReader.Reading
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import com.typesafe.scalalogging.LazyLogging
import org.bson.types.ObjectId
import org.joda.time.DateTime

/**
 * Stores temperature values. Document structure:
 * <pre>
 {
	"_id" : ObjectId("54eacff344ae2d97d8fca4cd"),
	"sensorId" : "28-011463e799ff",
	"timeStampHour" : ISODate("2015-02-23T07:00:00Z"),
	"secondlyValues" : {
		"58" : {
			"0" : 22.625,
			"10" : 22.625,
			"15" : 22.625
		},
		"59" : {
			"50" : 22.437000274658203,
			"55" : 22.437000274658203
		}
	}
}
</pre>
 */
class TemperatureStorage(mongoConnection: MongoConnection) extends LazyLogging {

  RegisterJodaTimeConversionHelpers()

  lazy val collection = mongoConnection.db.getCollection("temperatures")

  /** A cache containing the most recently used documentId per sensorId */
  private var documentIdCache = Map[String, (DateTime, ObjectId)]()

  private def documentIdCached(sensorId: String, hour: DateTime): ObjectId = {
    documentIdCache.get(sensorId).filter(_._1 == hour) match {
      case Some((h, docId)) => docId
      case None => {
        val docId = documentId(sensorId, hour)
        documentIdCache = documentIdCache.updated(sensorId, (hour, docId))
        docId
      }
    }
  }

  private def documentId(sensorId: String, hour: DateTime): ObjectId = {
    val o = MongoDBObject(
      "sensorId" -> sensorId,
      "timeStampHour" -> hour
    )
    Option(collection.findOne(o)) match {
      case Some(result) => result("_id").asInstanceOf[ObjectId]
      case None => collection.insert(o); o("_id").asInstanceOf[ObjectId]
    }
  }

  def persist(reading: Reading) {
    logger.debug(s"Persisting $reading")
    val ts = reading.timestamp
    val hour = ts.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
    val minutes = ts.getMinuteOfHour
    val seconds = ts.getSecondOfMinute
    reading.values.foreach {
      case (sensorId, temperature) => {
        val query = MongoDBObject("_id" -> documentIdCached(sensorId, hour))
        val update = $set(s"temp" -> temperature)
        collection.update(
          MongoDBObject("_id" -> documentIdCached(sensorId, hour)),
          $set(s"secondlyValues.${
            minutes
          }.${
            seconds
          }" -> temperature)
        )
      }
    }
  }
}
