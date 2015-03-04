package brewcontrol

import com.mongodb.casbah.Imports._

class MongoConnection {

  lazy val mongoClient = MongoClient("localhost", 27017)

  lazy val db = mongoClient("brewcontrol")
}
