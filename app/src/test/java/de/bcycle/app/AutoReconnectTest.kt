package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class AutoReconnectTest {
    @Test fun onlyConnectsInForegroundWithoutAnActiveConnection() {
        val policy = AutoReconnect()
        assertFalse(policy.due(0, false))
        policy.foreground = true
        assertTrue(policy.due(0, false))
        assertFalse(policy.due(0, true))
        policy.foreground = false
        assertFalse(policy.due(100_000, false))
    }

    @Test fun retriesBackOffAndSuccessfulConnectionResetsDelay() {
        val policy = AutoReconnect().apply { foreground = true }
        policy.attempted(0)
        assertFalse(policy.due(2999, false)); assertTrue(policy.due(3000, false))
        policy.attempted(3000)
        assertFalse(policy.due(8999, false)); assertTrue(policy.due(9000, false))
        policy.attempted(9000); policy.attempted(21_000); policy.attempted(45_000)
        assertFalse(policy.due(74_999, false)); assertTrue(policy.due(75_000, false))
        policy.connected(); policy.attempted(80_000)
        assertFalse(policy.due(82_999, false)); assertTrue(policy.due(83_000, false))
    }

    @Test fun stopSelectionAndExportCannotBeUndoneByForegroundOrRetries() {
        val policy = AutoReconnect().apply { foreground = true }
        policy.enabled = false
        policy.foreground = false; policy.foreground = true
        assertFalse(policy.due(100_000, false))
        policy.start(); policy.selecting = true
        assertFalse(policy.due(100_000, false))
        policy.start(); policy.exporting = true
        assertFalse(policy.due(100_000, false))
        policy.exporting = false
        assertTrue(policy.due(100_000, false))
    }
}
