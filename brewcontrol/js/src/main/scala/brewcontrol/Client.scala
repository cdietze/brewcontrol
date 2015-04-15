package brewcontrol

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLSelectElement
import rx._
import rx.ops._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._



@JSExport
object Client {

  implicit val scheduler = new DomScheduler

  private val obs = mutable.Queue[Obs]()

  def currentHourTimestamp(): Long = {
    val d = new Date()
    d.setMilliseconds(0)
    d.setSeconds(0)
    d.setMinutes(0)
    d.valueOf().toLong
  }

  val oneHourInMillis = (1 hour).toMillis

  val temperaturesRx: Var[List[Reading]] = Var(List())
  val lastTemperatureUpdate: Rx[String] = Rx {
    temperaturesRx() match {
      case Nil => "n/a"
      case x :: _ => s"${new Date(x.timestamp).toLocaleString()}"
    }
  }
  val relaysRx: Var[List[RelayState]] = Var(List())

  val timer: Rx[Long] = Timer(1 second)

  val currentHourRx: Var[Long] = Var(currentHourTimestamp())

  obs enqueue timer.foreach(t => {
    if (t % 5 == 0) {
      updateTemperatures()
      updateRelays()
    }
  })
  def updateTemperatures: (() => Unit) = () => {
    ServerApi.temperatures().foreach { readings =>
      temperaturesRx() = readings
    }
  }

  def updateRelays: (() => Unit) = () => {
    ServerApi.relayStates().foreach { relayStates =>
      relaysRx() = relayStates
    }
  }

  val targetTemperature: Var[Float] = Var(0f)
  var o: Obs = null
  ServerApi.targetTemperature().foreach { v =>
    targetTemperature() = v
    o = targetTemperatureUpdater()
  }

  def targetTemperatureUpdater() = targetTemperature.foreach(v => {
    ServerApi.updateTargetTemperature(v)
  }, skipInitial = true)

  def temperaturesFrag(): Frag = {
    def readingFrag(reading: Reading): Frag = {
      val formattedValue = "%1.2f °C".format(reading.value)
      div(s"${reading.name}: ${formattedValue}")
    }
    temperaturesRx().map(reading => readingFrag(reading))
  }

  def relaysFrag(): Frag = {
    relaysRx().map(state => div(s"${state.name}: ${state.value}"))
  }

  def targetTemperatureSelect(): Frag = {
    lazy val s: HTMLSelectElement = select(
      (0 to 30).map(t => option(t.toString)),
      value := targetTemperature(),
      onchange := { () => {
        targetTemperature() = s.value.toFloat
      }
      }
    ).render
    obs enqueue targetTemperature.foreach(t => s.value = t.toString)
    s
  }

  @JSExport
  def main(container: html.Div) = {
    updateTemperatures()
    updateRelays()

    val flotContainer = div(style := "width:600px;height:300px;background:#eee").render
    val flotMessages = div().render
    container.appendChild(
      div(
        h1("BrewControl"),
        Rx {
          temperaturesFrag()
        },
        Rx {
          relaysFrag()
        },
        Rx {
          div(s"Next update in ${5 - (timer() % 5)} seconds")
        },
        br,
        div("Ziel-Temperatur: ", targetTemperatureSelect(), " °C"),
        br,
        Rx {
          new Date(currentHourRx()).toString
        },
        button("<-", onclick := { () => {
          currentHourRx() = currentHourRx() - oneHourInMillis
        }
        }).render,
        button("->", onclick := { () => {
          currentHourRx() = currentHourRx() + oneHourInMillis
        }
        }).render,
        flotContainer,
        flotMessages
      ).render
    )
    val jQuery = js.Dynamic.global.jQuery
    dom.window.setTimeout({ () => new Plot(flotContainer, flotMessages) }, 1000)
  }
}

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