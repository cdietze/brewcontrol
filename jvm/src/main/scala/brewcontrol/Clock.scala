package brewcontrol

import org.joda.time.DateTime

class Clock {

  def now: DateTime = DateTime.now()
}
