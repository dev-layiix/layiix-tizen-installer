package com.layiix.tizenbridge.samsung

import android.content.Context
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

/**
 * Creazione certificati Samsung interamente sul telefono (niente PC):
 * login OAuth nel browser + CSR PKCS#10 + CA online Samsung + packaging .p12.
 * Replica il flusso di Tizen Certificate Manager (Samsung Developer docs).
 */
object SamsungCert {

    const val OAUTH_URL = "https://account.samsung.com/mobile/account/check.do"
    const val SERVICE_ID = "v285zxnl3h"
    const val ACTION_ID = "StartOAuth2"
    const val CA_BASE = "https://svdca.samsungqbe.com"
    const val OAUTH_PORT = 4794
    const val OAUTH_TIMEOUT_MS = 5 * 60 * 1000L

    data class Token(val access: String, val userId: String, val email: String, val expiresIn: Int)

    // ---------- DUID ----------

    /** Normalizza un DUID (uppercase, charset valido) o null. */
    fun normalizeDuid(raw: String?): String? {
        val d = (raw ?: "").trim().uppercase()
        if (d.isEmpty()) return null
        if (!d.all { it.isLetterOrDigit() || it in "#.-:=" }) return null
        return d
    }

    /** DUID pronto per il SAN: uppercase + prefisso versione (default 2.0#). */
    fun duidForSan(raw: String?): String? {
        val d = normalizeDuid(raw) ?: return null
        return if ('#' in d || d.startsWith("-")) d else "2.0#$d"
    }
    fun validPassword(pw: String): Boolean {
        if (pw.length < 8) return false
        return pw.any { it.isUpperCase() } && pw.any { it.isLowerCase() } && pw.any { it.isDigit() }
    }

    // ---------- DER minimo ----------

    private fun len(n: Int): ByteArray = when {
        n < 128 -> byteArrayOf(n.toByte())
        else -> {
            var v = n
            val b = mutableListOf<Byte>()
            while (v > 0) {
                b.add(0, (v and 0xFF).toByte())
                v = v ushr 8
            }
            byteArrayOf((0x80 or b.size).toByte()) + b.toByteArray()
        }
    }

    private fun tlv(tag: Int, vararg parts: ByteArray): ByteArray {
        val body = parts.fold(byteArrayOf()) { a, p -> a + p }
        return byteArrayOf(tag.toByte()) + len(body.size) + body
    }

    fun seq(vararg parts: ByteArray): ByteArray = tlv(0x30, *parts)
    fun set(vararg parts: ByteArray): ByteArray = tlv(0x31, *parts)
    fun intBytes(raw: ByteArray): ByteArray {
        var b = raw.dropWhile { it == 0.toByte() }.toByteArray()
        if (b.isEmpty()) b = byteArrayOf(0)
        if (b[0].toInt() and 0x80 != 0) b = byteArrayOf(0) + b
        return byteArrayOf(0x02) + len(b.size) + b
    }

    fun bitStr(data: ByteArray): ByteArray = tlv(0x03, byteArrayOf(0) + data)
    fun octStr(data: ByteArray): ByteArray = tlv(0x04, data)
    fun utf8(s: String): ByteArray = tlv(0x0C, s.toByteArray(Charsets.UTF_8))
    fun ia5(s: String): ByteArray = tlv(0x16, s.toByteArray(Charsets.US_ASCII))
    fun oid(vararg arcs: Int): ByteArray {
        val out = mutableListOf<Byte>()
        out.add(((arcs[0] * 40) + arcs[1]).toByte())
        for (a in arcs.drop(2)) {
            var v = a
            val st = mutableListOf<Byte>()
            st.add(0, (v and 0x7F).toByte())
            v = v ushr 7
            while (v > 0) {
                st.add(0, ((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
            out.addAll(st)
        }
        return byteArrayOf(0x06) + len(out.size) + out.toByteArray()
    }

    fun explicit0(vararg parts: ByteArray): ByteArray = tlv(0xA0, *parts)

    val OID_RSA = intArrayOf(1, 2, 840, 113549, 1, 1, 1)
    val OID_SHA512_RSA = intArrayOf(1, 2, 840, 113549, 1, 1, 13)
    val OID_EXT_REQ = intArrayOf(1, 2, 840, 113549, 1, 9, 14)
    val OID_SAN = intArrayOf(2, 5, 29, 17)
    val OID_CN = intArrayOf(2, 5, 4, 3)

    // ---------- PKCS#10 ----------

    /** CSR PEM con subject CN + SAN URI. Ritorna (pem, criDer) per verifica. */
    fun buildCsr(
        pair: KeyPair,
        cn: String,
        sanUris: List<String>,
    ): Pair<String, ByteArray> {
        val spki = X509EncodedKeySpec(pair.public.encoded) // SubjectPublicKeyInfo DER
        val name = seq(set(seq(oid(*OID_CN), utf8(cn))))
        val exts = mutableListOf<ByteArray>()
        if (sanUris.isNotEmpty()) {
            val names = sanUris.map { u -> byteArrayOf(0x86.toByte()) + len(u.length) + u.toByteArray(Charsets.US_ASCII) }
            val gns = seq(*names.toTypedArray())
            exts.add(seq(oid(*OID_SAN), octStr(gns)))
        }
        val attrs: ByteArray = if (exts.isNotEmpty()) {
            explicit0(seq(oid(*OID_EXT_REQ), set(seq(*exts.toTypedArray()))))
        } else {
            explicit0()
        }
        val cri = seq(
            tlv(0x02, byteArrayOf(0)), // version INTEGER 0
            name,
            spki.encoded,
            attrs,
        )
        val sigAlg = seq(oid(*OID_SHA512_RSA), tlv(0x05))
        val sig = Signature.getInstance("SHA512withRSA").apply {
            initSign(pair.private)
            update(cri)
        }.sign()
        val req = seq(cri, sigAlg, bitStr(sig))
        val pem = "-----BEGIN CERTIFICATE REQUEST-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(req) +
            "\n-----END CERTIFICATE REQUEST-----\n"
        return pem to cri
    }

    fun genRsa(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        return kpg.generateKeyPair()
    }

    // ---------- OAuth nel browser ----------

    @Volatile private var currentServer: ServerSocket? = null
    private val cancelled = AtomicBoolean(false)

    fun loginUrl(port: Int): String {
        // Il server Samsung accetta solo "localhost" (non 127.0.0.1) come redirect_uri.
        val redirect = "http://localhost:$port/signin/callback"
        return "$OAUTH_URL?serviceID=$SERVICE_ID&actionID=$ACTION_ID&accessToken=Y" +
            "&redirect_uri=${java.net.URLEncoder.encode(redirect, "UTF-8")}"
    }

    fun cancel() {
        cancelled.set(true)
        try {
            currentServer?.close()
        } catch (_: Exception) {
        }
    }

    /**
     * Apre il browser via [openBrowser] e attende il callback OAuth su loopback.
     * Lancia CancellationException se annullato, IllegalStateException al timeout.
     */
    @Throws(Exception::class)
    fun login(openBrowser: (String) -> Unit, isCancelled: () -> Boolean = { cancelled.get() }): Token {
        cancelled.set(false)
        // Il server Samsung accetta SOLO http://localhost:4794/signin/callback
        // (verificato: altre porte/host danno 400). Niente fallback.
        val loopback = try {
            java.net.InetAddress.getByName("localhost")
        } catch (_: Exception) {
            java.net.InetAddress.getByName("127.0.0.1")
        }
        val server: ServerSocket = try {
            ServerSocket(OAUTH_PORT, 1, loopback)
        } catch (_: Exception) {
            throw IllegalStateException("porta 4794 occupata")
        }
        currentServer = server
        val port = OAUTH_PORT
        try {
            server.soTimeout = 10_000
            openBrowser(loginUrl(port))
            val deadline = System.currentTimeMillis() + OAUTH_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                if (isCancelled() || cancelled.get()) throw java.util.concurrent.CancellationException("cancelled")
                val sock: Socket = try {
                    server.accept()
                } catch (_: java.net.SocketTimeoutException) {
                    continue
                }
                try {
                    val token = handleCallback(sock)
                    if (token != null) return token
                } catch (_: Exception) {
                } finally {
                    try {
                        sock.close()
                    } catch (_: Exception) {
                    }
                }
            }
            throw IllegalStateException("timeout")
        } finally {
            currentServer = null
            try {
                server.close()
            } catch (_: Exception) {
            }
        }
    }

    internal fun handleCallback(sock: Socket): Token? {
        val inp = sock.getInputStream()
        val head = ByteArrayOutputStream()
        val one = ByteArray(1)
        // Legge fino a fine header.
        while (!head.toString(Charsets.ISO_8859_1.name()).contains("\r\n\r\n")) {
            if (inp.read(one) <= 0) break
            head.write(one, 0, 1)
            if (head.size() > 65536) return null
        }
        val header = head.toString(Charsets.ISO_8859_1.name())
        val firstLine = header.lineSequence().firstOrNull() ?: return null
        val parts = firstLine.split(" ")
        if (parts.size < 2) return null
        val path = parts[1]
        var contentLen = 0
        for (l in header.lineSequence()) {
            val m = Regex("^Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(l)
            if (m != null) contentLen = m.groupValues[1].toIntOrNull() ?: 0
        }
        val bodyBytes = ByteArray(contentLen)
        var done = 0
        while (done < contentLen) {
            val n = inp.read(bodyBytes, done, contentLen - done)
            if (n <= 0) break
            done += n
        }
        val body = bodyBytes.toString(Charsets.ISO_8859_1)
        val code = parseCallbackCode(body, path)
        val out = sock.getOutputStream()
        if (code == null) {
            val msg = "Not found"
            out.write("HTTP/1.1 404 Not Found\r\nContent-Length: ${msg.length}\r\nConnection: close\r\n\r\n$msg".toByteArray())
            out.flush()
            return null
        }
        val json = JSONObject(code)
        val token = Token(
            access = json.getString("access_token"),
            userId = json.getString("userId"),
            email = json.optString("inputEmailID", ""),
            expiresIn = json.optString("access_token_expires_in", "86400").toIntOrNull() ?: 86400,
        )
        val page = "<html><body style=\"font-family:sans-serif;text-align:center;padding:50px\">" +
            "<h1>Login riuscito</h1><p>Torna all'app Tizen Bridge.</p></body></html>"
        val pb = page.toByteArray(Charsets.UTF_8)
        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${pb.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(pb)
        out.flush()
        return token
    }

    // ---------- CA online ----------

    private fun multipart(fields: Map<String, String>, fileName: String, fileContent: String): Pair<ByteArray, String> {
        val boundary = "----${System.currentTimeMillis()}"
        val out = ByteArrayOutputStream()
        fun field(name: String, value: String) {
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
            out.write(value.toByteArray(Charsets.UTF_8))
            out.write("\r\n".toByteArray())
        }
        for ((k, v) in fields) field(k, v)
        out.write("--$boundary\r\nContent-Disposition: form-data; name=\"csr\"; filename=\"$fileName\"\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
        out.write(fileContent.toByteArray(Charsets.UTF_8))
        out.write("\r\n--$boundary--\r\n".toByteArray())
        return out.toByteArray() to "multipart/form-data; boundary=$boundary"
    }

    private fun post(path: String, body: ByteArray, contentType: String): String {
        return try {
            postTo(CA_BASE, path, body, contentType, null)
        } catch (e: java.net.UnknownHostException) {
            // Il DNS del telefono blocca/filtra l'host della CA (ad-block/DNS privato):
            // risolvi via DoH e riprova con IP pinnato (TLS sempre verificato).
            val ip = dohResolve("svdca.samsungqbe.com") ?: throw e
            postTo("https://$ip", path, body, contentType, "svdca.samsungqbe.com")
        }
    }

    private fun postTo(base: String, path: String, body: ByteArray, contentType: String, pinHost: String?): String {
        val conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", contentType)
            if (pinHost != null) {
                setRequestProperty("Host", pinHost)
                (this as? javax.net.ssl.HttpsURLConnection)?.let {
                    it.sslSocketFactory = PinFactory(pinHost)
                    it.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, session ->
                        javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(pinHost, session)
                    }
                }
            } else {
                // Come il riferimento: niente verifica TLS rigida.
                (this as? javax.net.ssl.HttpsURLConnection)?.let {
                    it.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
                }
            }
        }
        try {
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = stream?.readBytes()?.toString(Charsets.UTF_8) ?: ""
            if (code == 403) throw IllegalStateException("rate limited (HTTP 403)")
            if (code !in 200..299 || resp.isBlank()) {
                throw IllegalStateException("HTTP $code: ${resp.take(300)}")
            }
            return resp
        } finally {
            conn.disconnect()
        }
    }

    /**
     * SSLSocketFactory che connette a un IP pinnato ma presenta SNI/verifica
     * per l'hostname originale. Il certificato resta sempre verificato.
     */
    internal class PinFactory(private val sniHost: String) : javax.net.ssl.SSLSocketFactory() {
        private val delegate = getDefault() as javax.net.ssl.SSLSocketFactory

        private fun wrap(raw: java.net.Socket, port: Int): java.net.Socket {
            val ssl = delegate.createSocket(raw, sniHost, port, true) as javax.net.ssl.SSLSocket
            val params = ssl.sslParameters ?: javax.net.ssl.SSLParameters()
            params.serverNames = listOf(javax.net.ssl.SNIHostName(sniHost))
            params.endpointIdentificationAlgorithm = "HTTPS"
            ssl.sslParameters = params
            return ssl
        }

        override fun createSocket(s: java.net.Socket, h: String, p: Int, a: Boolean): java.net.Socket {
            if (!s.isConnected) s.connect(java.net.InetSocketAddress(sniHost, p), 20_000)
            return wrap(s, p)
        }

        override fun createSocket(h: String, p: Int): java.net.Socket {
            val raw = java.net.Socket()
            raw.connect(java.net.InetSocketAddress(sniHost, p), 20_000)
            return wrap(raw, p)
        }

        override fun createSocket(h: String, p: Int, lh: java.net.InetAddress, lp: Int): java.net.Socket =
            createSocket(h, p)

        override fun createSocket(h: java.net.InetAddress, p: Int): java.net.Socket = createSocket(h.hostAddress, p)

        override fun createSocket(h: java.net.InetAddress, p: Int, lh: java.net.InetAddress, lp: Int): java.net.Socket =
            createSocket(h.hostAddress, p)

        override fun createSocket(): java.net.Socket = java.net.Socket()

        override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
    }

    /** Risolve un hostname via DNS-over-HTTPS (bypassa il DNS filtrato del telefono). */
    internal fun dohResolve(host: String): String? {
        // 1) DoH via hostname (se il DNS di sistema risolve google/cloudflare).
        for (u in listOf(
            "https://dns.google/resolve?name=$host&type=A",
            "https://cloudflare-dns.com/dns-query?name=$host&type=A",
        )) {
            try {
                parseDoh(URL(u).readTextWithTimeout())?.let { return it }
            } catch (_: Exception) {
            }
        }
        // 2) DoH via IP letterale (niente DNS di mezzo).
        for ((ip, name) in listOf("8.8.8.8" to "dns.google", "1.1.1.1" to "cloudflare-dns.com")) {
            try {
                val conn = (URL("https://$ip/resolve?name=$host&type=A").openConnection() as javax.net.ssl.HttpsURLConnection)
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/dns-json")
                conn.sslSocketFactory = PinFactory(name)
                conn.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, session ->
                    javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(name, session)
                }
                val body = conn.inputStream.readBytes().toString(Charsets.UTF_8)
                conn.disconnect()
                parseDoh(body)?.let { return it }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun URL.readTextWithTimeout(): String {
        val c = openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/dns-json")
        try {
            return c.inputStream.readBytes().toString(Charsets.UTF_8)
        } finally {
            c.disconnect()
        }
    }

    /** Estrae il primo record A dalla risposta DNS-JSON. null se assente. */
    internal fun parseDoh(json: String): String? {
        return try {
            val m = Regex("\"type\"\\s*:\\s*1[^}]*?\"data\"\\s*:\\s*\"([0-9.]+)\"").find(json)
                ?: Regex("\"data\"\\s*:\\s*\"([0-9.]+)\"[^}]*?\"type\"\\s*:\\s*1").find(json)
                ?: return null
            val ip = m.groupValues[1]
            if (ip.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) ip else null
        } catch (_: Exception) {
            null
        }
    }

    /** Estrae il "code" OAuth dal body POST o dalla query string. null se assente. */
    internal fun parseCallbackCode(body: String, path: String): String? {
        for (pair in body.split("&")) {
            val eq = pair.indexOf('=')
            if (eq > 0 && pair.substring(0, eq) == "code") {
                return try {
                    java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                } catch (_: Exception) {
                    pair.substring(eq + 1)
                }
            }
        }
        val q = path.substringAfter("?", "")
        for (pair in q.split("&")) {
            val eq = pair.indexOf('=')
            if (eq > 0 && pair.substring(0, eq) == "code") {
                return try {
                    java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                } catch (_: Exception) {
                    pair.substring(eq + 1)
                }
            }
        }
        return null
    }

    fun signAuthor(csrPem: String, token: Token): String {
        val (body, ct) = multipart(
            mapOf("access_token" to token.access, "user_id" to token.userId, "platform" to "VD"),
            "author.csr", csrPem,
        )
        return post("/apis/v3/authors", body, ct)
    }

    /** Ritorna (risposta v1, PEM v3). */
    fun signDistributor(csrPem: String, token: Token, privilege: String = "Public"): Pair<String, String> {
        val base = mapOf(
            "access_token" to token.access,
            "user_id" to token.userId,
            "privilege_level" to privilege,
            "developer_type" to "Individual",
        )
        val (b1, c1) = multipart(base + ("platform" to "VD"), "distributor.csr", csrPem)
        val v1 = post("/apis/v1/distributors", b1, c1)
        val (b3, c3) = multipart(base, "distributor.csr", csrPem)
        val v3 = post("/apis/v3/distributors", b3, c3)
        return v1 to v3
    }

    // ---------- Packaging .p12 ----------

    private fun parsePemCert(pem: String): java.security.cert.X509Certificate {
        val b64 = pem.lines()
            .filter { !it.startsWith("-----") && it.isNotBlank() }
            .joinToString("")
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(java.io.ByteArrayInputStream(Base64.getMimeDecoder().decode(b64)))
            as java.security.cert.X509Certificate
    }

    private fun certFromAsset(ctx: Context, name: String): java.security.cert.X509Certificate {
        ctx.assets.open("samsung-ca/$name").use { ins ->
            val cf = CertificateFactory.getInstance("X.509")
            return cf.generateCertificate(ins) as java.security.cert.X509Certificate
        }
    }

    fun loadCaAsset(ctx: Context, name: String): java.security.cert.X509Certificate = certFromAsset(ctx, name)

    /** Impacchetta chiave + foglia + CA in un PKCS#12 leggibile dall'app. */
    fun packP12(
        key: PrivateKey,
        leafPem: String,
        ca: java.security.cert.X509Certificate,
        alias: String,
        password: String,
    ): ByteArray {
        val leaf = parsePemCert(leafPem)
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)
        ks.setKeyEntry(alias, key, password.toCharArray(), arrayOf(leaf, ca))
        val out = ByteArrayOutputStream()
        ks.store(out, password.toCharArray())
        return out.toByteArray()
    }

    fun authorCa(ctx: Context) = certFromAsset(ctx, "vd_tizen_dev_author_ca.cer")
    fun distributorCa(ctx: Context, partner: Boolean) =
        certFromAsset(ctx, if (partner) "vd_tizen_dev_partner2.crt" else "vd_tizen_dev_public2.crt")

    fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }

    /** DUID dalla TV via SDB (0 getduid, fallback getduidgadget/duid-gadget). */
    fun readDuidFromTv(shell: (String) -> String): String? {
        val cmds = listOf("0 getduid", "0 getduidgadget", "/opt/etc/duid-gadget 2.0 2> /dev/null")
        for (c in cmds) {
            try {
                val out = shell(c)
                val line = out.lineSequence()
                    .map { it.trim().trim('"', '\'') }
                    .firstOrNull { it.isNotEmpty() && !it.startsWith("error", true) }
                    ?: continue
                normalizeDuid(line)?.let { return it }
            } catch (_: Exception) {
            }
        }
        return null
    }
}
