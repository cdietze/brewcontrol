package brewcontrol

import rx.core.Var
import rx.ops.{Scheduler, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class TemperatureReader(implicit temperatureConnection: TemperatureConnection, scheduler: Scheduler, ex: ExecutionContext, updateInterval: FiniteDuration = 5 seconds) {
  val current = Var[Map[String, Float]](Map())

  private val t = Timer(updateInterval)

  private val ob = t.foreach(_ => {
    val v = temperatureConnection.sensorIds().flatMap(l => Try(l.map(id => id -> temperatureConnection.temperature(id).get).toMap)).get
    current.update(v)
  })
}
