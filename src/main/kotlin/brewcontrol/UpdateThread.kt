package brewcontrol

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.Executors
import java.util.concurrent.Future

interface UpdateThread {
    fun <T> runOnUpdateThread(f: () -> T): Future<T>
}

/**
 * Holds a reference to single thread executor that's intended to run state updates in a safe way.
 */
class UpdateThreadImpl : UpdateThread {
    private val tf = ThreadFactoryBuilder().setNameFormat("state-%d").build()
    private val executor = Executors.newSingleThreadExecutor(tf)

    override fun <T> runOnUpdateThread(f: () -> T): Future<T> = executor.submit(f)
}
