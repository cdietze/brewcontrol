package brewcontrol

import java.io.File

import scala.io.Source
import scala.util.Try

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

  def temperature(sensorId: String): Try[Float] = {
    val path = Paths.w1Slave(sensorId)
    Try {
      val lines = Source.fromFile(path).getLines().toList
      val floats = (for {
        line <- lines
        s <- "t=(\\d+)".r.findFirstMatchIn(line).map(_.group(1))
        f = s.toInt / 1000f
      } yield f).toSeq
      floats match {
        case Nil => throw new RuntimeException(s"Failed to parse temperature of sensor: $sensorId, content: '$lines'")
        case v :: _ => v
      }
    }
  }
}
