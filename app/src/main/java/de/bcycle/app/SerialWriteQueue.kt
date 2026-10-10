package de.bcycle.app

import java.util.ArrayDeque

/** One GATT write at a time; queued startup replies may precede pending measurement requests. */
class SerialWriteQueue<T>(
    private val send: (T) -> Boolean,
    private val schedule: (Runnable, Long) -> Unit,
    private val cancel: (Runnable) -> Unit,
    private val changed: () -> Unit,
    private val failed: (String) -> Unit,
) {
    private val pending = ArrayDeque<T>()
    private var enabled = false
    private var stopped = false
    private var inFlight = false
    private val pump = Runnable { drain() }
    private val timeout = Runnable { abort("GATT-Schreibbestätigung fehlt") }
    val idle: Boolean get() = !inFlight && pending.isEmpty()

    fun enqueue(items: List<T>, urgent: Boolean = false) {
        if (stopped || items.isEmpty()) return
        if (pending.size + items.size + (if (inFlight) 1 else 0) > 16) {
            abort("Zu viele ausstehende Bosch-Datenanfragen")
            return
        }
        if (urgent) for (item in items.asReversed()) pending.addFirst(item)
        else pending.addAll(items)
        changed()
        if (enabled && !inFlight) {
            cancel(pump)
            schedule(pump, if (urgent) 0 else 300)
        }
    }

    fun start() { if (!stopped) { enabled = true; drain() } }

    fun complete(success: Boolean) {
        if (stopped || !inFlight) return
        cancel(timeout)
        inFlight = false
        if (!success) { abort("Bosch-Datenanfrage konnte nicht gesendet werden"); return }
        changed()
        if (pending.isNotEmpty()) schedule(pump, 300)
    }

    fun stop() {
        stopped = true; enabled = false; inFlight = false
        pending.clear()
        cancel(pump); cancel(timeout)
    }

    private fun drain() {
        if (stopped || !enabled || inFlight || pending.isEmpty()) return
        val next = pending.removeFirst()
        inFlight = true
        schedule(timeout, 5_000)
        changed()
        if (!send(next)) abort("Android konnte die Bosch-Datenanfrage nicht starten")
    }

    private fun abort(reason: String) { if (!stopped) { stop(); failed(reason) } }
}
