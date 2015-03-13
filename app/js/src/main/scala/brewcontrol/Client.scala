package brewcontrol

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html
import rx.core.Var

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
object Client {

  val sensorA: Var[String] = Var("unknown")

  @JSExport
  def main(container: html.Div) = {
    val inputBox = input.render
    val outputBox = ul.render
    val btn = button("My Button").render
    inputBox.onkeyup = (e: dom.Event) => outputBox.innerHTML = inputBox.value

    def updateSensorA: (() => Unit) = () => {
      Ajax.get("/temperatures/SensorA").foreach(xhr => {
        val reading = upickle.read[Reading](xhr.responseText)
        sensorA() = reading.toString
        dom.window.setTimeout(updateSensorA, 5000)
      })
    }
    updateSensorA()

    container.appendChild(
      div(
        h1("Hi from ScalaJS"),
        inputBox,
        outputBox,
        btn,
        sensorA
      ).render
    )
  }
}
