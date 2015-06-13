package brewcontrol

import rx.core.Var
import rx.ops._

class Config()(implicit mongoConnection: MongoConnection) {

  import com.mongodb.casbah.Imports._

  private val collection = mongoConnection.db("targetTemperature")

  private val idQuery = MongoDBObject("_id" -> "targetTemperature")

  val targetTemperature = new Var(initialValue())

  private def initialValue(): Double = {
    val o = Option(collection.find(idQuery).one()).map(o => o.as[Double]("value"))
    o.getOrElse(20f)
  }

  private val obs = targetTemperature.foreach((v: Double) =>
    collection.update(idQuery, $set("value" -> v), upsert = true)
  )
}