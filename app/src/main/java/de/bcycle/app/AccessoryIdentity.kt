package de.bcycle.app

import java.util.UUID

/** Own, non-sensitive DIS values. The peer's identity and private services are never read. */
class AccessoryIdentity(version: String) {
    val values: Map<UUID, ByteArray> = mapOf(
        uuid("2a29") to "B-Cycle".toByteArray(Charsets.UTF_8),
        uuid("2a24") to "B-Cycle Android".toByteArray(Charsets.UTF_8),
        uuid("2a26") to version.take(20).toByteArray(Charsets.US_ASCII),
    )

    fun read(uuid: UUID, offset: Int): ByteArray? {
        val value = values[uuid] ?: return null
        return if (offset in 0..value.size) value.copyOfRange(offset, value.size) else null
    }

    private fun uuid(short: String): UUID = UUID.fromString("0000$short-0000-1000-8000-00805f9b34fb")
}
