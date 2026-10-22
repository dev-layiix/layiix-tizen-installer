package com.layiix.tizenbridge.log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log locale-first: buffer in memoria (max 300 voci) + file interno
 * (max ~200 KB con troncamento). Nessun upload, nessuna analytics.
 * Non registrare mai password, certificati o contenuti di pacchetti.
 */
class AppLogStore(private val dir: File) {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    data class Entry(
        val timeMs: Long,
        val level: Level,
        val tag: String,
        val message: String,
    ) {
        fun formatted(): String {
            val df = SimpleDateFormat("HH:mm:ss", Locale.ITALIAN)
            return "[${df.format(Date(timeMs))}] ${level.name}/$tag: $message"
        }
    }

    companion object {
        const val MAX_ENTRIES = 300
        const val MAX_FILE_BYTES = 200L * 1024L
        const val FILE_NAME = "layiix-installer.log"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val logFile: File get() = File(dir, FILE_NAME)

    fun add(level: Level, tag: String, message: String) {
        val safe = message.take(2000)
        val entry = Entry(System.currentTimeMillis(), level, tag.take(32), safe)
        val updated = (_entries.value + entry).takeLast(MAX_ENTRIES)
        _entries.value = updated
        scope.launch {
            try {
                appendToFile(entry)
            } catch (_: Exception) {
            }
        }
    }

    fun info(tag: String, msg: String) = add(Level.INFO, tag, msg)
    fun warn(tag: String, msg: String) = add(Level.WARN, tag, msg)
    fun error(tag: String, msg: String) = add(Level.ERROR, tag, msg)

    fun clear() {
        _entries.value = emptyList()
        scope.launch {
            try {
                if (logFile.exists()) logFile.delete()
            } catch (_: Exception) {
            }
        }
    }

    fun copyText(): String = _entries.value.joinToString("\n") { it.formatted() }

    fun loadFromFile() {
        scope.launch {
            try {
                val f = logFile
                if (!f.exists()) return@launch
                val lines = f.readLines().takeLast(MAX_ENTRIES)
                // Ricostruzione best-effort come INFO: il dettaglio resta nel file.
                val restored = lines.map {
                    Entry(System.currentTimeMillis(), Level.INFO, "file", it.take(2000))
                }
                _entries.value = (_entries.value + restored).takeLast(MAX_ENTRIES)
            } catch (_: Exception) {
            }
        }
    }

    private fun appendToFile(entry: Entry) {
        try {
            if (!dir.exists()) dir.mkdirs()
            val f = logFile
            if (f.exists() && f.length() > MAX_FILE_BYTES) {
                val lines = f.readLines().takeLast(MAX_ENTRIES / 2)
                f.writeText(lines.joinToString("\n") + "\n")
            }
            f.appendText(entry.formatted() + "\n")
        } catch (_: Exception) {
        }
    }
}
