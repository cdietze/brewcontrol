package brewcontrol

/** hour is the timestamp when this hour started (in milliseconds since the epoch)
  * values contains timed data for that hour. Maps seconds to values */
case class HourTimeData(hourTimestamp: Long, values: Map[String, Float]) {

}
