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

  val readingsRx: Var[List[Reading]] = Var(List())
  val lastUpdate: Rx[String] = Rx {
    readingsRx() match {
      case Nil => "n/a"
      case x :: _ => s"${new Date(x.timestamp).toLocaleString()}"
    }
  }
  val timer: Rx[Long] = Timer(5 seconds).map(_ => Date.now().toLong)

  @JSExport
  def main(container: html.Div) = {
    val inputBox = input.render
    val outputBox = ul.render
    val btn = button("My Button").render
    inputBox.onkeyup = (e: dom.Event) => outputBox.innerHTML = inputBox.value

    def updateTemperatures: (() => Unit) = () => {
      Ajax.get("/temperatures").foreach(xhr => {
        val readings = upickle.read[List[Reading]](xhr.responseText)
        val map = readings.groupBy(_.sensorId).mapValues(_.head)
        readingsRx() = readings
        dom.window.setTimeout(updateTemperatures, (5 seconds).toMillis)
      })
    }
    updateTemperatures()

    container.appendChild(
      div(
        h1("Current temperatures"),
        Rx {
          div(s"Last update: ${lastUpdate()}")
        },
        Rx {
          allSensors()
        },
        br, br, br,
        inputBox,
        outputBox,
        btn
      ).render
    )
  }

  def allSensors(): Frag = {
    readingsRx().map(reading => readingFrag(reading))
  }

  def readingFrag(reading: Reading): Frag = {
    val age = DurationLong(timer() - reading.timestamp).millis
    val ageSuffix = if (age < (30 seconds)) "" else s" ${age.toSeconds} seconds ago"
    div(s"${reading.sensorId}: ${reading.value}${ageSuffix}")
  }
}
