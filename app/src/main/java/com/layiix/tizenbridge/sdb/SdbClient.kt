package com.layiix.tizenbridge.sdb

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min

/**
 * Client SDB minimale in puro Kotlin/Java: handshake, shell, capability,
 * push di file via servizio sync e comandi di installazione Tizen.
 *
 * Le TV Samsung resettano le connessioni a raffica: ogni nuovo socket aspetta
 * 400 ms (cooldown come il client Python di riferimento) e la chiusura invia
 * CLSE best-effort. [trace] riceve una riga per step (nomi comandi, size,
 * tempi — mai contenuti) per la diagnosi sul campo.
 */
class SdbClient(
    private val host: String,
    private val port: Int = DEFAULT_SDB_PORT,
    private val timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val trace: ((String) -> Unit)? = null,
) {
    companion object {
        const val DEFAULT_SDB_PORT = 26101
        const val DEFAULT_TIMEOUT_MS = 8_000
        const val LOCAL_ID = 1
        const val PRE_CONNECT_COOLDOWN_MS = 400L
    }

    private fun t(msg: String) {
        try {
            trace?.invoke(msg)
        } catch (_: Exception) {
        }
    }

    private fun openSocket(): Socket {
        try {
            Thread.sleep(PRE_CONNECT_COOLDOWN_MS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("connessione interrotta")
        }
        val t0 = System.currentTimeMillis()
        val s = Socket()
        try {
            s.tcpNoDelay = true
            s.connect(InetSocketAddress(host, port), timeoutMs)
            s.soTimeout = timeoutMs
            t("tcp connect ok in ${System.currentTimeMillis() - t0}ms")
            return s
        } catch (e: IOException) {
            try {
                s.close()
            } catch (_: IOException) {
            }
            throw IOException("tcp connect fallita: ${e.message}")
        }
    }

    private fun OutputStream.writePacket(cmd: String, arg0: Int, arg1: Int, payload: ByteArray) {
        write(SdbProtocol.pack(cmd, arg0, arg1, payload))
        flush()
    }

    private fun handshake(input: InputStream) {
        val hello = (SdbProtocol.HANDSHAKE_PAYLOAD + "\u0000").toByteArray(Charsets.UTF_8)
        val t0 = System.currentTimeMillis()
        val rsp = SdbProtocol.readPacket(input)
            ?: throw IOException("handshake: nessuna risposta (TV ha chiuso)")
        val dt = System.currentTimeMillis() - t0
        t("rx ${rsp.cmd} len=${rsp.payload.size} in ${dt}ms")
        when (rsp.cmd) {
            SdbProtocol.CMD_CNXN -> Unit
            SdbProtocol.CMD_CLSE -> throw IOException("handshake: rifiutato (CLSE) — accettare il popup sulla TV")
            else -> throw IOException("handshake: risposta inattesa ${rsp.cmd}")
        }
    }

    private fun sendHello(out: OutputStream) {
        val hello = (SdbProtocol.HANDSHAKE_PAYLOAD + "\u0000").toByteArray(Charsets.UTF_8)
        out.writePacket(SdbProtocol.CMD_CNXN, 0x01000000, 65536, hello)
        t("tx CNXN len=${hello.size}")
    }

    /** Esegue un comando shell remoto e ne restituisce l'output. */
    @Throws(IOException::class)
    fun shell(command: String): String {
        val socket = openSocket()
        var out: OutputStream? = null
        var remoteId = -1
        try {
            val input = socket.getInputStream()
            out = socket.getOutputStream()
            sendHello(out)
            handshake(input)

            val service = (SdbProtocol.shellService(command) + "\u0000").toByteArray(Charsets.UTF_8)
            out.writePacket(SdbProtocol.CMD_OPEN, LOCAL_ID, 0, service)
            t("tx OPEN shell len=${service.size}")

            val first = SdbProtocol.readPacket(input)
                ?: throw IOException("shell: nessuna risposta all'OPEN (reset?)")
            if (first.cmd == SdbProtocol.CMD_CLSE) {
                throw IOException("shell: canale rifiutato (CLSE)")
            }
            remoteId = SdbProtocol.remoteIdFromOpen(first)
            t("canale remoto=$remoteId su ${first.cmd}")

            val acc = StringBuilder()
            var wrte = 0
            var okay = 0
            while (true) {
                val pkt = SdbProtocol.readPacket(input) ?: break
                when (pkt.cmd) {
                    SdbProtocol.CMD_CLSE -> break
                    SdbProtocol.CMD_WRTE -> {
                        wrte++
                        if (pkt.payload.isNotEmpty()) {
                            acc.append(String(pkt.payload, Charsets.UTF_8))
                        }
                        out.writePacket(SdbProtocol.CMD_OKAY, LOCAL_ID, remoteId, ByteArray(0))
                    }
                    SdbProtocol.CMD_OKAY -> okay++
                    else -> Unit
                }
            }
            t("shell fine: wrte=$wrte okay=$okay out=${acc.length}ch")
            return acc.toString().trim()
        } finally {
            try {
                if (remoteId >= 0) {
                    out?.writePacket(SdbProtocol.CMD_CLSE, LOCAL_ID, remoteId, ByteArray(0))
                }
            } catch (_: Exception) {
            }
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
    }

    /** Legge le capability SDB (platform_version, sdk_toolpath, cpu_arch, ...). */
    @Throws(IOException::class)
    fun capability(): Map<String, String> {
        val socket = openSocket()
        var out: OutputStream? = null
        var remoteId = -1
        try {
            val input = socket.getInputStream()
            out = socket.getOutputStream()
            sendHello(out)
            handshake(input)

            val service = (SdbProtocol.CAPABILITY_SERVICE + "\u0000").toByteArray(Charsets.UTF_8)
            out.writePacket(SdbProtocol.CMD_OPEN, LOCAL_ID, 0, service)
            t("tx OPEN capability")
            val first = SdbProtocol.readPacket(input)
                ?: throw IOException("capability: nessuna risposta all'OPEN (reset?)")
            if (first.cmd == SdbProtocol.CMD_CLSE) {
                throw IOException("capability: canale rifiutato (CLSE)")
            }
            remoteId = SdbProtocol.remoteIdFromOpen(first)

            val acc = StringBuilder()
            while (true) {
                val pkt = SdbProtocol.readPacket(input) ?: break
                when (pkt.cmd) {
                    SdbProtocol.CMD_CLSE -> break
                    SdbProtocol.CMD_WRTE -> {
                        if (pkt.payload.isNotEmpty()) {
                            acc.append(String(pkt.payload, Charsets.UTF_8))
                        }
                        out.writePacket(SdbProtocol.CMD_OKAY, LOCAL_ID, remoteId, ByteArray(0))
                    }
                    else -> Unit
                }
            }
            t("capability: ${acc.length}ch")
            return acc.toString()
                .lineSequence()
                .mapNotNull { line ->
                    val idx = line.indexOf(':')
                    if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                }
                .toMap()
        } finally {
            try {
                if (remoteId >= 0) {
                    out?.writePacket(SdbProtocol.CMD_CLSE, LOCAL_ID, remoteId, ByteArray(0))
                }
            } catch (_: Exception) {
            }
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
    }

    /**
     * Trasferisce [data] nel percorso remoto [remotePath] via servizio sync.
     * [onProgress] riceve (inviati, totale). Ritorna il percorso remoto usato.
     */
    @Throws(IOException::class)
    fun pushFile(
        data: ByteArray,
        remotePath: String,
        onProgress: ((sent: Long, total: Long) -> Unit)? = null,
    ): String {
        require(!remotePath.contains("..")) { "percorso remoto non valido" }
        val socket = openSocket()
        var out: OutputStream? = null
        var remoteId = -1
        try {
            val input = socket.getInputStream()
            out = socket.getOutputStream()
            sendHello(out)
            handshake(input)

            val service = (SdbProtocol.SYNC_SERVICE + "\u0000").toByteArray(Charsets.UTF_8)
            out.writePacket(SdbProtocol.CMD_OPEN, LOCAL_ID, 0, service)
            t("tx OPEN sync")
            val first = SdbProtocol.readPacket(input)
                ?: throw IOException("sync: nessuna risposta all'OPEN (reset?)")
            if (first.cmd == SdbProtocol.CMD_CLSE) {
                throw IOException("sync: canale rifiutato (CLSE)")
            }
            remoteId = SdbProtocol.remoteIdFromOpen(first)

            // SEND
            out.writePacket(
                SdbProtocol.CMD_WRTE, LOCAL_ID, remoteId,
                SdbProtocol.syncSendPayload(remotePath),
            )
            consumeAck(input, out, remoteId)

            // DATA a blocchi: frammenti da 4000 byte raggruppati fino a 32768.
            var offset = 0
            while (offset < data.size) {
                val block = StringBuilderBytes()
                while (block.size < 32768 && offset < data.size) {
                    val n = min(4000, data.size - offset)
                    block.append(SdbProtocol.syncDataPayload(data.copyOfRange(offset, offset + n)))
                    offset += n
                }
                val payload = block.toByteArray()
                out.writePacket(SdbProtocol.CMD_WRTE, LOCAL_ID, remoteId, payload)
                onProgress?.invoke(offset.toLong(), data.size.toLong())
                consumeAck(input, out, remoteId)
            }

            // DONE
            val mtime = (System.currentTimeMillis() / 1000L).toInt()
            out.writePacket(
                SdbProtocol.CMD_WRTE, LOCAL_ID, remoteId,
                SdbProtocol.syncDonePayload(mtime),
            )
            consumeAck(input, out, remoteId)
            // La TV invia un ultimo OKAY di chiusura sync: lettura best-effort.
            try {
                SdbProtocol.readPacket(input)
            } catch (_: IOException) {
            }
            t("push ok: ${data.size}B -> $remotePath")
            return remotePath
        } finally {
            try {
                if (remoteId >= 0) {
                    out?.writePacket(SdbProtocol.CMD_CLSE, LOCAL_ID, remoteId, ByteArray(0))
                }
            } catch (_: Exception) {
            }
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun consumeAck(input: InputStream, out: OutputStream, remoteId: Int) {
        val pkt = try {
            SdbProtocol.readPacket(input)
        } catch (e: IOException) {
            throw IOException("timeout di risposta dalla TV: ${e.message}")
        } ?: throw IOException("canale chiuso dalla TV durante il trasferimento")
        // Alcune TV rispondono WRTE invece di OKAY: in quel caso confermiamo.
        if (pkt.cmd == SdbProtocol.CMD_WRTE) {
            out.writePacket(SdbProtocol.CMD_OKAY, LOCAL_ID, remoteId, ByteArray(0))
        }
    }

    /** Accumulatore di byte senza dipendenze extra. */
    private class StringBuilderBytes {
        private val parts = ArrayList<ByteArray>()
        var size = 0
            private set

        fun append(b: ByteArray) {
            parts.add(b)
            size += b.size
        }

        fun toByteArray(): ByteArray {
            val out = ByteArray(size)
            var o = 0
            for (p in parts) {
                System.arraycopy(p, 0, out, o, p.size)
                o += p.size
            }
            return out
        }
    }
}
