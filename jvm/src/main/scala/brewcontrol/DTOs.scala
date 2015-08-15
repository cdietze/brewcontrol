package brewcontrol

case class Reading(timestamp: Long, sensorId: String, name: String, value: Double)

case class RelayState(name: String, value: Boolean)
