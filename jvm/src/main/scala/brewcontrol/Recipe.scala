package brewcontrol

import java.time.{Duration, Instant}

import rx.core.{Rx, Var}

import scala.annotation.tailrec

case class Recipe(stages: List[Stage])

case class Stage(temperature: Double, duration: Duration)

trait Task {
  /** @return whether this task wants to continue */
  def step(clock: Clock): Boolean
}

abstract class BrewProcess(val recipe: Recipe) {

  implicit def heater: Var[Boolean]
  implicit def potTemperature: Rx[Double]

  private var tasks: Seq[Task] = buildTaskSeq()

  var currentTask: Option[Task] = tasks.headOption

  def isActive: Boolean = tasks.nonEmpty

  @tailrec
  final def step(clock: Clock): Unit = {
    tasks.headOption match {
      case None =>
      case Some(t) => {
        if (!t.step(clock)) {
          // Advance and call step on next item immediately
          tasks = tasks.tail
          step(clock)
        }
      }
    }
  }

  private def buildTaskSeq(): Seq[Task] = {
    def toTasks(stage: Stage): Seq[Task] = {
      List(HeatTask(stage), RestTask(stage))
    }
    recipe.stages.flatMap(toTasks)
  }

  case class HeatTask(stage: Stage) extends Task {
    var startTime: Option[Instant] = None

    override def step(clock: Clock): Boolean = {
      if (startTime.isEmpty) startTime = Some(clock.now())
      heater() = potTemperature() < stage.temperature
      potTemperature() < stage.temperature
    }
  }

  case class RestTask(stage: Stage) extends Task {
    var startTime: Option[Instant] = None

    override def step(clock: Clock): Boolean = {
      if (startTime.isEmpty) startTime = Some(clock.now())
      val timeUp = clock.now().isAfter(startTime.get.plus(stage.duration))
      heater() = !timeUp && potTemperature() < stage.temperature
      !timeUp
    }
  }
}