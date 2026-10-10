package de.bcycle.app

/** Android can report an existing peer immediately after opening a GATT server. */
class AccessoryLinkGate(connectedAtStart: Boolean) {
    enum class Decision { ACCEPT, BEFORE_ADVERTISING, WAIT_FOR_DISCONNECT, ALREADY_ACCEPTED, STOPPED }

    private var active = true
    private var peerConnected = connectedAtStart
    private var accepted = false
    var advertisingStarted = false
        private set
    val waitingForDisconnect: Boolean get() = active && !accepted && peerConnected

    fun onAdvertisingStarted() {
        if (active) advertisingStarted = true
    }

    fun onConnected(): Decision {
        if (!active) return Decision.STOPPED
        if (accepted) return Decision.ALREADY_ACCEPTED
        if (peerConnected) return Decision.WAIT_FOR_DISCONNECT
        peerConnected = true
        if (!advertisingStarted) return Decision.BEFORE_ADVERTISING
        accepted = true
        return Decision.ACCEPT
    }

    fun onDisconnected() { peerConnected = false }
    fun onAdvertisingStopped() { advertisingStarted = false }
    fun stop() { active = false; advertisingStarted = false }
}
