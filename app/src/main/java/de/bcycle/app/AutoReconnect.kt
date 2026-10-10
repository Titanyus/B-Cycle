package de.bcycle.app

/** Foreground-only retries; scans, manual stop and the export picker suspend them. */
class AutoReconnect {
    var enabled = true
    var foreground = false
    var selecting = false
    var exporting = false
    private var retryAt = 0L
    private var delayMillis = 3_000L

    fun due(now: Long, connectionActive: Boolean) = enabled && foreground && !selecting && !exporting &&
        !connectionActive && now >= retryAt

    fun attempted(now: Long) {
        retryAt = now + delayMillis
        delayMillis = minOf(delayMillis * 2, 30_000)
    }

    fun connected() { retryAt = 0; delayMillis = 3_000 }
    fun start() { enabled = true; selecting = false; connected() }
}
