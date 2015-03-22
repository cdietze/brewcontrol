package brewcontrol

import utest._
import utest.framework.TestSuite

import scala.util.Success

object TemperatureConnectionTest extends TestSuite {

  val tests = TestSuite {
    'parseOneLiner {
      val con = new TemperatureConnection()
      assert(con.parseTemperature(List("t=1000")) == Success(1f))
    }
    'parseNegativeNumber {
      val con = new TemperatureConnection()
      assert(con.parseTemperature(List("t=-1000")) == Success(-1f))
    }
    'parseWithNoise {
      val con = new TemperatureConnection()
      assert(con.parseTemperature(List("ignoreme t=1000 ignoreme")) == Success(1f))
    }
    'parseMultipleLines {
      val con = new TemperatureConnection()
      assert(con.parseTemperature(List("ignoreme", "ignoreme t=1000 ignoreme", "ignoreme")) == Success(1f))
    }
    'parseFraction {
      val con = new TemperatureConnection()
      assert(con.parseTemperature(List("t=1250")) == Success(1.25f))
    }
  }
}
