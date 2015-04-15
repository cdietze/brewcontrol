package brewcontrol

/** hour is the timestamp when this hour started (in milliseconds since the epoch)
  * values contains timed data for that hour. Maps seconds to values */
case class HourTimeData(hourTimestamp: Long, values: List[(Int, Float)])

sealed trait SeriesKind
case object Temperature extends SeriesKind
case object Relay extends SeriesKind

case class SeriesData(seriesId: String, kind: SeriesKind, hourTimeData: HourTimeData)