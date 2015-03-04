package brewcontrol

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import rx.core.{Obs, Var}
import rx.ops.{Scheduler, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class TemperatureReader()(implicit temperatureConnection: TemperatureConnection, clock: Clock, scheduler: Scheduler, ex: ExecutionContext, updateInterval: FiniteDuration = 5 seconds) extends LazyLogging {

  import brewcontrol.TemperatureReader._

  val current = Var[Reading](Reading(clock.now, Map()))

  private val timer = Timer(updateInterval)

  private val obs = Obs(timer) {
    reading() match {
      case Success(r) => current.update(r)
      case Failure(e) => logger.warn(s"Failed to get reading", e)
    }
  }

  private def reading(): Try[Reading] = {
    val values = temperatureConnection.sensorIds().flatMap(sensorIds =>
      Try(sensorIds.map(sensorId =>
        sensorName(sensorId) -> temperatureConnection.temperature(sensorId).get)
        .toMap))
    logger.debug(s"Read temperatures: $values")
    values.map(v => Reading(clock.now, v))
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