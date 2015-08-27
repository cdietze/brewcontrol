package brewcontrol

import java.time.{Duration, Instant}

import rx.core.{Rx, Var}

case class Stage(temperature: Double, duration: Duration)

case class Recipe(stages: List[Stage])

abstract class BrewProcess(val recipe: Recipe) {

  implicit def heater: Var[Boolean]
  implicit def potTemperature: Rx[Double]

  var currentTask: Option[Task] = buildTaskChain()

  def isActive = currentTask.isDefined

  def step(clock: Clock): Unit = {
    for (t <- currentTask) {
      currentTask = t.step(clock)
    }
  }

  private def buildTaskChain(): Option[Task] = {
    def toTasks(stage: Stage): Seq[Task] = {
      List(HeatTask(stage), RestTask(stage))
    }
    Some(SeqTask(recipe.stages.flatMap(toTasks)))
  }

  case class HeatTask(stage: Stage) extends Task {
    var startTime: Option[Instant] = None

    override def step(clock: Clock) = {
      if (startTime.isEmpty) startTime = Some(clock.now)
      heater() = (potTemperature() < stage.temperature)
      if (potTemperature() < stage.temperature) {
        Some(this)
      } else {
        None
      }
    }
  }

  case class RestTask(stage: Stage) extends Task {
    var startTime: Option[Instant] = None

    override def step(clock: Clock) = {
      if (startTime.isEmpty) startTime = Some(clock.now)
      heater() = (potTemperature() < stage.temperature)
      if (clock.now.isAfter(startTime.get.plus(stage.duration))) {
        // rested long enough
        heater() = false
        None
      } else {
        Some(this)
      }
    }
  }
}

trait Task {
  def step(clock: Clock): Option[Task]
}

case class SeqTask(tasks: Seq[Task]) extends Task {
  def step(clock: Clock) = {
    tasks.headOption match {
      case Some(t) => {
        t.step(clock) match {
          case Some(t2) => Some(SeqTask(t2 +: tasks.tail))
          case None => SeqTask(tasks.tail).step(clock) // Advance and call step on next item immediately
        }
      }
      case None => None
    }
  }
}


