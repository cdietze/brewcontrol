package brewcontrol

import org.scalatest.concurrent.Eventually
import org.scalatest.{FlatSpec, Matchers}

class HistoryTest extends FlatSpec with Matchers {

  "History" should "initially be empty" in {
    val h = new History()
    assert(h.get() === Map())
  }

  it should "store my items" in {
    val h = new History()
    h.addItem("name", "double", (1L, 1D))
    Eventually.eventually {
      h.get().size === 1
    }
    h.addItem("name", "double", (2L, 2D))
    Eventually.eventually {
      h.get().size === 2
    }
  }

  it should "remove old entries" in {
    val h = new History()
    h.addItem("name", "double", (1L, 1D))
    Eventually.eventually {
      h.get().size === 1 && h.get()("name").data.size === 1
    }
    h.addItem("name", "double", (2L, 2D))
    Eventually.eventually {
      h.get().size === 1 && h.get()("name").data.size === 2
    }
    h.addItem("name", "double", (1L + History.maxItemAge, 3D))
    Eventually.eventually {
      h.get().size === 1 && h.get()("name").data.size === 2
    }
  }
}
