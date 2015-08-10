package brewcontrol

import akka.agent.Agent
import upickle.Js

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object History {
  sealed trait Kind
  case object binary extends Kind {
    implicit val writer = upickle.default.Writer[binary.type] {
      case t => Js.Str("binary")
    }
  }
  case object double extends Kind {
    implicit val writer = upickle.default.Writer[double.type] {
      case t => Js.Str("double")
    }
  }

  type Item = (Long, Double)

  case class Series(name: String, kind: Kind, data: Queue[Item] = Queue()) {
    val maxAge = (1 day).toMillis
    def addItem(item: Item): Series = {
      var d = data.enqueue(item)
      d = d.dropWhile { case (t, v) => ((item._1 - t) > maxAge) }
      this.copy(data = d)
    }
  }

  private val agent: Agent[Map[String, Series]] = Agent(Map())

  def addItem(name: String, kind: Kind, item: Item) = {
    agent send (seriesMap => {
      val series = seriesMap.getOrElse(name, Series(name, kind)).addItem(item)
      seriesMap.updated(name, series)
    })
  }

  def get() = agent.get()
}
