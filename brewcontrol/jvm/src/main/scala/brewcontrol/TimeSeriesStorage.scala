package brewcontrol

import com.mongodb.casbah.Imports._
import com.typesafe.scalalogging.LazyLogging
import org.bson.types.ObjectId
import org.joda.time.DateTime

import scala.collection.mutable.ListBuffer

/**
 * Stores values of time series. Document structure:
 * <pre>
 {
	"_id" : ObjectId("54eacff344ae2d97d8fca4cd"),
	"seriesId" : "temperatureSensor1",
	"hourTimestamp" : 1426374000000,
	"values" : [
		[0, 22.625],
		[1, 22.5],
    [16, 22.5],
    ...
    [3599, 23.0]
		]
	}
}
</pre>
 */
class TimeSeriesStorage(val collection: MongoCollection) extends LazyLogging {

  /** A cache containing the most recently used documentId per seriesId */
  private var documentIdCache = Map[String, (Long, ObjectId)]()

  private def documentIdCached(sensorId: String, hour: Long): ObjectId = {
    documentIdCache.get(sensorId).filter(_._1 == hour) match {
      case Some((h, docId)) => docId
      case None => {
        val docId = documentId(sensorId, hour)
        documentIdCache = documentIdCache.updated(sensorId, (hour, docId))
        docId
      }
    }
  }

  private def documentId(seriesId: String, hour: Long): ObjectId = {
    collection.findOne(MongoDBObject(
      "seriesId" -> seriesId,
      "hourTimestamp" -> hour)
    ) match {
      case Some(result) => result("_id").asInstanceOf[ObjectId]
      case None => {
        val o = emptyDocument(seriesId, hour)
        collection.insert(o)
        o("_id").asInstanceOf[ObjectId]
      }
    }
  }

  private def emptyDocument(seriesId: String, hour: Long): MongoDBObject = {
    MongoDBObject("seriesId" -> seriesId,
      "hourTimestamp" -> hour)
  }

  /**
   * Creates a new document when it doesn't exist already
   */
  def persist(seriesId: String, timestamp: Long, value: Double) {
    logger.debug(s"Persisting $value")
    val date = new DateTime(timestamp)
    val hour = date.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).getMillis()
    val seconds = date.getSecondOfMinute + date.getMinuteOfHour * 60
    val query = MongoDBObject("_id" -> documentIdCached(seriesId, hour))
    collection.update(
      MongoDBObject("_id" -> documentIdCached(seriesId, hour)),
      $push(("values", MongoDBList(seconds, value)))
    )
  }

  def getDocument(seriesId: String, hourTimestamp: Long): Option[HourTimeData] = {
    Option(collection.find(MongoDBObject("seriesId" -> seriesId, "hourTimestamp" -> hourTimestamp)).one())
      .map(TimeSeriesStorage.dbObjectToTimeData)
  }
  def getLatestDocument(seriesId: String): Option[HourTimeData] = {
    Option(collection.find(MongoDBObject("seriesId" -> seriesId)).sort(MongoDBObject("hourTimestamp" -> -1)).one())
      .map(TimeSeriesStorage.dbObjectToTimeData)
  }
  /** Returns a map of sensorId -> HourTimeData for all documents available for that hour */
  def getDocumentsByHour(hourTimestamp: Long): Map[String, HourTimeData] = {
    collection.find(MongoDBObject("hourTimestamp" -> hourTimestamp)).map(o =>
      o.as[String]("seriesId") -> TimeSeriesStorage.dbObjectToTimeData(o)
    ).toMap
  }

  def deleteDocumentsOlderThan(timestamp: Long): Unit = {
    val result = collection.remove("hourTimestamp" $lt timestamp)
    logger.info(s"Deleted ${result.getN} expired documents that are older than ${new DateTime(timestamp)}")
  }
}

object TimeSeriesStorage {
  implicit def dbObjectToTimeData(o: DBObject): HourTimeData = {
    val values = ListBuffer[(Int, Double)]()
    val list = o.get("values").asInstanceOf[BasicDBList]
    for (elem <- list) {
      val tuple = elem.asInstanceOf[BasicDBList]
      val time = tuple.as[Double](0).toInt
      val temp = tuple.as[Double](1)
      values.append((time, temp))
    }
    HourTimeData(o.as[Long]("hourTimestamp"), values.toList)
  }
}
