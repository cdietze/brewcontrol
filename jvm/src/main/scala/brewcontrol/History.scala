package brewcontrol

import akka.agent.Agent

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class History {

  import History._

  private val agent: Agent[Map[String, Series]] = Agent(Map())

  def addItem(name: String, kind: String, item: Item) = {
    agent send (seriesMap => {
      val series = seriesMap.getOrElse(name, Series(name, kind)).addItem(item)
      seriesMap.updated(name, series)
    })
  }

  def get() = agent.get()
}

object History extends History {
  case class Item(x: Long, y: Double) {

  }
  object Item {
    // def apply(t: (Long, Double)): Item = apply(t._1, t._2)
    implicit def tupleToItemInt(t: (Long, Double)): Item = Item(t._1, t._2)
    // implicit def tupleToItemLong(t: (Long, Double)): Item = Item(t._1, t._2)
  }

  case class Series(name: String, kind: String, data: Queue[Item] = Queue()) {
    def addItem(item: Item): Series = {
      var d = data.enqueue(item)
      d = d.dropWhile { e => ((item.x - e.x) >= maxItemAge) }
      this.copy(data = d)
    }
  }

  val maxItemAge = (2 hours).toMillis
}