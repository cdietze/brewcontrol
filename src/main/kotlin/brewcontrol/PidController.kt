package brewcontrol

import react.ValueView
import react.Values

/**
 * Takes a input and goal value and returns a view of the error
 *
 * This is not really a PID controller but only a P controller.
 *
 * @see [Wikipedia][http://en.wikipedia.org/wiki/PID_controller#PID_controller_theory]
 */
fun pidController(input: ValueView<Double?>, target: ValueView<Double?>): ValueView<Double> {
    fun calcOutput(input: Double?, target: Double?): Double {
        if (input == null || target == null) return 0.0
        val Kp = 1.0
        val error = target - input
        val output = Kp * error
        log.trace("PID calculation: input=$input, target=$target, output=$output")
        return output
    }
    // Do note that map is called for old and new value. Thus, calcOutput gets called twice on every update
    return Values.join(input, target).map { j -> calcOutput(j.a, j.b) }
}
