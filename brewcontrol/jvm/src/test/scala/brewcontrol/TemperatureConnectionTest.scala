package brewcontrol

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success

class TemperatureConnectionTest extends FlatSpec with Matchers {

  "Temperature Connection" should "parse one liner" in {
    val con = new TemperatureConnection()
    assert(con.parseTemperature(List("t=1000")) === Success(1d))
  }
  it should "parse negative number" in {
    val con = new TemperatureConnection()
    assert(con.parseTemperature(List("t=-1000")) == Success(-1f))
  }
  it should "parse with noise" in {
    val con = new TemperatureConnection()
    assert(con.parseTemperature(List("ignoreme t=1000 ignoreme")) == Success(1f))
  }
  it should "parse multiple lines" in {
    val con = new TemperatureConnection()
    assert(con.parseTemperature(List("ignoreme", "ignoreme t=1000 ignoreme", "ignoreme")) == Success(1f))
  }
  it should "parse fraction" in {
    val con = new TemperatureConnection()
    assert(con.parseTemperature(List("t=1250")) == Success(1.25f))
  }
}
