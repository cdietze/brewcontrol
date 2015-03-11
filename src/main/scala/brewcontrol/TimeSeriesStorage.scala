package brewcontrol

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import com.typesafe.scalalogging.LazyLogging
import org.bson.types.ObjectId
import org.joda.time.DateTime

/**
 * Stores values of time series. Document structure:
 * <pre>
 {
	"_id" : ObjectId("54eacff344ae2d97d8fca4cd"),
	"seriesId" : "temperatureSensor1",
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
class TimeSeriesStorage(val collection: MongoCollection) extends LazyLogging {

  RegisterJodaTimeConversionHelpers()

  /** A cache containing the most recently used documentId per seriesId */
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

  private def documentId(seriesId: String, hour: DateTime): ObjectId = {
    collection.findOne(MongoDBObject(
      "seriesId" -> seriesId,
      "timeStampHour" -> hour)
    ) match {
      case Some(result) => result("_id").asInstanceOf[ObjectId]
      case None => {
        val o = emptyDocument(seriesId, hour)
        collection.insert(o)
        o("_id").asInstanceOf[ObjectId]
      }
    }
  }

  private def toHourTimeStamp(timestamp: DateTime): DateTime = timestamp.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
  private def emptyDocument(seriesId: String, hour: DateTime): MongoDBObject = {
    MongoDBObject("seriesId" -> seriesId,
      "timeStampHour" -> hour)
  }

  /**
   * Creates a new document when it doesn't exist already
   */
  def persist(seriesId: String, timestamp: DateTime, value: Float) {
    logger.debug(s"Persisting $value")
    val hour = toHourTimeStamp(timestamp)
    val minutes = timestamp.getMinuteOfHour
    val seconds = timestamp.getSecondOfMinute
    val query = MongoDBObject("_id" -> documentIdCached(seriesId, hour))
    collection.update(
      MongoDBObject("_id" -> documentIdCached(seriesId, hour)),
      $set(s"secondlyValues.${
        minutes
      }.${
        seconds
      }" -> value)
    )
  }

  def getHourlyDocument(seriesId: String, timeStamp: DateTime): DBObject = {
    val hour = toHourTimeStamp(timeStamp)
    collection.findOne(MongoDBObject(
      "seriesId" -> seriesId,
      "timeStampHour" -> hour)
    ).getOrElse(emptyDocument(seriesId, hour))
  }
}
