package brewcontrol

import java.time.Instant

import org.scalatest.{FlatSpec, Matchers}

class RecipeTest extends FlatSpec with Matchers {

  "Empty recipe" should "complete immediately" in {

    val clock = MockClock(Instant.now)

    val task = Recipe(List()).startProcess(clock)

    task.step(clock) === None
  }

  case class MockClock(var current: Instant) extends Clock {

    override def now = current
  }

}
