package brewcontrol

import akka.agent.Agent

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class History {

  import History._

  type Item = (Long, Double)

  case class Series(name: String, kind: String, data: Queue[Item] = Queue()) {
    def addItem(item: Item): Series = {
      var d = data.enqueue(item)
      d = d.dropWhile { case (t, v) => ((item._1 - t) >= maxItemAge) }
      this.copy(data = d)
    }
  }

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
  val maxItemAge = (1 day).toMillis
}