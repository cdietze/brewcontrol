package brewcontrol

import java.time.{Duration, Instant}

import org.scalatest.{FlatSpec, Matchers}
import rx.core.Var
import upickle.default._

import scala.concurrent.duration._

class RecipeTest extends FlatSpec with Matchers {

  "Recipe with single stage" should "heat up and rest accordingly" in {

    val clock = MockClock(Instant.now)
    val heater = Var[Boolean](false)
    val potTemperature = Var[Double](10d)
    val process = new MashControlSync(Recipe(List(HeatStep(64d), RestStep((30 minutes).toMillis), HoldStep)), clock, heater, potTemperature)

    process.step()
    assert(process.heater() === true)

    clock.add(Duration.ofMinutes(10))
    process.step()
    assert(process.heater() === true)

    // finish heat task
    clock.add(Duration.ofMinutes(10))
    potTemperature() = 65d
    process.step()
    assert(process.heater() === false)

    // rest task is active
    clock.add(Duration.ofMinutes(16))
    potTemperature() = 61d
    process.step()
    assert(process.heater() === true)

    clock.add(Duration.ofMinutes(16))
    potTemperature() = 61d
    process.step()
    assert(process.heater() === true)
  }

  "BrewProcess" should "serialize" in {

    val clock = MockClock(Instant.now)
    val heater = Var[Boolean](false)
    val potTemperature = Var[Double](10d)
    val mashControl = new MashControlSync(Recipe(List(
      HeatStep(64d), RestStep((30 minutes).toMillis), HeatStep(72d), RestStep((45 minutes).toMillis)
    )), clock, heater, potTemperature)

    val t: HeatTask = HeatTask(64d)
    t.startTime = Some(17)
    println(s"task: ${write(t)}")
    println(s"mashControl: ${mashControl.toJs}")
  }

  case class MockClock(var instant: Instant) extends Clock {
    override def now() = instant
    def add(d: Duration) = {
      instant = instant.plus(d)
    }
  }
}
