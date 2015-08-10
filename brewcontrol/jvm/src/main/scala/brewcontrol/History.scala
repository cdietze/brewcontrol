package brewcontrol

import akka.agent.Agent

import scala.collection.immutable.Queue

import scala.concurrent.ExecutionContext.Implicits.global

object History {
  sealed trait Kind
  case object binary extends Kind
  case object double extends Kind

  type Item = (Long, Double)

  case class Series(name: String, kind: Kind, data: Queue[Item] = Queue()) {
    def addItem(item: Item): Series = {
      this.copy(data = data.enqueue(item))
    }
  }

  private val agent: Agent[Map[String, Series]] = Agent(Map())

  def addItem(name: String, kind: Kind, item: Item) = {
    agent send (seriesMao => {
      val series = seriesMao.getOrElse(name, Series(name, kind)).addItem(item)
      seriesMao.updated(name, series)
    })
  }

  def get() = agent.get()
}
