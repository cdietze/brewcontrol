import org.scalajs.dom
import rx._

import scala.scalajs.js
import scalatags.JsDom.all._

package object brewcontrol {

  implicit def rxFrag[T <% Frag](r: Rx[T]): Frag = {
    def rSafe: dom.Node = span(r()).render
    var last = rSafe
    Obs(r, skipInitial = true){
      val newLast = rSafe
      js.Dynamic.global.last = last
      last.parentNode.replaceChild(newLast, last)
      last = newLast
    }
    last
  }
}
