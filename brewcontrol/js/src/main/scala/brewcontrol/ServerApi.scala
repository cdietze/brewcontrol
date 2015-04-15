package brewcontrol

import org.scalajs.dom.ext.Ajax
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.concurrent.Future

object ServerApi {
  val baseUrl = ""
  //  val baseUrl = "http://pi:8080"

  def temperatures(): Future[List[Reading]] = {
    Ajax.get(s"$baseUrl/temperatures").map(xhr => {
      upickle.read[List[Reading]](xhr.responseText)
    })
  }

  def relayStates(): Future[List[RelayState]] = {
    Ajax.get(s"$baseUrl/relays").map(xhr => {
      upickle.read[List[RelayState]](xhr.responseText)
    })
  }

  def updateTargetTemperature(value: Float): Future[String] = {
    Ajax.post(s"$baseUrl/targetTemperature", upickle.write(value)).map(xhr => xhr.responseText)
  }

  def targetTemperature(): Future[Float] = {
    Ajax.get(s"$baseUrl/targetTemperature").map(xhr => upickle.read[Float](xhr.responseText))
  }

  def history(hourTimestamp: Long): Future[Seq[SeriesData]] = {
    Ajax.get(s"$baseUrl/history/hour/$hourTimestamp").map(xhr => {
      upickle.read[Seq[SeriesData]](xhr.responseText)
    })
  }
}
