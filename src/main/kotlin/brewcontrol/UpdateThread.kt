package brewcontrol

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.Executors

/**
 * Holds a reference to single thread executor that's intended to run state updates in a safe way.
 */
object UpdateThread {
    private val tf = ThreadFactoryBuilder().setNameFormat("state-%d").build()
    val executor = Executors.newSingleThreadExecutor(tf)
}