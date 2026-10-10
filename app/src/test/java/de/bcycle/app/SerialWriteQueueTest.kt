package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class SerialWriteQueueTest {
    private class Harness(var accepted: Boolean = true) {
        val sent = mutableListOf<Int>()
        val scheduled = linkedMapOf<Runnable, Long>()
        val failures = mutableListOf<String>()
        val queue = SerialWriteQueue<Int>(
            send = { sent += it; accepted },
            schedule = { task, delay -> scheduled[task] = delay },
            cancel = { scheduled.remove(it) },
            changed = {}, failed = { failures += it },
        )
        fun runDelay(delay: Long) {
            val tasks = scheduled.filterValues { it == delay }.keys.toList()
            tasks.forEach { scheduled.remove(it); it.run() }
        }
    }

    @Test fun waitsForDescriptorSetupAndEveryGattWriteCallback() {
        val test = Harness()
        test.queue.enqueue(listOf(1, 2, 3))
        assertTrue(test.sent.isEmpty())
        test.queue.start()
        assertEquals(listOf(1), test.sent)
        test.runDelay(300)
        assertEquals(listOf(1), test.sent)
        test.queue.complete(true); test.runDelay(300)
        assertEquals(listOf(1, 2), test.sent)
        test.queue.complete(true); test.runDelay(300)
        assertEquals(listOf(1, 2, 3), test.sent)
        test.queue.complete(true)
        assertTrue(test.queue.idle); assertTrue(test.failures.isEmpty())
    }

    @Test fun startupRepliesTakePriorityWithoutOverlappingTheCurrentWrite() {
        val test = Harness()
        test.queue.enqueue(listOf(1, 2)); test.queue.start()
        test.queue.enqueue(listOf(9, 10), urgent = true)
        assertEquals(listOf(1), test.sent)
        test.queue.complete(true); test.runDelay(300)
        assertEquals(listOf(1, 9), test.sent)
        test.queue.complete(true); test.runDelay(300)
        assertEquals(listOf(1, 9, 10), test.sent)
    }

    @Test fun stopCancelsTimersAndLateCallbacksCannotSendAnotherRequest() {
        val test = Harness()
        test.queue.enqueue(listOf(1, 2)); test.queue.start(); test.queue.complete(true)
        val late = test.scheduled.keys.toList()
        test.queue.stop(); test.queue.complete(true)
        late.forEach { it.run() }
        assertEquals(listOf(1), test.sent)
        assertTrue(test.scheduled.isEmpty()); assertTrue(test.queue.idle)
    }

    @Test fun refusedAndMissingWriteCallbacksFailInsteadOfSilentlyDroppingRequests() {
        val refused = Harness(false)
        refused.queue.enqueue(listOf(1, 2)); refused.queue.start()
        assertEquals(1, refused.failures.size)
        assertTrue(refused.scheduled.isEmpty())
        val missing = Harness()
        missing.queue.enqueue(listOf(1, 2)); missing.queue.start(); missing.runDelay(5_000)
        assertEquals(1, missing.failures.size)
        assertEquals(listOf(1), missing.sent)
    }

    @Test fun backlogIsBoundedAndFailedAcknowledgementsCancelPendingWrites() {
        val overflow = Harness()
        overflow.queue.enqueue((1..17).toList())
        assertEquals(1, overflow.failures.size); assertTrue(overflow.sent.isEmpty())
        val failed = Harness()
        failed.queue.enqueue(listOf(1, 2)); failed.queue.start(); failed.queue.complete(false)
        assertEquals(1, failed.failures.size); assertTrue(failed.scheduled.isEmpty())
    }
}
