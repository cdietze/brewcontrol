package brewcontrol

import java.time.Instant

import akka.actor.{Actor, Cancellable, Props}
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
case object HoldStep extends Step

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

case class HoldTask(temperature: Double, var startTime: Option[Double] = None) extends Task {
  override def step(clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Boolean = {
    if (startTime.isEmpty) startTime = Some(clock.now().toEpochMilli)
    heater() = potTemperature() < temperature
    true
  }
}

/** Wraps an Actor around [[MashControlSync]] */
class MashControlActor(val recipe: Recipe, val clock: Clock, val heater: Var[Boolean], val potTemperature: Rx[Double]) extends Actor with LazyLogging {

  import MashControlActor._

  var cancellable: Option[Cancellable] = None
  var impl = new MashControlSync(recipe, clock, heater, potTemperature)

  override def receive = {
    case GetRecipe => sender ! recipe
    case GetStateAsJson => {
      val js: Js.Value = impl.toJs
      sender ! js
    }
    case Start => cancellable match {
      case Some(c) => logger.warn("Trying to start, but already running")
      case None => {
        logger.info("Starting")
        cancellable = Some(context.system.scheduler.schedule(0 seconds, 5 seconds, self, Step))
      }
    }
    case Step => {
      impl.step()
    }
    case Skip => {
      impl.skip()
      // When running, step immediately to start the next task
      if (cancellable.isDefined) impl.step()
    }
    case Reset => {
      logger.info(s"Resetting")
      cancellable.foreach(_.cancel())
      cancellable = None
      heater() = false
      impl = new MashControlSync(recipe, clock, heater, potTemperature)
    }
  }

  override def postStop() = cancellable.foreach(_.cancel())
}

object MashControlActor {
  def props(recipe: Recipe, clock: Clock, heater: Var[Boolean], potTemperature: Rx[Double]): Props = Props(new MashControlActor(recipe, clock, heater, potTemperature))
  case object GetStateAsJson
  case object GetRecipe
  case object Start
  case object Step
  case object Skip
  case object Reset
}

class MashControlSync(val recipe: Recipe, val clock: Clock, val heater: Var[Boolean], val potTemperature: Rx[Double]) extends LazyLogging {
  require(!recipe.steps.isEmpty)

  val allTasks: Vector[Task] = {
    var lastTemperature: Option[Double] = None
    def toTask(step: Step): Task = step match {
      case HeatStep(t) => {
        lastTemperature = Some(t)
        HeatTask(t)
      }
      case RestStep(d) => RestTask(lastTemperature.get, d)
      case HoldStep => HoldTask(lastTemperature.get)
    }
    recipe.steps.map(toTask).toVector
  }
  var taskIndex: Int = 0

  def currentTask: Task = allTasks(taskIndex)

  @tailrec
  final def step(): Unit = {
    logger.debug(s"Stepping, currentTask: $currentTask, clock: $clock")
    if (currentTask.step(clock, heater, potTemperature)) {
      // Task is not done yet
    } else {
      // Advance and call step on next item immediately
      logger.debug(s"Task $currentTask is done, moving to next task: ${allTasks(taskIndex + 1)}")
      taskIndex += 1
      step()
    }
  }

  /** Skips over the current task. */
  def skip(): Unit = {
    if (taskIndex + 1 >= allTasks.size) {
      logger.info(s"Cannot skip beyond last task, currentTask: $currentTask")
    } else {
      logger.info(s"Skipping, old task: $currentTask, new task: ${allTasks(taskIndex + 1)}")
      taskIndex += 1
    }
  }

  def toJs: Js.Obj =
    Js.Obj(
      "tasks" -> writeJs(allTasks.toList),
      "currentTaskIndex" -> writeJs(taskIndex)
    )
}