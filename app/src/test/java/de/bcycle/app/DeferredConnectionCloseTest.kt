package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class DeferredConnectionCloseTest {
    private class Harness {
        val disconnected = mutableListOf<Any>()
        val closed = mutableListOf<Any>()
        val scheduled = linkedMapOf<Runnable, Long>()
        val release = DeferredConnectionClose<Any>(
            disconnect = { disconnected.add(it) },
            close = { closed.add(it) },
            schedule = { task, delay -> scheduled[task] = delay },
            cancel = { scheduled.remove(it) },
        )
    }

    @Test fun clientIsUnregisteredOnlyAfterAndroidConfirmsDisconnect() {
        val test = Harness()
        val connection = Any()
        test.release.release(connection)
        test.release.release(connection)
        assertEquals(listOf(connection), test.disconnected)
        assertTrue(test.closed.isEmpty())
        assertFalse(test.release.isEmpty)
        assertTrue(test.release.onDisconnected(connection))
        assertEquals(listOf(connection), test.closed)
        assertTrue(test.scheduled.isEmpty())
        assertTrue(test.release.isEmpty)
        assertFalse(test.release.onDisconnected(connection))
    }

    @Test fun lostCallbackHasBoundedCleanupAndLateCallbackCannotCloseANewClient() {
        val test = Harness()
        val oldConnection = Any()
        val currentConnection = Any()
        test.release.release(oldConnection)
        val timeout = test.scheduled.keys.single()
        assertEquals(20_000L, test.scheduled.values.single())
        timeout.run()
        test.release.release(currentConnection)
        timeout.run()
        assertFalse(test.release.onDisconnected(oldConnection))
        assertEquals(listOf(oldConnection), test.closed)
        assertFalse(test.release.isEmpty)
        assertTrue(test.release.onDisconnected(currentConnection))
        assertEquals(listOf(oldConnection, currentConnection), test.closed)
    }

    @Test fun activityDestructionClosesEverythingAndCancelsAllTimeouts() {
        val test = Harness()
        val first = Any()
        val second = Any()
        test.release.release(first)
        test.release.release(second)
        test.release.closeAll()
        assertEquals(setOf(first, second), test.closed.toSet())
        assertTrue(test.scheduled.isEmpty())
        assertTrue(test.release.isEmpty)
        test.release.closeAll()
        assertEquals(2, test.closed.size)
    }

    @Test fun rapidSwitchingKeepsAtMostFourOutstandingClients() {
        val test = Harness()
        repeat(10) { test.release.release(Any()) }
        assertEquals(4, test.scheduled.size)
        assertEquals(6, test.closed.size)
        test.release.closeAll()
        assertEquals(10, test.closed.size)
        assertEquals(10, test.closed.toSet().size)
        assertTrue(test.scheduled.isEmpty())
    }
}
