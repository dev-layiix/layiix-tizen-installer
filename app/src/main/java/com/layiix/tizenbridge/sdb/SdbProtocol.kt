package com.layiix.tizenbridge.sdb

import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Framing del protocollo SDB/Tizen su TCP (porta 26101).
 *
 * Formato pacchetto (little-endian, 24 byte di header):
 *   cmd(4 ASCII) | arg0 u32 | arg1 u32 | length u32 | checksum u32 | magic u32
 * dove checksum = somma dei byte del payload (mod 2^32)
 * e magic = NOT bit-a-bit della word del comando.
 *
 * Reimplementazione documentata a partire dal comportamento osservabile del
 * client Python open source di Suz41/Sideload-Tizen-by-Android (MIT) e dalla
 * documentazione/analisi pubblica del protocollo SDB. Nessun codice copiato:
 * solo il formato wire, che è un fatto di interoperabilità.
 */
object SdbProtocol {

    const val CMD_CNXN = "CNXN"
    const val CMD_OPEN = "OPEN"
    const val CMD_WRTE = "WRTE"
    const val CMD_OKAY = "OKAY"
    const val CMD_CLSE = "CLSE"

    const val HANDSHAKE_PAYLOAD = "host::sdb-net-client"
    const val SYNC_SERVICE = "sync:"
    const val CAPABILITY_SERVICE = "capability:"

    fun shellService(command: String): String = "shell:$command"

    data class Packet(
        val cmd: String,
        val arg0: Int,
        val arg1: Int,
        val payload: ByteArray,
    )

    /**
     * Nell'OKAY di risposta a OPEN, l'ID del canale remoto è arg0 (quello del
     * peer); arg1 è il nostro ID locale rimandato indietro. Invertirli fa
     * resettare la connessione alla TV (bug reale trovato sul campo).
     */
    fun remoteIdFromOpen(openResponse: Packet): Int = openResponse.arg0

    /** Word little-endian dei 4 char ASCII del comando. */
    fun commandWord(cmd: String): Int {
        require(cmd.length == 4) { "cmd deve essere di 4 caratteri" }
        var w = 0
        for (i in 0..3) {
            w = w or ((cmd[i].code and 0xFF) shl (i * 8))
        }
        return w
    }

    fun magicFor(cmd: String): Int = commandWord(cmd).inv()

    fun checksumOf(payload: ByteArray): Int {
        var sum = 0L
        for (b in payload) sum += (b.toInt() and 0xFF)
        return (sum and 0xFFFFFFFFL).toInt()
    }

    fun pack(cmd: String, arg0: Int, arg1: Int, payload: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(24 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        for (c in cmd) buf.put(c.code.toByte())
        buf.putInt(arg0)
        buf.putInt(arg1)
        buf.putInt(payload.size)
        buf.putInt(checksumOf(payload))
        buf.putInt(magicFor(cmd))
        buf.put(payload)
        return buf.array()
    }

    @Throws(EOFException::class)
    fun readExact(input: InputStream, n: Int): ByteArray {
        val out = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(out, read, n - read)
            if (r < 0) throw EOFException("stream chiuso dopo $read/$n byte")
            read += r
        }
        return out
    }

    /** Legge un pacchetto; ritorna null se lo stream è chiuso in modo pulito. */
    fun readPacket(input: InputStream): Packet? {
        val hdr = try {
            readExact(input, 24)
        } catch (e: EOFException) {
            return null
        }
        val buf = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
        val cmdBytes = ByteArray(4)
        buf.get(cmdBytes)
        val cmd = String(cmdBytes, Charsets.US_ASCII)
        val arg0 = buf.int
        val arg1 = buf.int
        val length = buf.int
        // checksum e magic vengono letti ma non bloccano: TV reali note per tollerarli.
        buf.int // checksum
        buf.int // magic
        if (length < 0 || length > 8 * 1024 * 1024) return null
        val payload = if (length > 0) {
            try {
                readExact(input, length)
            } catch (e: EOFException) {
                return null
            }
        } else {
            ByteArray(0)
        }
        return Packet(cmd, arg0, arg1, payload)
    }

    /** Frammento SYNC "SEND": header interno da 8 byte + destinazione. */
    fun syncSendPayload(remotePath: String): ByteArray {
        val dest = "$remotePath,33279".toByteArray(Charsets.UTF_8)
        val buf = ByteBuffer.allocate(8 + dest.size).order(ByteOrder.LITTLE_ENDIAN)
        for (c in "SEND") buf.put(c.code.toByte())
        buf.putInt(dest.size)
        buf.put(dest)
        return buf.array()
    }

    /** Frammento SYNC "DATA". */
    fun syncDataPayload(chunk: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(8 + chunk.size).order(ByteOrder.LITTLE_ENDIAN)
        for (c in "DATA") buf.put(c.code.toByte())
        buf.putInt(chunk.size)
        buf.put(chunk)
        return buf.array()
    }

    /** Frammento SYNC "DONE" con mtime unix. */
    fun syncDonePayload(mtimeSec: Int): ByteArray {
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        for (c in "DONE") buf.put(c.code.toByte())
        buf.putInt(mtimeSec)
        return buf.array()
    }
}
