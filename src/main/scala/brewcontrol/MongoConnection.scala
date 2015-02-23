package brewcontrol

import com.mongodb.casbah.Imports._

class MongoConnection {

  private val mongoClient = MongoClient("localhost", 27017)

  def db = mongoClient("brewcontrol")
}
