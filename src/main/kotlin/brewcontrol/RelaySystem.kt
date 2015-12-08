package brewcontrol

import org.slf4j.LoggerFactory
import react.Value
import react.ValueView
import java.util.*

class RelaySystem {
    val log = LoggerFactory.getLogger(RelaySystem::class.java)

    enum class Relay {
        Cooler, Heater, PotHeater
    }

    val relays = Value<Map<Relay, Boolean>>(Collections.emptyMap())

    // TODO I need a value here
    fun relayView(relay: Relay): ValueView<Boolean?> {
        return relays.map { it.get(relay) }
    }


    //    sealed class Relay(pinNumber: Int, val name: String) {
    //        val outPin = Await.result(gpio.outPin(pinNumber), 15 seconds)
    //        val value: Var[Boolean] = inversePin(outPin)
    //        value.update(false)
    //    }
    //
    //    case object Cooler extends Relay(7, "Kühlung")
    //    case object Heater extends Relay(8, "Heizung")
    //    case object PotHeater extends Relay(25, "Kessel")

}
