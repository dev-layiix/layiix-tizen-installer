package com.layiix.tizenbridge.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Utilità di rete: validazione input, reachability, IP locale, scansione LAN.
 * Nessun input utente viene mai concatenato in comandi shell.
 */
object NetworkUtils {

    const val DEFAULT_SDB_PORT = 26101
    const val MAX_PORT = 65535

    private val IPV4_RE = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

    fun isValidIpv4(raw: String): Boolean {
        val s = raw.trim()
        val m = IPV4_RE.matchEntire(s) ?: return false
        for (i in 1..4) {
            val octet = m.groupValues[i]
            // Rifiuta ottetti con zeri iniziali ambigui ("01") e valori fuori scala.
            if (octet.length > 1 && octet.startsWith("0")) return false
            val v = octet.toIntOrNull() ?: return false
            if (v !in 0..255) return false
        }
        // 0.0.0.0 e broadcast non sono TV valide.
        if (s == "0.0.0.0" || s == "255.255.255.255") return false
        return true
    }

    fun isValidPort(raw: String): Boolean {
        val p = raw.trim().toIntOrNull() ?: return false
        return p in 1..MAX_PORT
    }

    fun sanitizeHost(raw: String): String = raw.trim().take(64)

    /** TCP connect con timeout. Non lancia eccezioni: false = non raggiungibile. */
    fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Primo IPv4 non-loopback delle interfacce attive (Wi-Fi/hotspot). */
    fun localIpAddress(): String? {
        return try {
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
            for (ni in ifaces) {
                if (!ni.isUp || ni.isLoopback) continue
                for (addr in ni.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        // Preferisce indirizzi di rete privata, accetta anche gli altri.
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
            for (ni in ifaces) {
                if (!ni.isUp || ni.isLoopback) continue
                for (addr in ni.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        (addr.hostAddress as? String)?.let { return it }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun subnetBaseOf(ip: String): String? {
        if (!isValidIpv4(ip)) return null
        return ip.substringBeforeLast('.')
    }

    /**
     * Scansiona 1..254 della sottorete sulla porta SDB.
     * Chiama [onFound] per ogni host con porta aperta. Best-effort, cancellabile.
     */
    suspend fun scanSubnet(
        base: String,
        port: Int = DEFAULT_SDB_PORT,
        timeoutMs: Int = 400,
        onFound: (String) -> Unit,
    ): List<String> = withContext(Dispatchers.IO) {
        val sem = Semaphore(50)
        val found = mutableListOf<String>()
        (1..254).map { i ->
            async {
                sem.acquire()
                try {
                    val host = "$base.$i"
                    if (isPortOpen(host, port, timeoutMs)) {
                        synchronized(found) { found.add(host) }
                        onFound(host)
                    }
                } finally {
                    sem.release()
                }
            }
        }.awaitAll()
        found.sorted()
    }
}
