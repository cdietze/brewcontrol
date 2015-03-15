package brewcontrol

import brewcontrol.HourTimeData.Links

/** hour is the timestamp when this hour started (in milliseconds since the epoch)
  * values contains timed data for that hour. Maps seconds to values */
case class HourTimeData(hourTimestamp: Long, values: List[(Int, Float)], links: Option[Links] = None)

object HourTimeData {

  case class Links(`this`: String, prev: String)

}
