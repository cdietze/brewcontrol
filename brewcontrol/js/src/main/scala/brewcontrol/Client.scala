package brewcontrol

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html
import rx.core.{Rx, Var}
import rx.ops.{DomScheduler, Timer}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

object ServerApi {
  def temperatures(): Future[List[Reading]] = {
    Ajax.get("/temperatures").map(xhr => {
      upickle.read[List[Reading]](xhr.responseText)
    })
  }

  def temperatureHourData(sensorId: String): Future[HourTimeData] = {
    Ajax.get(s"/temperatures/$sensorId/hour").map(xhr => {
      upickle.read[HourTimeData](xhr.responseText)
    })
  }

  def relayStates(): Future[List[RelayState]] = {
    Ajax.get("/relays").map(xhr => {
      upickle.read[List[RelayState]](xhr.responseText)
    })
  }
}

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
    ServerApi.temperatures().foreach { readings =>
      temperaturesRx() = readings
      dom.window.setTimeout(updateTemperatures, (5 seconds).toMillis)
    }
  }

  def updateRelays: (() => Unit) = () => {
    ServerApi.relayStates().foreach { relayStates =>
      relaysRx() = relayStates
      dom.window.setTimeout(updateRelays, (5 seconds).toMillis)
    }
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
        },
        div(id := "flotContainer", style := "width:600px;height:300px;background:#eee")
      ).render
    )

    dom.window.setTimeout(Plot.init, 1000)
  }
}

object Plot {

  import scala.scalajs.js.Dynamic.literal

  var plot: js.Dynamic = null

  def init(): (() => Unit) = () => {
    val seriesData = js.Array(js.Array(0, 0), js.Array(1, 1))
    val data = js.Array()
    val options = literal("series" -> literal("shadowSize" -> 0))
    plot = js.Dynamic.global.jQuery.plot(js.Dynamic.global.jQuery("#flotContainer"), data, options)

    getSensorData(sensorId).map(data => updateData(js.Array(data)))
  }

  def update(): Unit = {
    ServerApi.temperatures().flatMap(readings => {
      val x = readings.map {
        reading => getSensorData(reading.sensorId)
      }
      Future.sequence(x).map(seriesList => {
        val data: js.Array[js.Object] = js.Array()
        seriesList.foreach(s => data.push(s))
        updateData(data)
      })
    })
  }

  val sensorId = "SensorA"

  def getSensorData(sensorId: String): Future[js.Object] = {
    ServerApi.temperatureHourData(sensorId).map(hourData => {
      val data: js.Array[js.Array[Float]] = js.Array()
      hourData.values.foreach { case (k, v) => data.push(js.Array(k.toFloat, v))}
      literal("data" -> data, "label" -> sensorId)
    })
  }

  def updateData(data: js.Array[_]): Unit = {
    println(s"pushing new data: $data")
    plot.setData(data)
    plot.setupGrid()
    plot.draw()
  }
}