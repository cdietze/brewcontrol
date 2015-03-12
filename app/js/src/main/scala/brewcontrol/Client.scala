package brewcontrol

import org.scalajs.dom
import org.scalajs.dom.html

import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
object Client {
  @JSExport
  def main(container: html.Div) = {
    val inputBox = input.render
    val outputBox = ul.render
    inputBox.onkeyup = (e: dom.Event) => outputBox.innerHTML = inputBox.value
    container.appendChild(
      div(
        h1("Hi from ScalaJS"),
        inputBox,
        outputBox
      ).render
    )
  }
}
