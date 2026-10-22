package com.layiix.tizenbridge.prefs

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Lingua manuale dell'app. "" = segue il sistema (default), altrimenti tag ISO.
 * Applicata in attachBaseContext di Application e Activity, senza dipendenze extra.
 */
object LangPrefs {
    const val SYSTEM = ""
    val SUPPORTED = listOf("", "it", "en", "es", "pt", "ru")

    /** null = sistema. Funzione pura, testabile su JVM. */
    fun localeFor(tag: String): Locale? = when (tag) {
        "it" -> Locale.ITALIAN
        "en" -> Locale.ENGLISH
        "es" -> Locale("es")
        "pt" -> Locale("pt")
        "ru" -> Locale("ru")
        else -> null
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("layiix_prefs", Context.MODE_PRIVATE)

    fun get(ctx: Context): String =
        (prefs(ctx).getString("lang", SYSTEM) ?: SYSTEM).takeIf { it in SUPPORTED } ?: SYSTEM

    fun set(ctx: Context, tag: String) {
        prefs(ctx).edit().putString("lang", tag.takeIf { it in SUPPORTED } ?: SYSTEM).apply()
    }

    /** Ritorna un contesto con la lingua salvata (o quello originale se "sistema"). */
    fun wrap(ctx: Context): Context {
        val loc = localeFor(get(ctx)) ?: return ctx
        Locale.setDefault(loc)
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(loc)
        return ctx.createConfigurationContext(config)
    }
}
