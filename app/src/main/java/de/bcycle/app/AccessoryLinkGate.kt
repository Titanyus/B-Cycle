package de.bcycle.app

/** Defer GATT attachment until advertising starts; both existing and new links are usable. */
class AccessoryLinkGate(connectedAtStart: Boolean) {
    enum class Decision { ATTACH_EXISTING, ATTACH_NEW, WAIT_FOR_ADVERTISING, WAIT_FOR_PEER, ALREADY_ATTACHED, STOPPED }

    private var active = true
    private var peerConnected = connectedAtStart
    private var attached = false
    var advertisingStarted = false
        private set
    fun onAdvertisingStarted(): Decision {
        if (!active) return Decision.STOPPED
        advertisingStarted = true
        if (attached) return Decision.ALREADY_ATTACHED
        if (!peerConnected) return Decision.WAIT_FOR_PEER
        attached = true
        return Decision.ATTACH_EXISTING
    }

    fun onConnected(): Decision {
        if (!active) return Decision.STOPPED
        val wasConnected = peerConnected
        peerConnected = true
        if (attached) return Decision.ALREADY_ATTACHED
        if (!advertisingStarted) return Decision.WAIT_FOR_ADVERTISING
        attached = true
        return if (wasConnected) Decision.ATTACH_EXISTING else Decision.ATTACH_NEW
    }

    fun onDisconnected() { peerConnected = false }
    fun onAdvertisingStopped() { advertisingStarted = false }
    fun stop() { active = false; advertisingStarted = false }
}
