package com.layiix.tizenbridge.pkg

/**
 * Validazione pacchetti Tizen (.wgt / .tpk) prima dell'invio.
 * Logica pura (JVM) separata dalla lettura SAF Android per la testabilità.
 */
object PackageValidator {

    const val MAX_PACKAGE_BYTES = 500L * 1024L * 1024L // 500 MB
    const val MAX_ENTRIES = 20_000

    enum class Kind { WGT, TPK }

    /** Messaggi utente. Default italiano (per test JVM); l'app passa quelli localizzati. */
    data class PkgStrings(
        val apk: String,
        val ext: String,
        val empty: String,
        val big: String,
        val nozip: String,
        val many: String,
        val noconfig: String,
        val nomanifest: String,
        val nosignWarn: String,
        val noidWarn: String,
    ) {
        companion object {
            fun it() = PkgStrings(
                apk = "Gli APK Android non sono pacchetti Tizen: serve un file .wgt o .tpk.",
                ext = "Estensione non supportata: usa un pacchetto .wgt (web) o .tpk (nativo).",
                empty = "Il file è vuoto (0 byte).",
                big = "Il file supera il limite di 500 MB.",
                nozip = "Il file non è un archivio ZIP valido (firma PK mancante).",
                many = "Archivio con troppe voci: possibile file anomalo.",
                noconfig = "config.xml mancante: non è un pacchetto .wgt valido.",
                nomanifest = "tizen-manifest.xml mancante: non è un pacchetto .tpk valido.",
                nosignWarn = "Pacchetto non firmato: la TV lo rifiuterà con errore -11, " +
                    "salvo certificato Samsung valido per questa TV.",
                noidWarn = "ID applicazione non trovato nel manifest: installazione a rischio.",
            )
        }
    }

    data class ValidationResult(
        val ok: Boolean,
        val kind: Kind?,
        val fileName: String,
        val sizeBytes: Long,
        val appId: String?,
        val packageId: String?,
        val signed: Boolean,
        /** Versione Tizen minima richiesta dal manifest (es. "6.0"), null se assente. */
        val requiredVersion: String?,
        val errors: List<String>,
        val warnings: List<String>,
    )

    fun detectKind(fileName: String): Kind? {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".wgt") -> Kind.WGT
            lower.endsWith(".tpk") -> Kind.TPK
            else -> null
        }
    }

    /**
     * @param magic2 primi 2 byte del file (firma ZIP = 'P','K').
     * @param entries nomi delle entry ZIP (streaming, senza caricare il file).
     * @param manifestXml contenuto di config.xml o tizen-manifest.xml, se trovato.
     */
    fun validate(
        fileName: String,
        sizeBytes: Long,
        magic2: ByteArray?,
        entries: List<String>,
        manifestXml: String?,
        msgs: PkgStrings = PkgStrings.it(),
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val shortName = fileName.substringAfterLast('/').substringAfterLast('\\').take(128)

        val kind = detectKind(shortName)
        if (kind == null) {
            if (shortName.lowercase().endsWith(".apk")) {
                errors.add(msgs.apk)
            } else {
                errors.add(msgs.ext)
            }
        }
        if (sizeBytes <= 0) errors.add(msgs.empty)
        if (sizeBytes > MAX_PACKAGE_BYTES) errors.add(msgs.big)
        if (magic2 == null || magic2.size < 2 || magic2[0] != 0x50.toByte() || magic2[1] != 0x4B.toByte()) {
            errors.add(msgs.nozip)
        }
        if (entries.size > MAX_ENTRIES) errors.add(msgs.many)

        val names = entries.toSet()
        val hasConfig = "config.xml" in names
        val hasTizenManifest = "tizen-manifest.xml" in names
        if (kind == Kind.WGT && !hasConfig) {
            errors.add(msgs.noconfig)
        }
        if (kind == Kind.TPK && !hasTizenManifest) {
            errors.add(msgs.nomanifest)
        }

        val signed = "author-signature.xml" in names || "signature1.xml" in names
        if (!signed && errors.isEmpty()) {
            warnings.add(msgs.nosignWarn)
        }

        var appId: String? = null
        var packageId: String? = null
        var requiredVersion: String? = null
        if (manifestXml != null) {
            appId = parseAppId(manifestXml)
            packageId = parsePackageId(manifestXml) ?: appId?.substringBefore('.')
            requiredVersion = parseRequiredVersion(manifestXml)
            if (appId == null) {
                warnings.add(msgs.noidWarn)
            }
        }

        return ValidationResult(
            ok = errors.isEmpty(),
            kind = kind,
            fileName = shortName,
            sizeBytes = sizeBytes,
            appId = appId,
            packageId = packageId,
            signed = signed,
            requiredVersion = requiredVersion,
            errors = errors,
            warnings = warnings,
        )
    }

    /** Cerca id="..." nel tag <application ...> (config.xml dei .wgt, anche con prefisso tizen:). */
    fun parseAppId(manifestXml: String): String? {
        val m = Regex("""<(?:\w+:)?application\b[^>]*\bid\s*=\s*"([^"]+)"""").find(manifestXml)
        if (m != null) return m.groupValues[1].trim().takeIf { it.isNotEmpty() }
        val ui = Regex("""<(?:\w+:)?ui-application\b[^>]*\bappid\s*=\s*"([^"]+)"""").find(manifestXml)
        if (ui != null) return ui.groupValues[1].trim().takeIf { it.isNotEmpty() }
        val svc = Regex("""<(?:\w+:)?service-application\b[^>]*\bappid\s*=\s*"([^"]+)"""").find(manifestXml)
        return svc?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun parsePackageId(manifestXml: String): String? {
        val m = Regex("""<manifest\b[^>]*\bpackage\s*=\s*"([^"]+)"""").find(manifestXml)
        if (m != null) return m.groupValues[1].trim().takeIf { it.isNotEmpty() }
        val w = Regex("""<widget\b[^>]*\bid\s*=\s*"([^"]+)"""").find(manifestXml)
        return w?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Legge required_version="..." dal tag <application> (anche con prefisso tizen:). */
    fun parseRequiredVersion(manifestXml: String): String? {
        val m = Regex("""<(?:\w+:)?application\b[^>]*\brequired_version\s*=\s*"([^"]+)"""").find(manifestXml)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Confronta la versione Tizen richiesta con quella della TV.
     * @return true se compatibile, false se la TV è più vecchia, null se non confrontabili.
     */
    fun isCompatible(required: String?, tv: String?): Boolean? {
        val req = splitVersion(required) ?: return null
        val cur = splitVersion(tv) ?: return null
        val n = maxOf(req.size, cur.size)
        for (i in 0 until n) {
            val r = req.getOrElse(i) { 0 }
            val c = cur.getOrElse(i) { 0 }
            if (c < r) return false
            if (c > r) return true
        }
        return true
    }

    private fun splitVersion(v: String?): List<Int>? {
        if (v.isNullOrBlank()) return null
        // Prende il prefisso numerico (es. "6.5" da "6.5" o "Tizen 6.5").
        val m = Regex("""(\d+(?:\.\d+)*)""").find(v) ?: return null
        return m.groupValues[1].split('.').mapNotNull { it.toIntOrNull() }.takeIf { it.isNotEmpty() }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
        return String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
    }
}
