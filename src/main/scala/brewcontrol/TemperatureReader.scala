package brewcontrol

import org.joda.time.DateTime
import rx.core.Var
import rx.ops.{Scheduler, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class TemperatureReader()(implicit temperatureConnection: TemperatureConnection, clock: Clock, scheduler: Scheduler, ex: ExecutionContext, updateInterval: FiniteDuration = 5 seconds) {

  import brewcontrol.TemperatureReader._

  val current = Var[Reading](reading())

  private val t = Timer(updateInterval)

  private val obs = t.foreach(_ => current.update(reading()))

  private def reading(): Reading = {
    val values = temperatureConnection.sensorIds().flatMap(sensorIds =>
      Try(sensorIds.map(sensorId =>
        sensorName(sensorId) -> temperatureConnection.temperature(sensorId).get)
        .toMap)).get
    Reading(clock.now, values)
  }

  private def sensorName(sensorId: String): String = sensorId match {
    case "28-031462078cff" => "Gäreimer Innen"
    case "28-011463e799ff" => "Gäreimer Umgebung"
    case x => s"SensorId($x)"
  }
}

object TemperatureReader {

  case class Reading(timestamp: DateTime, values: Map[String, Float])

}