package brewcontrol

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import slick.driver.SQLiteDriver.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DbTest extends FlatSpec with Matchers {

  "DB" should "init" in {
    Class.forName("org.sqlite.JDBC")
    sbt.IO.delete(new File("unittest.sqlite"))
    val database = Database.forURL("jdbc:sqlite:unittest.sqlite")
    val db = new DB(database)
    Await.result(db.init(), 5 seconds)

    Await.result(
      db.PropsDao.getProp("test").map(v => assert(v.isEmpty))
      , 5 seconds)

    Await.result(
      db.PropsDao.setProp("test", "1").map(_ => db.PropsDao.getProp("test").map(v => assert(v === Some("2"))))
      , 5 seconds)

  }

}
