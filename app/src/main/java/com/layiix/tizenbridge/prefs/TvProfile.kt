package com.layiix.tizenbridge.prefs

import android.content.Context

/**
 * Profilo TV dell'utente (wizard primo avvio).
 * brand: "" = non scelto, "samsung", "lg", "android", "other".
 * year: "" = ignoto/saltato, "2025".."2015", "legacy" = 2014 o prima.
 * Mapping anno→Tizen da specifiche ufficiali Samsung Developer
 * (2025=9.0, 2024=8.0, 2023=7.0, 2022=6.5, 2021=6.0, 2020=5.5,
 *  2019=5.0, 2018=4.0, 2017=3.0, 2016=2.4, 2015=2.3).
 */
object TvProfile {
    const val SAMSUNG = "samsung"
    const val LG = "lg"
    const val ANDROID = "android"
    const val OTHER = "other"
    const val LEGACY = "legacy"
    const val UNKNOWN = ""
    val YEARS = listOf("2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017", "2016", "2015")

    /** null = versione ignota o piattaforma non-Tizen. Funzione pura, testabile su JVM. */
    fun tizenForYear(year: String): String? = when (year) {
        "2025" -> "9.0"
        "2024" -> "8.0"
        "2023" -> "7.0"
        "2022" -> "6.5"
        "2021" -> "6.0"
        "2020" -> "5.5"
        "2019" -> "5.0"
        "2018" -> "4.0"
        "2017" -> "3.0"
        "2016" -> "2.4"
        "2015" -> "2.3"
        else -> null
    }

    /** Mapping inverso: Tizen rilevato dalla TV → anno (biunivoco). null se ignoto. */
    fun yearForTizen(tizen: String?): String? {
        if (tizen.isNullOrBlank()) return null
        val m = Regex("""(\d+(?:\.\d+)*)""").find(tizen) ?: return null
        val v = m.groupValues[1].split('.').take(2).joinToString(".")
        return when (v) {
            "9.0" -> "2025"
            "8.0" -> "2024"
            "7.0" -> "2023"
            "6.5" -> "2022"
            "6.0" -> "2021"
            "5.5" -> "2020"
            "5.0" -> "2019"
            "4.0" -> "2018"
            "3.0" -> "2017"
            "2.4" -> "2016"
            "2.3" -> "2015"
            else -> null
        }
    }
    /** true se l'installazione .wgt va bloccata con spiegazione. Funzione pura. */
    fun isBlocked(brand: String, year: String): Boolean = when (brand) {
        LG, ANDROID, OTHER -> true
        SAMSUNG -> year == LEGACY
        else -> false
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("layiix_prefs", Context.MODE_PRIVATE)

    fun isDone(ctx: Context): Boolean =
        prefs(ctx).getBoolean("tv_done", false)

    fun brand(ctx: Context): String =
        prefs(ctx).getString("tv_brand", "") ?: ""

    fun year(ctx: Context): String =
        prefs(ctx).getString("tv_year", "") ?: ""

    fun save(ctx: Context, brand: String, year: String) {
        prefs(ctx).edit()
            .putString("tv_brand", brand)
            .putString("tv_year", year)
            .putBoolean("tv_done", true)
            .apply()
    }
}
