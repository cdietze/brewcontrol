package brewcontrol

import rx.core.Var
import rx.ops._

class Config()(implicit mongoConnection: MongoConnection) {

  import com.mongodb.casbah.Imports._

  private val collection = mongoConnection.db("config")

  private val soleDocQuery = MongoDBObject("_id" -> "config")

  val targetTemperature = mongoVar(soleDocQuery, "targetTemperature", 20d)

  val heaterEnabled = mongoVar(soleDocQuery, "heaterEnabled", false)
  val coolerEnabled = mongoVar(soleDocQuery, "coolerEnabled", false)

  /** @return a Var that is backed by a specific field in a specific document. Any changes to this var will be propagated via update() to the field. */
  private def mongoVar[T](docQuery: MongoDBObject, fieldName: String, defaultVal: T)(implicit m: Manifest[T]): Var[T] = {
    val initialVal: T = Option(collection.find(docQuery).one()).flatMap(o => o.getAs[T](fieldName)).getOrElse(defaultVal)
    new Var(initialVal) {
      val o = this.foreach((x: T) =>
        collection.update(docQuery, $set(fieldName -> x), upsert = true)
      )
    }
  }
}