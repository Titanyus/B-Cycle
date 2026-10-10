package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import de.bcycle.app.AccessoryLinkGate.Decision.*

class AccessoryLinkGateTest {
    @Test fun replays013ButWaitsForAdvertisingBeforeAttachingTheExistingLink() {
        val gate = AccessoryLinkGate(false)
        // Uploaded trace: 2714 ms CONNECTED, 2717 ms AD request, 2763 ms AD started.
        assertEquals(WAIT_FOR_ADVERTISING, gate.onConnected())
        assertFalse(gate.advertisingStarted)
        assertEquals(ATTACH_EXISTING, gate.onAdvertisingStarted())
        assertEquals(ALREADY_ATTACHED, gate.onConnected())
    }

    @Test fun persistentFlowLinkDoesNotRequireDisconnectOrAnotherConnectedCallback() {
        val gate = AccessoryLinkGate(true)
        // 0.1.5 trace: existing peer at start, advertising succeeds, no disconnect follows.
        assertEquals(ATTACH_EXISTING, gate.onAdvertisingStarted())
        assertEquals(ALREADY_ATTACHED, gate.onConnected())
    }

    @Test fun newConnectionAfterAdvertisingIsAcceptedOnlyOnce() {
        val gate = AccessoryLinkGate(false)
        assertEquals(WAIT_FOR_PEER, gate.onAdvertisingStarted())
        assertEquals(ATTACH_NEW, gate.onConnected())
        assertEquals(ALREADY_ATTACHED, gate.onConnected())
        gate.onDisconnected()
        assertEquals(ALREADY_ATTACHED, gate.onConnected())
    }

    @Test fun oldLinkCanDisconnectBeforeAdvertisingStartConfirmation() {
        val gate = AccessoryLinkGate(true)
        gate.onDisconnected()
        assertEquals(WAIT_FOR_PEER, gate.onAdvertisingStarted())
        assertEquals(ATTACH_NEW, gate.onConnected())
    }

    @Test fun stoppedSessionCannotBeRevivedByLateCallbacks() {
        val gate = AccessoryLinkGate(false)
        gate.stop()
        assertEquals(STOPPED, gate.onAdvertisingStarted())
        gate.onDisconnected()
        assertFalse(gate.advertisingStarted)
        assertEquals(STOPPED, gate.onConnected())
    }

    @Test fun advertisingStoppedBeforeAnIncomingCallbackPreventsAcceptance() {
        val gate = AccessoryLinkGate(false)
        gate.onAdvertisingStarted()
        gate.onAdvertisingStopped()
        assertEquals(WAIT_FOR_ADVERTISING, gate.onConnected())
    }

    @Test fun peerThatLeavesDuringAdvertisingSetupIsNotAttachedAsAStaleSharedLink() {
        val gate = AccessoryLinkGate(false)
        assertEquals(WAIT_FOR_ADVERTISING, gate.onConnected())
        gate.onDisconnected()
        assertEquals(WAIT_FOR_PEER, gate.onAdvertisingStarted())
        assertEquals(ATTACH_NEW, gate.onConnected())
    }

    @Test fun duplicateAdvertisingConfirmationCannotCreateASecondGattClient() {
        val gate = AccessoryLinkGate(true)
        assertEquals(ATTACH_EXISTING, gate.onAdvertisingStarted())
        assertEquals(ALREADY_ATTACHED, gate.onAdvertisingStarted())
    }
}
