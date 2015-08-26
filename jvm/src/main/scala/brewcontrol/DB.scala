package brewcontrol

import rx.core.Var
import rx.ops._
import slick.dbio.{DBIOAction, NoStream}
import slick.driver.SQLiteDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import upickle.default._

class DB(val db: Database) {
  import DB._

  def init() = db.run(
    if (!tableNames().contains("Props")) {
      propsTable.schema.create
    } else {
      DBIO.successful()
    })

  def runSync[R](f: Future[R]): R = Await.result(f, 10 seconds)
  def runSync[R](a: DBIOAction[R, NoStream, Nothing]): R = runSync(db.run(a): Future[R])

  private def tableNames(): Set[String] = Await.result(db.run(MTable.getTables).map(_.map(_.name.name).toSet), 60 seconds)

  /** DAO for the Props table */
  object PropsDao {

    val targetTemperature = asVarSync("targetTemperature", 20d)

    val heaterEnabled = asVarSync("heaterEnabled", false)
    val coolerEnabled = asVarSync("coolerEnabled", false)

    def setProp(key: String, value: String): Future[_] = {
      db.run(propsTable.insertOrUpdate((key, value)))
    }

    def getProp(key: String): Future[Option[String]] = {
      db.run(propsTable.filter(_.key === key).map(_.value).result.headOption)
    }

    /** @return a Var that is backed by a row in the Props table. */
    private def asVar[T](propsKey: String, defaultVal: T)(implicit rw: ReadWriter[T]): Future[Var[T]] = {
      getProp(propsKey).map(v => {
        val initialVal: T = v.map(s => read[T](s)).getOrElse(defaultVal)
        new Var(initialVal) {
          val o = this.foreach((x: T) =>
            setProp(propsKey, write(x))
          )
        }
      })
    }

    private def asVarSync[T](propsKey: String, defaultVal: T)(implicit rw: ReadWriter[T]): Var[T] = runSync(asVar(propsKey, defaultVal))
  }
}

object DB {

  class PropsTable(tag: Tag) extends Table[(String, String)](tag, "Props") {
    def key = column[String]("ID", O.PrimaryKey)
    def value = column[String]("VALUE")
    def * = (key, value)
  }
  val propsTable = TableQuery[PropsTable]

}
