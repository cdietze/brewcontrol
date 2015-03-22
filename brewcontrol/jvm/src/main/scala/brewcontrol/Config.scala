package brewcontrol

import rx.core.Var
import rx.ops._

class Config()(implicit mongoConnection: MongoConnection) {

  import com.mongodb.casbah.Imports._

  private val collection = mongoConnection.db("targetTemperature")

  private val idQuery = MongoDBObject("_id" -> "targetTemperature")

  val targetTemperature = new Var(initialValue())

  private def initialValue(): Float = {
    val o = Option(collection.find(idQuery).one()).map(o => o.as[Double]("value").toFloat)
    o.getOrElse(20f)
  }

  private val obs = targetTemperature.foreach((v: Float) =>
    collection.update(idQuery, $set("value" -> v), upsert = true)
  )
}