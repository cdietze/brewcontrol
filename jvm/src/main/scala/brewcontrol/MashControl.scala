package brewcontrol

import java.time.Instant

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import rx.core.{Rx, Var}
import upickle.Js
import upickle.default._

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class Recipe(steps: List[Step])

sealed trait Step
case class HeatStep(temperature: Double) extends Step
case class RestStep(durationInMillis: Double) extends Step

sealed trait Task {
  /** @return whether this task wants to continue */
  def step(clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Boolean
}

case class HeatTask(temperature: Double, var startTime: Option[Double] = None) extends Task {
  override def step(clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Boolean = {
    if (startTime.isEmpty) startTime = Some(clock.now().toEpochMilli)
    heater() = potTemperature() < temperature
    potTemperature() < temperature
  }
}

case class RestTask(temperature: Double, durationInMillis: Double, var startTime: Option[Double] = None) extends Task {
  override def step(clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Boolean = {
    if (startTime.isEmpty) startTime = Some(clock.now().toEpochMilli)
    val timeUp = clock.now().isAfter(Instant.ofEpochMilli((startTime.get + durationInMillis).asInstanceOf[Long]))
    heater() = !timeUp && potTemperature() < temperature
    !timeUp
  }
}

/** Wraps an Actor around [[MashControlSync]] */
class MashControlActor(val recipe: Recipe, val clock: Clock, val heater: Var[Boolean], val potTemperature: Rx[Double]) extends Actor with LazyLogging {

  import MashControlActor._

  var running = false
  val impl = new MashControlSync(recipe, heater, potTemperature)

  override def receive = {
    case GetRecipe => sender ! recipe
    case GetStateAsJson => {
      val js: Js.Value = impl.toJs
      sender ! js
    }
    case Start => if (running) {
      logger.warn("Trying to start, but already running")
    } else {
      logger.info("Starting")
      running = true
      self ! Step
    }
    case Step => {
      if (running) {
        impl.step(clock)
        context.system.scheduler.scheduleOnce(5 seconds) {
          self ! Step
        }
      }
    }
  }

}

object MashControlActor {
  def props(recipe: Recipe, clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Props = Props(new MashControlActor(recipe, clock, heater, potTemperature))
  case object GetStateAsJson
  case object GetRecipe
  case object Start
  case object Step
}

class MashControlSync(val recipe: Recipe, val heater: Var[Boolean], val potTemperature: Rx[Double]) extends LazyLogging {

  val allTasks: Vector[Task] = {
    var lastTemperature: Option[Double] = None
    def toTask(step: Step): Task = step match {
      case HeatStep(t) => {
        lastTemperature = Some(t);
        HeatTask(t)
      }
      case RestStep(d) => RestTask(lastTemperature.get, d)
    }
    recipe.steps.map(toTask).toVector
  }
  var taskIndex: Int = 0

  def currentTask: Option[Task] = if (taskIndex >= allTasks.size) None else Some(allTasks(taskIndex))
  def isActive: Boolean = taskIndex < allTasks.size

  @tailrec
  final def step(clock: Clock): Unit = {
    logger.debug(s"Stepping, currentTask: $currentTask, clock: $clock")
    currentTask match {
      case None =>
      // Already done
      case Some(t) => {
        if (t.step(clock, heater, potTemperature)) {
          // Task is not done yet
        } else {
          // Advance and call step on next item immediately
          taskIndex += 1
          step(clock)
        }
      }
    }
  }

  def toJs: Js.Obj =
    Js.Obj(
      "tasks" -> writeJs(allTasks.toList),
      "currentTaskIndex" -> writeJs(taskIndex)
    )
}