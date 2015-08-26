package brewcontrol

import java.time.{Duration, Instant}

case class Stage(temperature: Double, duration: Duration)

case class Recipe(stages: List[Stage]) {

  def startProcess(clock: Clock): Task = {
    def toTasks(stage: Stage): Seq[Task] = {
      List(HeatTask(stage), RestTask(stage))
    }
    SeqTask(stages.flatMap(toTasks))
  }
}

trait Task {
  def step(clock: Clock): Option[Task]
}

case class SeqTask(tasks: Seq[Task]) extends Task {
  def step(clock: Clock) = {
    tasks.headOption match {
      case Some(t) => Some(SeqTask(t.step(clock).toSeq ++ tasks.tail))
      case None => None
    }
  }
}

case class HeatTask(stage: Stage) extends Task {
  var startTime: Option[Instant] = None

  def temperatureReached(): Boolean = ???

  override def step(clock: Clock) = {
    if (startTime.isEmpty) startTime = Some(clock.now)
    if (temperatureReached()) {
      None
    } else {
      // TODO if temperature not reached turn heater relay on
      Some(this)
    }
  }
}

case class RestTask(stage: Stage) extends Task {
  var startTime: Option[Instant] = None

  override def step(clock: Clock) = {
    if (startTime.isEmpty) startTime = Some(clock.now)
    // TODO set heater relay to match stage.temperature as good as possible
    if (clock.now.isAfter(startTime.get.plus(stage.duration))) {
      // rest is up
      None
    } else {
      Some(this)
    }
  }
}
