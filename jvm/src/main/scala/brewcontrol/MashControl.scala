package brewcontrol

import java.time.Instant

import akka.actor.{Actor, Props}
import rx.core.{Rx, Var}
import upickle.Js
import upickle.default._

import scala.annotation.tailrec

case class Recipe(stages: List[Stage])

case class Stage(temperature: Double, durationInMillis: Double)

sealed trait Step
case class HeatStep(temperature: Double) extends Step
case class RestStep(duration: Double) extends Step

sealed trait Task {
  /** @return whether this task wants to continue */
  def step(clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Boolean
}

case class HeatTask(stage: Stage, var startTime: Option[Double] = None) extends Task {

  override def step(clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Boolean = {
    if (startTime.isEmpty) startTime = Some(clock.now().toEpochMilli)
    heater() = potTemperature() < stage.temperature
    potTemperature() < stage.temperature
  }
}

case class RestTask(stage: Stage, var startTime: Option[Double] = None) extends Task {

  override def step(clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Boolean = {
    if (startTime.isEmpty) startTime = Some(clock.now().toEpochMilli)
    val timeUp = clock.now().isAfter(Instant.ofEpochMilli((startTime.get + stage.durationInMillis).asInstanceOf[Long]))
    heater() = !timeUp && potTemperature() < stage.temperature
    !timeUp
  }
}

/** Wraps an Actor around [[MashControlSync]] */
class MashControlActor(val recipe: Recipe, val heater: Var[Boolean], val potTemperature: Rx[Double]) extends Actor {

  import MashControlActor._

  val impl = new MashControlSync(recipe, heater, potTemperature)

  override def receive = {
    case GetStateAsJson => {
      val js: Js.Value = impl.toJs
      sender ! js
    }
    case Step(clock) => impl.step(clock)
  }

}

object MashControlActor {
  def props(recipe: Recipe, heater: Var[Boolean], potTemperature: Rx[Double]): Props = Props(new MashControlActor(recipe, heater, potTemperature))
  case class GetStateAsJson()
  case class Step(var clock: Clock)
}

class MashControlSync(val recipe: Recipe, val heater: Var[Boolean], val potTemperature: Rx[Double]) {

  val allTasks: Vector[Task] = {
    def toTasks(stage: Stage): List[Task] = List(HeatTask(stage), RestTask(stage))
    recipe.stages.flatMap(toTasks).toVector
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
    Js.Obj(("tasks", writeJs(allTasks.toList)))
}