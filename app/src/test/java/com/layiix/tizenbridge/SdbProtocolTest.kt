package com.layiix.tizenbridge

import com.layiix.tizenbridge.sdb.SdbProtocol
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class SdbProtocolTest {

    @Test
    fun `command word e magic sono bitwise not`() {
        assertEquals(0x4E584E43.toInt(), SdbProtocol.commandWord("CNXN"))
        assertEquals(0x4E584E43.toInt().inv(), SdbProtocol.magicFor("CNXN"))
        assertEquals("OPEN".let { SdbProtocol.commandWord(it) }.inv(), SdbProtocol.magicFor("OPEN"))
    }

    @Test
    fun `checksum somma i byte unsigned`() {
        assertEquals(0, SdbProtocol.checksumOf(ByteArray(0)))
        assertEquals(3, SdbProtocol.checksumOf(byteArrayOf(1, 2)))
        assertEquals(255, SdbProtocol.checksumOf(byteArrayOf(0xFF.toByte())))
    }

    @Test
    fun `pack e readPacket fanno roundtrip`() {
        val payload = "shell:0 applist\u0000".toByteArray(Charsets.UTF_8)
        val packed = SdbProtocol.pack("OPEN", 1, 0, payload)
        assertEquals(24 + payload.size, packed.size)
        val pkt = SdbProtocol.readPacket(ByteArrayInputStream(packed))
        assertNotNull(pkt)
        assertEquals("OPEN", pkt!!.cmd)
        assertEquals(1, pkt.arg0)
        assertEquals(0, pkt.arg1)
        assertArrayEquals(payload, pkt.payload)
    }

    @Test
    fun `readPacket su stream vuoto ritorna null`() {
        assertNull(SdbProtocol.readPacket(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    fun `sync payloads hanno gli header giusti`() {
        val send = SdbProtocol.syncSendPayload("/tmp/a.wgt")
        assertEquals("SEND", String(send.copyOfRange(0, 4), Charsets.US_ASCII))
        val data = SdbProtocol.syncDataPayload(byteArrayOf(1, 2, 3))
        assertEquals("DATA", String(data.copyOfRange(0, 4), Charsets.US_ASCII))
        assertEquals(11, data.size)
        val done = SdbProtocol.syncDonePayload(123)
        assertEquals("DONE", String(done.copyOfRange(0, 4), Charsets.US_ASCII))
        assertEquals(8, done.size)
    }

    @Test
    fun `remote id del canale e arg0 della risposta OPEN`() {
        // La TV risponde all'OPEN con OKAY(idRemoto, nostroId): il canale è arg0.
        val packed = SdbProtocol.pack("OKAY", 77, 1, ByteArray(0))
        val pkt = SdbProtocol.readPacket(ByteArrayInputStream(packed))!!
        assertEquals(77, SdbProtocol.remoteIdFromOpen(pkt))
    }

    @Test
    fun `shell service format`() {
        assertEquals("shell:0 applist", SdbProtocol.shellService("0 applist"))
    }
}
