package brewcontrol

import java.time.{Duration, Instant}

import org.scalatest.{FlatSpec, Matchers}
import rx.core.Var

class RecipeTest extends FlatSpec with Matchers {

  "Empty recipe" should "complete immediately" in {

    val clock = MockClock(Instant.now)

    val process = new BrewProcess(Recipe(List())) {
      override val heater = Var[Boolean](false)
      override val potTemperature = Var[Double](10d)
    }

    assert(process.isActive === true)
    process.step(clock)
    assert(process.isActive === false)
  }

  "Recipe with single stage" should "heat up and rest accordingly" in {

    val clock = MockClock(Instant.now)

    val stage = Stage(64d, Duration.ofMinutes(30))

    val process = new BrewProcess(Recipe(List(stage))) {
      override val heater = Var[Boolean](false)
      override val potTemperature = Var[Double](10d)
    }

    assert(process.isActive === true)
    process.step(clock)
    assert(process.isActive === true)
    assert(process.heater() === true)

    clock.add(Duration.ofMinutes(10))
    process.step(clock)
    assert(process.isActive === true)
    assert(process.heater() === true)

    clock.add(Duration.ofMinutes(10))
    process.potTemperature() = 65d
    process.step(clock)
    assert(process.isActive === true)
    assert(process.heater() === false)

    clock.add(Duration.ofMinutes(16))
    process.potTemperature() = 63d
    process.step(clock)
    assert(process.isActive === true)
    assert(process.heater() === true)

    clock.add(Duration.ofMinutes(16))
    process.potTemperature() = 63d
    process.step(clock)
    assert(process.isActive === false)
    assert(process.heater() === false)
  }

  case class MockClock(var instant: Instant) extends Clock {
    override def now() = instant
    def add(d: Duration) = {
      instant = instant.plus(d)
    }
  }
}
