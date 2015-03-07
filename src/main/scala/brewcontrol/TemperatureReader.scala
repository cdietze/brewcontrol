package brewcontrol

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import rx.Rx
import rx.ops.{Scheduler, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class TemperatureReader()(implicit temperatureConnection: TemperatureConnection, clock: Clock, scheduler: Scheduler, ex: ExecutionContext, updateInterval: FiniteDuration = 5 seconds) extends LazyLogging {

  import brewcontrol.TemperatureReader._

  val currentReading: Rx[Reading] = Timer(updateInterval).map { t =>
    reading().get
  }

  private def reading(): Try[Reading] = {
    val values = temperatureConnection.sensorIds().flatMap(sensorIds =>
      Try(sensorIds.map(sensorId =>
        sensorId -> temperatureConnection.temperature(sensorId).get)
        .toMap))
    logger.debug(s"Read temperatures: $values")
    values.map(v => Reading(clock.now, v))
  }

  def sensorName(sensorId: String): String = sensors.find(_.id == sensorId).map(_.name).getOrElse(s"Sensor($sensorId}")

  sealed abstract class Sensor(val id: String,
                               val name: String) {
    lazy val temperature: Rx[Float] = Rx {
      currentReading().values.get(id).get
    }
  }

  case object DevBoard extends Sensor("28-011463e799ff", "Entwicklungsboard")
  case object BucketInside extends Sensor("28-031462078cff", "Gäreimer Innen")
  case object BucketOutside extends Sensor("28-0214638301ff", "Gäreimer Außen")

  val sensors = List(BucketOutside, BucketInside)
}

object TemperatureReader {

  case class Reading(timestamp: DateTime, values: Map[String, Float])

}

