package brewcontrol

import com.mongodb.MongoTimeoutException
import com.mongodb.casbah.Imports._

class MockMongoConnection extends MongoConnection {

  override lazy val mongoClient = MongoClient("localhost:27017", MongoClientOptions(connectTimeout = 1000))

  override lazy val db = mongoClient("test_brewcontrol")

  def reset() = db.dropDatabase()

  try {
    mongoClient.getDatabaseNames()
  } catch {
    case e: MongoTimeoutException => sys.error("MongoDB not found, did you really start it?")
  }

  reset()
}
