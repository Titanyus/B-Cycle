package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import de.bcycle.app.AccessoryLinkGate.Decision.*

class AccessoryLinkGateTest {
    @Test fun replays013ConnectionBeforeAdvertisingWithoutAcceptingTheExistingLink() {
        val gate = AccessoryLinkGate(false)
        // Uploaded trace: 2714 ms CONNECTED, 2717 ms AD request, 2763 ms AD started.
        assertEquals(BEFORE_ADVERTISING, gate.onConnected())
        assertTrue(gate.waitingForDisconnect)
        gate.onAdvertisingStarted()
        assertTrue(gate.waitingForDisconnect)
        assertEquals(WAIT_FOR_DISCONNECT, gate.onConnected())
        gate.onDisconnected()
        assertFalse(gate.waitingForDisconnect)
        assertEquals(ACCEPT, gate.onConnected())
    }

    @Test fun delayedExistingConnectionCallbackIsRejectedEvenAfterAdvertisingStarts() {
        val gate = AccessoryLinkGate(true)
        gate.onAdvertisingStarted()
        assertEquals(WAIT_FOR_DISCONNECT, gate.onConnected())
        gate.onDisconnected()
        assertEquals(ACCEPT, gate.onConnected())
    }

    @Test fun newConnectionAfterAdvertisingIsAcceptedOnlyOnce() {
        val gate = AccessoryLinkGate(false)
        gate.onAdvertisingStarted()
        assertEquals(ACCEPT, gate.onConnected())
        assertFalse(gate.waitingForDisconnect)
        assertEquals(ALREADY_ACCEPTED, gate.onConnected())
        gate.onDisconnected()
        assertEquals(ALREADY_ACCEPTED, gate.onConnected())
    }

    @Test fun oldLinkCanDisconnectBeforeAdvertisingStartConfirmation() {
        val gate = AccessoryLinkGate(true)
        gate.onDisconnected()
        gate.onAdvertisingStarted()
        assertFalse(gate.waitingForDisconnect)
        assertEquals(ACCEPT, gate.onConnected())
    }

    @Test fun stoppedSessionCannotBeRevivedByLateCallbacks() {
        val gate = AccessoryLinkGate(false)
        gate.stop()
        gate.onAdvertisingStarted()
        gate.onDisconnected()
        assertFalse(gate.advertisingStarted)
        assertFalse(gate.waitingForDisconnect)
        assertEquals(STOPPED, gate.onConnected())
    }

    @Test fun advertisingStoppedBeforeAnIncomingCallbackPreventsAcceptance() {
        val gate = AccessoryLinkGate(false)
        gate.onAdvertisingStarted()
        gate.onAdvertisingStopped()
        assertEquals(BEFORE_ADVERTISING, gate.onConnected())
    }
}
