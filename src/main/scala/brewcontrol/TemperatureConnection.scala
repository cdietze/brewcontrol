package brewcontrol

import scala.io.Source
import scala.util.Try

/** Low level interface to deal with temperature sensors */
class TemperatureConnection {

  def sensorIds(): Try[Set[String]] = {
    val path = "/sys/bus/w1/devices/w1_bus_master1/w1_master_slaves"
    Try(Source.fromFile(path).getLines().toSet)
  }

  def temperature(sensorId: String): Try[Float] = {
    val path = s"/sys/bus/w1/devices/$sensorId/w1_slave"
    Try((for {
      line <- Source.fromFile(path).getLines()
      s <- "t=(\\d+)".r.findFirstMatchIn(line).map(_.group(1))
      f = s.toInt / 1000f
    } yield f).toSeq.head)
  }
}
