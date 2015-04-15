package brewcontrol

import org.scalajs.dom.ext.Ajax

import scala.concurrent.Future

object ServerApi {
  val baseUrl = ""
  //  val baseUrl = "http://pi:8080"

  def temperatures(): Future[List[Reading]] = {
    Ajax.get(s"$baseUrl/temperatures").map(xhr => {
      upickle.read[List[Reading]](xhr.responseText)
    })
  }

  def temperatureHourData(sensorId: String, hourTimestamp: Long): Future[HourTimeData] = {
    Ajax.get(s"$baseUrl/temperatures/$sensorId/hour/$hourTimestamp").map(xhr => {
      upickle.read[HourTimeData](xhr.responseText)
    })
  }

  def relayStates(): Future[List[RelayState]] = {
    Ajax.get(s"$baseUrl/relays").map(xhr => {
      upickle.read[List[RelayState]](xhr.responseText)
    })
  }

  def relayHourData(relayName: String, hourTimestamp: Long): Future[HourTimeData] = {
    Ajax.get(s"$baseUrl/relays/$relayName/hour/$hourTimestamp").map(xhr => {
      upickle.read[HourTimeData](xhr.responseText)
    })
  }

  def updateTargetTemperature(value: Float): Future[String] = {
    Ajax.post(s"$baseUrl/targetTemperature", upickle.write(value)).map(xhr => xhr.responseText)
  }

  def targetTemperature(): Future[Float] = {
    Ajax.get(s"$baseUrl/targetTemperature").map(xhr => upickle.read[Float](xhr.responseText))
  }
}
