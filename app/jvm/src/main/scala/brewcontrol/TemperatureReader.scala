package brewcontrol

import brewcontrol.TemperatureReader.{Reading, Readings}
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import rx.Rx
import rx.ops.{Scheduler, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

trait TemperatureReader {
  import brewcontrol.TemperatureReader._
  def currentReadings: Rx[Readings]

  sealed abstract class Sensor(val id: String,
                               val name: String) {
    lazy val temperature: Rx[Float] = Rx {
      currentReadings().find(_.sensorId == id).get.value
    }
  }

  case object Bucket extends Sensor("28-031462078cff", "Gäreimer")
  case object Cooler extends Sensor("28-0214638301ff", "Kühlschrank")
  case object Outside extends Sensor("28-011463e799ff", "Außen")

  val sensors = List(Bucket, Cooler, Outside)
  def sensorName(sensorId: String): String = sensors.find(_.id == sensorId).map(_.name).getOrElse(s"Sensor($sensorId)")
}

object TemperatureReader {
  type Readings = List[Reading]
  case class Reading(timestamp: DateTime, sensorId: String, value: Float)
}

class TemperatureReaderImpl()(implicit temperatureConnection: TemperatureConnection, clock: Clock, scheduler: Scheduler, ex: ExecutionContext, updateInterval: FiniteDuration = 5 seconds) extends TemperatureReader with LazyLogging {
  val currentReadings: Rx[Readings] = Timer(updateInterval).map { t =>
    reading().get
  }

  private def reading(): Try[Readings] = {
    Try {
      val sensorIds = temperatureConnection.sensorIds().get.toList
      sensorIds.map(sensorId => Reading(DateTime.now(), sensorId, temperatureConnection.temperature(sensorId).get))
    }
  }
}