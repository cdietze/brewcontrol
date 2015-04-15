package brewcontrol

import org.scalajs.dom
import rx.ops._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scalatags.JsDom.all._

class Plot(plotContainer: dom.Element, messagesContainer: dom.Element) {

  val data = js.Array()
  val options = literal(
    "legend" -> literal(
      "position" -> "nw"
    ),
    "series" -> literal("shadowSize" -> 0),
    "xaxis" -> literal(
      "mode" -> "time",
      "timezone" -> "browser"
    ),
    "yaxes" -> js.Array(
      literal("position" -> "right"),
      literal("show" -> false, "min" -> -0.1, "max" -> 1.1)
    ))
  val plot = js.Dynamic.global.jQuery.plot(plotContainer, data, options)

  val o = Client.currentHourRx.foreach { hour =>
    update(hour)
  }

  def update(hour: Long): Unit = {
    messagesContainer.innerHTML = ""
    val temperatureFutures: Future[List[js.Object]] = ServerApi.temperatures().flatMap(readings => {
      val futures = readings.map(
        reading => getTemperatureData(reading.sensorId, reading.name, hour).map(Some(_)).recover { case x => {
          messagesContainer.appendChild(div(s"No temperature data available for: ${reading.name}").render)
          None
        }
        }
      )
      Future.sequence(futures).map(_.flatten)
    })
    val relayFutures: Future[List[js.Object]] = ServerApi.relayStates().flatMap(states => {
      val futures = states.map(
        state => getRelayData(state.name, hour).map(Some(_)).recover { case x => {
          messagesContainer.appendChild(div(s"No relay data available for: ${state.name}").render)
          None
        }
        }
      )
      Future.sequence(futures).map(_.flatten)
    })

    val allFutures = Future.sequence(List(temperatureFutures, relayFutures)).map(_.flatten)
    allFutures.map(seriesList => {
      val data: js.Array[js.Object] = js.Array()
      seriesList.foreach(s => data.push(s))
      updateData(data)
    })
  }

  def getTemperatureData(sensorId: String, name: String, hour: Long): Future[js.Object] = {
    ServerApi.temperatureHourData(sensorId, hour).map(hourData => {
      literal("label" -> name, "data" -> hourDataToSeries(hourData))
    })
  }

  def getRelayData(relayName: String, hour: Long): Future[js.Object] = {
    ServerApi.relayHourData(relayName, hour).map(hourData => {
      literal("label" -> relayName, "data" -> hourDataToSeries(hourData), "lines" -> literal("show" -> true, "steps" -> true), "yaxis" -> 2)
    })
  }

  def hourDataToSeries(hourData: HourTimeData): js.Array[_] = {
    val data: js.Array[js.Array[js.Any]] = js.Array()
    hourData.values.foreach { case (k, v) => data.push(js.Array(k.toLong * 1000L + hourData.hourTimestamp, v)) }
    data
  }

  def updateData(data: js.Array[_]): Unit = {
    plot.setData(data)
    plot.setupGrid()
    plot.draw()
  }
}
