package brewcontrol

import com.typesafe.config.{Config => Conf, ConfigFactory}

class Settings(config: Conf) {

  val foo = config.getString("simple-lib.foo")
  val bar = config.getInt("simple-lib.bar")
}

