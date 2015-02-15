import framboos.InPin

object Main extends App {

  println("Hello")

  val input = InPin(0)

  while (true) {
    println(s"input value: ${input.value}")
    Thread.sleep(1000)
  }
}
