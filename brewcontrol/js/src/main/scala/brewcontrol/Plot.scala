package brewcontrol

import org.scalajs.dom
import rx.Var
import rx.ops._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

class Plot(plotContainer: dom.Element) {

  val data = js.Array()
  val options = literal(
    "legend" -> literal(
      "show" -> false
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

  case class SeriesInfo(seriesId: String, show: Boolean, color: Int)

  val seriesStatus: Var[List[SeriesInfo]] = new Var(List())

  val o = Client.currentHourRx.foreach { hour =>
    update(hour)
  }

  def toggleSeries(seriesId: String): Unit = {
    val e = seriesStatus.now.zipWithIndex.find(_._1.seriesId == seriesId).get
    seriesStatus.updateSilent(seriesStatus.now.updated(e._2, e._1.copy(show = !e._1.show)))
    update()
  }

  def color(seriesId: String): Option[String] = {
    val series = plot.getData().asInstanceOf[js.Array[js.Dynamic]]
    series.find(e => e.label == seriesId) match {
      case None => None
      case Some(x) => Some(x.color.asInstanceOf[String])
    }
  }

  def update(): Future[_] = {
    update(Client.currentHourRx.now)
  }

  def update(hour: Long): Future[_] = {
    val temperatureSeries = getTemperatureData(hour)
    temperatureSeries.map(seriesList => {
      updateData(seriesList)
    }).map(x => {
      seriesStatus.propagate()
      x
    })
  }

  def getTemperatureData(hour: Long): Future[js.Array[js.Object]] = {
    def convert(data: Seq[SeriesData]): js.Array[js.Object] = {
      val result: js.Array[js.Object] = js.Array()
      data.foreach(e => {
        seriesStatus.now.find(_.seriesId == e.seriesId) match {
          case Some(SeriesInfo(_, true, color)) =>
            result.push(convertSeries(e, color))
          case Some(_) =>
          case None =>
            val color = seriesStatus.now.length
            seriesStatus.update(seriesStatus.now ::: List(SeriesInfo(e.seriesId, true, color)))
            result.push(convertSeries(e, color))
        }
      })
      result
    }
    def convertSeries(data: SeriesData, index: Int): js.Object = data.kind match {
      case Temperature => literal("label" -> data.seriesId, "data" -> hourDataToSeries(data.hourTimeData), "color" -> index)
      case Relay => literal("label" -> data.seriesId, "data" -> hourDataToSeries(data.hourTimeData), "color" -> index, "lines" -> literal("show" -> true, "steps" -> true), "yaxis" -> 2)
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
