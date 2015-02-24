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
    val values = temperatureConnection.sensorIds().flatMap(l => Try(l.map(id => id -> temperatureConnection.temperature(id).get).toMap)).get
    Reading(clock.now, values)
  }
}

object TemperatureReader {

  case class Reading(timestamp: DateTime, values: Map[String, Float])

}