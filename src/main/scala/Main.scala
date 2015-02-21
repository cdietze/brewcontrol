import rx._
import rx.core.Var
import rx.ops._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App {

  println("Hello")

  implicit val scheduler = new AkkaScheduler(akka.actor.ActorSystem())
  val gpio = new Gpio()

  val temp = gpio.temperatureSensor("28-031462078cff")

  val timer = Timer(1500 millis)

  val o = temp.foreach { t => println(s"temp is now $t")}

  while (true) {
    println(s"waiting...")
    Thread.sleep(2000)
  }
}

object RxUtils {
  def window[T](source: Rx[T], count: Int): Rx[Seq[T]] = {
    val result = Var(List[T]())
    source.foreach {
      v => result() = v :: result().take(count - 1)
    }
    result
  }
}
