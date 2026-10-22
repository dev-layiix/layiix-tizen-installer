package com.layiix.tizenbridge.prefs

import android.content.Context
import java.io.File
import java.security.cert.X509Certificate

/**
 * Certificati di firma (.p12 di Tizen Studio) in storage interno privato.
 * author.p12 + distributor.p12 con le rispettive password.
 * Se l'utente non importa i suoi, si usano quelli condivisi integrati nell'app.
 */
object CertStore {
    const val AUTHOR = "author"
    const val DIST = "distributor"

    // Certificati condivisi integrati (firma immediata, senza import).
    const val BUNDLED_AUTHOR_PWD = "layiix-community-01"
    const val BUNDLED_DIST_PWD = "tizenpkcs12passfordsigner"

    fun bundled(ctx: Context, kind: String): Pair<ByteArray, String>? {
        return try {
            val name = if (kind == AUTHOR) "author.p12" else "distributor.p12"
            val bytes = ctx.assets.open("certs/$name").use { it.readBytes() }
            val pwd = if (kind == AUTHOR) BUNDLED_AUTHOR_PWD else BUNDLED_DIST_PWD
            // Valida subito: se il bundle è corrotto, niente firma silenziosa rotta.
            com.layiix.tizenbridge.pkg.TizenSigner.loadP12(bytes, pwd)
            bytes to pwd
        } catch (_: Exception) {
            null
        }
    }

    /** Importato dall'utente se presente, altrimenti quello integrato. */
    fun effective(ctx: Context, kind: String): Pair<ByteArray, String>? =
        load(ctx, kind) ?: bundled(ctx, kind)

    fun hasEffective(ctx: Context): Boolean =
        effective(ctx, AUTHOR) != null && effective(ctx, DIST) != null

    private fun dir(ctx: Context): File = File(ctx.filesDir, "certs").apply { mkdirs() }
    private fun file(ctx: Context, kind: String): File = File(dir(ctx), "$kind.p12")
    private fun pwdKey(kind: String): String = "cert_${kind}_pwd"

    fun has(ctx: Context, kind: String): Boolean = file(ctx, kind).exists()

    fun hasAll(ctx: Context): Boolean = has(ctx, AUTHOR) && has(ctx, DIST)

    fun save(ctx: Context, kind: String, p12: ByteArray, password: String) {
        file(ctx, kind).writeBytes(p12)
        ctx.getSharedPreferences("layiix_prefs", Context.MODE_PRIVATE)
            .edit().putString(pwdKey(kind), password).apply()
    }

    fun load(ctx: Context, kind: String): Pair<ByteArray, String>? {
        val f = file(ctx, kind)
        if (!f.exists()) return null
        val pwd = ctx.getSharedPreferences("layiix_prefs", Context.MODE_PRIVATE)
            .getString(pwdKey(kind), "") ?: ""
        return f.readBytes() to pwd
    }

    /** "CN=nome · scade 2027-01-01" o null. Non lancia. */
    fun info(ctx: Context, kind: String): String? {
        return try {
            val (bytes, pwd) = load(ctx, kind) ?: return null
            val cert = com.layiix.tizenbridge.pkg.TizenSigner.loadP12(bytes, pwd).chain.first()
            val cn = cert.subjectX500Principal.name.split(",")
                .firstOrNull { it.trim().startsWith("CN=") }
                ?.substringAfter("CN=")?.trim() ?: "?"
            val exp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format((cert as X509Certificate).notAfter)
            "$cn · $exp"
        } catch (_: Exception) {
            null
        }
    }

    fun clear(ctx: Context) {
        try {
            file(ctx, AUTHOR).delete()
            file(ctx, DIST).delete()
            ctx.getSharedPreferences("layiix_prefs", Context.MODE_PRIVATE).edit()
                .remove(pwdKey(AUTHOR)).remove(pwdKey(DIST)).apply()
        } catch (_: Exception) {
        }
    }
}
