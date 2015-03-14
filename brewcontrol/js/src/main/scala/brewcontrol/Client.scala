package brewcontrol

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html
import rx.core.{Rx, Var}
import rx.ops.{DomScheduler, Timer}

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
object Client {

  implicit val scheduler = new DomScheduler

  val temperaturesRx: Var[List[Reading]] = Var(List())
  val lastTemperatureUpdate: Rx[String] = Rx {
    temperaturesRx() match {
      case Nil => "n/a"
      case x :: _ => s"${new Date(x.timestamp).toLocaleString()}"
    }
  }
  val relaysRx: Var[List[RelayState]] = Var(List())

  val timer: Rx[Long] = Timer(5 seconds).map(_ => Date.now().toLong)

  def updateTemperatures: (() => Unit) = () => {
    Ajax.get("/temperatures").foreach(xhr => {
      temperaturesRx() = upickle.read[List[Reading]](xhr.responseText)
      dom.window.setTimeout(updateTemperatures, (5 seconds).toMillis)
    })
  }

  def updateRelays: (() => Unit) = () => {
    Ajax.get("/relays").foreach(xhr => {
      relaysRx() = upickle.read[List[RelayState]](xhr.responseText)
      dom.window.setTimeout(updateRelays, (5 seconds).toMillis)
    })
  }

  def temperaturesFrag(): Frag = {
    def readingFrag(reading: Reading): Frag = {
      val age = DurationLong(timer() - reading.timestamp).millis
      val ageSuffix = if (age < (30 seconds)) "" else s" ${age.toSeconds} seconds ago"
      div(s"${reading.name}: ${reading.value}${ageSuffix}")
    }
    temperaturesRx().map(reading => readingFrag(reading))
  }

  def relaysFrag(): Frag = {
    relaysRx().map(state => div(s"${state.name}: ${state.value}"))
  }

  @JSExport
  def main(container: html.Div) = {
    updateTemperatures()
    updateRelays()

    container.appendChild(
      div(
        h1("BrewControl"),
        h2("Current temperatures"),
        Rx {
          div(s"Last update: ${lastTemperatureUpdate()}")
        },
        Rx {
          temperaturesFrag()
        },
        h2("Relay states"),
        Rx {
          relaysFrag()
        }
      ).render
    )
  }
}
