package brewcontrol

import org.scalajs.dom
import rx.ops._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

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
    val temperatureSeries = getTemperatureData(hour)
    temperatureSeries.map(seriesList => {
      updateData(seriesList)
    })
  }

  def getTemperatureData(hour: Long): Future[js.Array[js.Object]] = {
    def convert(data: Seq[SeriesData]): js.Array[js.Object] = {
      val result: js.Array[js.Object] = js.Array()
      data.foreach(e => result.push(convertSeries(e)))
      result
    }
    def convertSeries(data: SeriesData): js.Object = data.kind match {
      case Temperature => literal("label" -> data.seriesId, "data" -> hourDataToSeries(data.hourTimeData))
      case Relay => literal("label" -> data.seriesId, "data" -> hourDataToSeries(data.hourTimeData), "lines" -> literal("show" -> true, "steps" -> true), "yaxis" -> 2)
    }

    ServerApi.history(hour).map(convert)
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
