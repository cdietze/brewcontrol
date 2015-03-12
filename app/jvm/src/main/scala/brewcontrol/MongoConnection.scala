package brewcontrol

import com.mongodb.casbah.Imports._

class MongoConnection {

  lazy val mongoClient: MongoClient = MongoClient("localhost", 27017)

  lazy val db: MongoDB = mongoClient("brewcontrol")
}
