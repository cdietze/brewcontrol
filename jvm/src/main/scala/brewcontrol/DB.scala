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

  def init() = db.run(
    if (!tableNames().contains("Props")) {
      props.schema.create
    } else {
      DBIO.successful()
    })

  def runSync[R](f: Future[R]): R = Await.result(f, 10 seconds)
  def runSync[R](a: DBIOAction[R, NoStream, Nothing]): R = runSync(db.run(a): Future[R])

  private def tableNames(): Set[String] = Await.result(db.run(MTable.getTables).map(_.map(_.name.name).toSet), 60 seconds)

  class Props(tag: Tag) extends Table[(String, String)](tag, "Props") {
    def key = column[String]("ID", O.PrimaryKey)
    def value = column[String]("VALUE")
    def * = (key, value)
  }
  val props = TableQuery[Props]

  object Props {

    def setProp(key: String, value: String): Future[_] = {
      db.run(props.insertOrUpdate((key, value)))
    }

    def getProp(key: String): Future[Option[String]] = {
      db.run(props.filter(_.key === key).map(_.value).result.headOption)
    }

    /** @return a Var that is backed by a row in the Props table. */
    def asVar[T](propsKey: String, defaultVal: T)(implicit rw: ReadWriter[T]): Future[Var[T]] = {
      getProp(propsKey).map(v => {
        val initialVal: T = v.map(s => read[T](s)).getOrElse(defaultVal)
        new Var(initialVal) {
          val o = this.foreach((x: T) =>
            setProp(propsKey, write(x))
          )
        }
      })
    }

    def asVarSync[T](propsKey: String, defaultVal: T)(implicit rw: ReadWriter[T]): Var[T] = runSync(asVar(propsKey, defaultVal))
  }
}

object DB {
}
