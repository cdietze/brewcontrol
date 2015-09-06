package brewcontrol

import java.time.{Duration, Instant}

import org.scalatest.{FlatSpec, Matchers}
import rx.core.Var
import upickle.default._

import scala.concurrent.duration._

class RecipeTest extends FlatSpec with Matchers {

  "Empty recipe" should "be inactive and do nothing" in {

    val clock = MockClock(Instant.now)

    val heater = Var[Boolean](false)
    val potTemperature = Var[Double](10d)
    val process = new BrewProcessSync(Recipe(List()), heater, potTemperature)

    assert(process.isActive === false)
    process.step(clock)
    assert(process.isActive === false)
  }

  "Recipe with single stage" should "heat up and rest accordingly" in {

    val clock = MockClock(Instant.now)

    val heater = Var[Boolean](false)
    val potTemperature = Var[Double](10d)
    val process = new BrewProcessSync(Recipe(List(Stage(64d, (30 minutes).toMillis))), heater, potTemperature)

    assert(process.isActive === true)
    process.step(clock)
    assert(process.isActive === true)
    assert(process.heater() === true)

    clock.add(Duration.ofMinutes(10))
    process.step(clock)
    assert(process.isActive === true)
    assert(process.heater() === true)

    clock.add(Duration.ofMinutes(10))
    potTemperature() = 65d
    process.step(clock)
    assert(process.isActive === true)
    assert(process.heater() === false)

    clock.add(Duration.ofMinutes(16))
    potTemperature() = 63d
    process.step(clock)
    assert(process.isActive === true)
    assert(process.heater() === true)

    clock.add(Duration.ofMinutes(16))
    potTemperature() = 63d
    process.step(clock)
    assert(process.isActive === false)
    assert(process.heater() === false)
  }

  "BrewProcess" should "serialize" in {

    val heater = Var[Boolean](false)
    val potTemperature = Var[Double](10d)
    val process = new BrewProcessSync(Recipe(
      Stage(64d, (30 minutes).toMillis) :: Stage(72d, (45 minutes).toMillis) :: Nil
    ), heater, potTemperature)

    val t: HeatTask = HeatTask(Stage(64d, (32 minutes).toMillis))
    t.startTime = Some(17)
    println(s"task: ${write(t)}")
    println(s"process: ${process.toJs}")
  }

  case class MockClock(var instant: Instant) extends Clock {
    override def now() = instant
    def add(d: Duration) = {
      instant = instant.plus(d)
    }
  }
}
