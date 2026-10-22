package com.layiix.tizenbridge.sdb

import java.net.HttpURLConnection
import java.net.URL

/**
 * Info TV via API REST Samsung (porta 8001, nessuna autenticazione).
 * Parsing manuale senza dipendenze, testabile su JVM pura.
 */
object TvRestApi {

    const val REST_PORT = 8001

    data class TvInfo(
        val model: String = "",
        val name: String = "",
        val duid: String = "",
        val developerMode: String = "",
        val developerIp: String = "",
        val resolution: String = "",
    ) {
        val isDevMode: Boolean get() = developerMode == "1"
    }

    fun fetch(host: String, port: Int = REST_PORT, timeoutMs: Int = 2500): TvInfo? {
        return try {
            val url = URL("http://$host:$port/api/v2/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", "Layiix-Tizen-Installer/1.0")
                instanceFollowRedirects = false
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            parse(body)
        } catch (_: Exception) {
            null
        }
    }

    /** Estrae una stringa JSON "key": "value" (con o senza virgolette sul valore). */
    internal fun extractString(json: String, key: String): String {
        val q = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"([^\"]*)\"").find(json)
        if (q != null) return q.groupValues[1]
        val n = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*([0-9]+)").find(json)
        return n?.groupValues?.get(1) ?: ""
    }

    fun parse(json: String): TvInfo {
        // Il campo "device" contiene gli attributi utili; il parsing piatto basta.
        return TvInfo(
            model = extractString(json, "modelName"),
            name = extractString(json, "name"),
            duid = extractString(json, "duid").removePrefix("uuid:"),
            developerMode = extractString(json, "developerMode"),
            developerIp = extractString(json, "developerIP"),
            resolution = extractString(json, "resolution"),
        )
    }
}
