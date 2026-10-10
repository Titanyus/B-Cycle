package de.bcycle.app

import java.util.IdentityHashMap

/** Let Android finish disconnecting before unregistering the GATT client. Main-thread only. */
class DeferredConnectionClose<T : Any>(
    private val disconnect: (T) -> Unit,
    private val close: (T) -> Unit,
    private val schedule: (Runnable, Long) -> Unit,
    private val cancel: (Runnable) -> Unit,
) {
    private val pending = IdentityHashMap<T, Runnable>()
    val isEmpty: Boolean get() = pending.isEmpty()

    fun release(connection: T) {
        if (pending.containsKey(connection)) return
        // Rapid mode switches must not accumulate an unbounded number of clients.
        if (pending.size >= 4) finish(pending.keys.first())
        val timeout = Runnable { finish(connection) }
        pending[connection] = timeout
        schedule(timeout, 20_000)
        disconnect(connection)
    }

    fun onDisconnected(connection: T): Boolean {
        if (!pending.containsKey(connection)) return false
        finish(connection)
        return true
    }

    fun closeAll() { for (connection in pending.keys.toList()) finish(connection) }

    private fun finish(connection: T) {
        val timeout = pending.remove(connection) ?: return
        cancel(timeout)
        close(connection)
    }
}
