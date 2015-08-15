package brewcontrol

import com.typesafe.scalalogging.LazyLogging
import rx._
import rx.ops._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * sp = setpoint = goal temperature
 * pv = process variable = temperature reading
 *
 * @see http://en.wikipedia.org/wiki/PID_controller#PID_controller_theory
 */
class PidController(sp: Rx[Double], pv: Rx[Double], updateInterval: FiniteDuration)(implicit scheduler: Scheduler, ec: ExecutionContext) extends LazyLogging {

  val Kp = 1d

  def calcOutput(): Double = {
    val error = sp() - pv()
    val o = Kp * error
    logger.debug(s"PID calculation: o=$o, sp=${sp()}, pv=${pv()}, Kp=$Kp")
    o
  }

  val output: Rx[Double] = Timer(updateInterval).map { t =>
    val o = calcOutput()
    o
  }
}
