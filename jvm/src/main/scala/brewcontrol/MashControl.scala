package brewcontrol

import java.time.Instant

import akka.actor.{Actor, Props}
import rx.core.{Rx, Var}
import upickle.Js
import upickle.default._

import scala.annotation.tailrec

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
class MashControlActor(val recipe: Recipe, val heater: Var[Boolean], val potTemperature: Rx[Double]) extends Actor {

  import MashControlActor._

  val impl = new MashControlSync(recipe, heater, potTemperature)

  override def receive = {
    case GetRecipe => sender ! recipe
    case GetStateAsJson => {
      val js: Js.Value = impl.toJs
      sender ! js
    }
    case Step(clock) => impl.step(clock)
  }

}

object MashControlActor {
  def props(recipe: Recipe, heater: Var[Boolean], potTemperature: Rx[Double]): Props = Props(new MashControlActor(recipe, heater, potTemperature))
  case object GetStateAsJson
  case object GetRecipe
  case class Step(var clock: Clock)
}

class MashControlSync(val recipe: Recipe, val heater: Var[Boolean], val potTemperature: Rx[Double]) {

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