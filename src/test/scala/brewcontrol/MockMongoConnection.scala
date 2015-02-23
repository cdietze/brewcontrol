package brewcontrol

import com.mongodb.casbah.Imports._

class MockMongoConnection extends MongoConnection {

  val mongoClient = MongoClient("localhost", 27017)

  override def db = mongoClient("brewcontrol")

  def reset() = db.dropDatabase()

  reset()
}
