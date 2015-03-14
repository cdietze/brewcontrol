import org.joda.time.DateTime

package object brewcontrol {
  implicit def longToDateTime(l: Long) = new DateTime(l)
  implicit def dateTimeToLong(d: DateTime) = d.getMillis
}
