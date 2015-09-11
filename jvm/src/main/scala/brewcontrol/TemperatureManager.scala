package brewcontrol

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import rx.Rx
import rx.ops.{Scheduler, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Try

case class TemperatureReading(timestamp: Long, sensorId: String, name: String, value: Double)

trait TemperatureManager {
  type Readings = List[TemperatureReading]

  def currentReadings: Rx[Readings]

  def currentReading(sensorId: String): Try[TemperatureReading] = Try(currentReadings().find(_.sensorId == sensorId).get)

  sealed abstract class Sensor(val id: String,
                               val name: String) {
    lazy val temperature: Rx[Double] = Rx {
      currentReadings().find(_.sensorId == id).getOrElse(throw new RuntimeException(s"No reading found for $id - $name") ).value
    }
  }

  case object Bucket extends Sensor("28-031462078cff", "Gäreimer")
  case object Cooler extends Sensor("28-0214638301ff", "Kühlschrank")
  case object Outside extends Sensor("28-011463e799ff", "Außen")
  case object Pot extends Sensor("28-02146345f4ff", "Kessel")

  val sensors = List(Bucket, Cooler, Outside, Pot)
  def sensorName(sensorId: String): String = sensors.find(_.id == sensorId).map(_.name).getOrElse(s"Sensor($sensorId)")
}

object TemperatureManager {
}

class TemperatureManagerImpl()(implicit temperatureConnection: TemperatureConnection, clock: Clock, scheduler: Scheduler, ex: ExecutionContext, updateInterval: FiniteDuration = 5 seconds) extends TemperatureManager with LazyLogging {
  val currentReadings: Rx[Readings] = Timer(updateInterval).map { t =>
    reading().get
  }

  private def reading(): Try[Readings] = {
    Try {
      val sensorIds = temperatureConnection.sensorIds().get.toList
      sensorIds.map(sensorId => TemperatureReading(System.currentTimeMillis(), sensorId, sensorName(sensorId), temperatureConnection.temperature(sensorId).get))
    }
  }
}

/** Low level interface to deal with temperature sensors */
class TemperatureConnection {

  private object Paths {
    def w1MasterSlaves: String = "/sys/bus/w1/devices/w1_bus_master1/w1_master_slaves"
    def w1Slave(sensorId: String): String = s"/sys/bus/w1/devices/$sensorId/w1_slave"
  }

  def sensorIds(): Try[Set[String]] = {
    Try {
      val ids = Source.fromFile(Paths.w1MasterSlaves).getLines().toSet
      // Filter out any devices that don't have w1_slave files. Whatever they are, they aren't the sensors we seek
      ids.filter(id => new File(Paths.w1Slave(id)).exists())
    }
  }

  def temperature(sensorId: String): Try[Double] = {
    val path = Paths.w1Slave(sensorId)
    Try {
      Source.fromFile(path).getLines().toList
    }.flatMap(lines => parseTemperature(lines))
  }

  def parseTemperature(lines: Seq[String]): Try[Double] = {
    Try {
      val floats = (for {
        line <- lines
        s <- "t=(-?\\d+)".r.findFirstMatchIn(line).map(_.group(1))
        f = s.toInt / 1000d
      } yield f).toSeq
      floats match {
        case Nil => throw new RuntimeException(s"Failed to parse temperature event, content: '$lines'")
        case v :: _ => v
      }
    }
  }
}
