package brewcontrol

import org.scalajs.dom.ext.Ajax
import rx._
import rx.core.Var

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import upickle.default._

object ServerApi {
  val baseUrl = ""
  //  val baseUrl = "http://pi:8080"

  def temperatures(): Future[List[Reading]] = {
    Ajax.get(s"$baseUrl/temperatures").map(xhr => {
      read[List[Reading]](xhr.responseText)
    })
  }

  def relayStates(): Future[List[RelayState]] = {
    Ajax.get(s"$baseUrl/relays").map(xhr => {
      read[List[RelayState]](xhr.responseText)
    })
  }

  def history(hourTimestamp: Long): Future[Seq[SeriesData]] = {
    Ajax.get(s"$baseUrl/history/hour/$hourTimestamp").map(xhr => {
      read[Seq[SeriesData]](xhr.responseText)
    })
  }

  val targetTemperature: Var[Float] = ServerApi.createVarSync("/targetTemperature", 0f)
  val heaterEnabled: Var[Boolean] = ServerApi.createVarSync("/heaterEnabled", false)
  val coolerEnabled: Var[Boolean] = ServerApi.createVarSync("/coolerEnabled", false)

  private def createVarSync[T](path: String, initialValue: T)(implicit rw: ReadWriter[T]): Var[T] = {
    val url = s"${baseUrl}${path}"
    new Var(initialValue) {
      val o = Ajax.get(url).map(xhr => {
        val v = read(xhr.responseText)(rw)
        this.update(v)
        // start propagating updates of the Var after we received the initial state
        Obs(this, skipInitial = true) {
          Ajax.post(url, write(this()))
        }
      })
    }
  }
}
