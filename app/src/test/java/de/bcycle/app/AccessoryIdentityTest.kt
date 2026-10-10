package de.bcycle.app

import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class AccessoryIdentityTest {
    private val manufacturer = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    private val model = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
    private val firmware = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")

    @Test fun exposesOnlyOwnStandardDeviceInformationAndFitsDefaultAttReads() {
        val identity = AccessoryIdentity("0.1.3")
        assertEquals(setOf(manufacturer, model, firmware), identity.values.keys)
        assertEquals("B-Cycle", identity.read(manufacturer, 0)!!.toString(Charsets.UTF_8))
        assertEquals("B-Cycle Android", identity.read(model, 0)!!.toString(Charsets.UTF_8))
        assertEquals("0.1.3", identity.read(firmware, 0)!!.toString(Charsets.UTF_8))
        assertTrue(identity.values.values.all { it.size <= 22 })
    }

    @Test fun offsetReadsReturnRemainderAndEmptyAtEnd() {
        val identity = AccessoryIdentity("0.1.3")
        assertEquals("Cycle", identity.read(manufacturer, 2)!!.toString(Charsets.UTF_8))
        assertArrayEquals(byteArrayOf(), identity.read(manufacturer, 7))
    }

    @Test fun invalidOffsetsAndUnknownCharacteristicsAreRejected() {
        val identity = AccessoryIdentity("0.1.3")
        assertNull(identity.read(manufacturer, -1))
        assertNull(identity.read(manufacturer, 8))
        assertNull(identity.read(manufacturer, Int.MAX_VALUE))
        assertNull(identity.read(UUID.fromString("0000eb21-eaa2-11e9-81b4-2a2ae2dbcce4"), 0))
    }

    @Test fun repliesCannotMutateIdentityAndLongVersionFitsDefaultMtu() {
        val identity = AccessoryIdentity("1234567890".repeat(10))
        val reply = identity.read(manufacturer, 0)!!
        reply[0] = 0
        assertEquals("B-Cycle", identity.read(manufacturer, 0)!!.toString(Charsets.UTF_8))
        assertEquals(20, identity.read(firmware, 0)!!.size)
    }
}
