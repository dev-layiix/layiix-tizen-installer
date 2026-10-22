package com.layiix.tizenbridge.catalog

import com.layiix.tizenbridge.R

/**
 * Catalogo curato: solo URL ufficiali verificati (HEAD 200 il 22/10/2026,
 * firma e required_version letti dai manifest reali). Le app non firmate
 * vengono firmate in-app con i certificati integrati prima dell'invio.
 * Niente link non verificati: ogni voce è revisionata. Il download passa
 * comunque per PackageValidator prima dell'invio alla TV.
 */
data class CatalogApp(
    val id: String,
    val name: String,
    val fileName: String,
    val url: String,
    val minTizen: String,
    val category: String,
    val descRes: Int,
)

object CatalogApps {
    const val ARCHIVE_URL = "https://github.com/Apps2Samsung/tizen-community-packages/releases"

    val all: List<CatalogApp> = listOf(
        CatalogApp(
            "tizenbrew", "TizenBrew", "TizenBrew.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/TizenBrew.wgt",
            "4.0", "Framework", R.string.cat_d_tizenbrew,
        ),
        CatalogApp(
            "jellyfin", "Jellyfin TV", "Jellyfin.wgt",
            "https://github.com/jeppevinkel/jellyfin-tizen-builds/releases/latest/download/Jellyfin.wgt",
            "5.0", "Streaming", R.string.cat_d_jellyfin,
        ),
        CatalogApp(
            "jellyfin-osa", "Jellyfin OSA", "Jellyfin-OSA.wgt",
            "https://github.com/jeppevinkel/jellyfin-tizen-builds/releases/latest/download/Jellyfin-OSA.wgt",
            "5.0", "Streaming", R.string.cat_d_osa,
        ),
        CatalogApp(
            "jellyfin-grayfix", "Jellyfin GrayFix", "Jellyfin-GrayFix.wgt",
            "https://github.com/jeppevinkel/jellyfin-tizen-builds/releases/latest/download/Jellyfin-GrayFix.wgt",
            "5.0", "Streaming", R.string.cat_d_grayfix,
        ),
        CatalogApp(
            "jellyfin-oblong", "Jellyfin Oblong", "Jellyfin-OblongIcon.wgt",
            "https://github.com/jeppevinkel/jellyfin-tizen-builds/releases/latest/download/Jellyfin-OblongIcon.wgt",
            "5.0", "Streaming", R.string.cat_d_oblong,
        ),
        CatalogApp(
            "jellyfin-secondary", "Jellyfin 2a istanza", "Jellyfin-secondary.wgt",
            "https://github.com/jeppevinkel/jellyfin-tizen-builds/releases/latest/download/Jellyfin-secondary.wgt",
            "5.0", "Streaming", R.string.cat_d_secondary,
        ),
        CatalogApp(
            "jellyfin-legacy", "Jellyfin Legacy", "Jellyfin.wgt",
            "https://github.com/jeppevinkel/jellyfin-tizen-builds/releases/download/2024-10-27-1821/Jellyfin.wgt",
            "2.4", "Streaming", R.string.cat_d_legacy,
        ),
        CatalogApp(
            "pelagica", "Pelagica TV", "pelagica-tizen-4.11.1.wgt",
            "https://github.com/PelagicaApp/pelagica/releases/download/4.11.1/pelagica-tizen-4.11.1.wgt",
            "5.0", "Streaming", R.string.cat_d_pelagica,
        ),
        CatalogApp(
            "stremio", "Stremio TV", "Stremio-Tizen4.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Stremio-Tizen4.wgt",
            "4.0", "Streaming", R.string.cat_d_stremio,
        ),
        CatalogApp(
            "twitch", "SmartTV Twitch", "SmartTV_Twitch.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/SmartTV_Twitch.wgt",
            "5.0", "Streaming", R.string.cat_d_twitch,
        ),
        CatalogApp(
            "moonlight", "Moonlight TV", "Moonlight-Tizen.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Moonlight-Tizen.wgt",
            "5.5", "Gaming", R.string.cat_d_moonlight,
        ),
        CatalogApp(
            "chiaki", "Chiaki", "Chiaki-Tizen.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Chiaki-Tizen.wgt",
            "5.5", "Gaming", R.string.cat_d_chiaki,
        ),
        CatalogApp(
            "doom", "Doom", "Doom.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Doom.wgt",
            "4.0", "Gaming", R.string.cat_d_doom,
        ),
        CatalogApp(
            "gameboy", "GameBoy", "GameBoy-Emulator.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/GameBoy-Emulator.wgt",
            "4.0", "Gaming", R.string.cat_d_gameboy,
        ),
        CatalogApp(
            "airtizen", "AirTizen", "AirTizen.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/AirTizen.wgt",
            "5.5", "Utility", R.string.cat_d_airtizen,
        ),
        CatalogApp(
            "fcast", "FCastReceiver", "FCastReceiver.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/FCastReceiver.wgt",
            "5.0", "Utility", R.string.cat_d_fcast,
        ),
        CatalogApp(
            "tailscale", "Tailscale", "Tailscale.tpk",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Tailscale.tpk",
            "5.0", "Utility", R.string.cat_d_tailscale,
        ),
        CatalogApp(
            "iperf3", "iperf3", "iperf3-TV.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/iperf3-TV.wgt",
            "4.0", "Utility", R.string.cat_d_iperf3,
        ),
        CatalogApp(
            "plex", "Plex Flixor", "Plex-Flixor.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Plex-Flixor.wgt",
            "2.4", "Streaming", R.string.cat_d_plex,
        ),
        CatalogApp(
            "lampa", "Lampa", "Lampa.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Lampa.wgt",
            "2.3", "Streaming", R.string.cat_d_lampa,
        ),
        CatalogApp(
            "stremiok", "StremioK", "StremioK.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/StremioK.wgt",
            "3.0", "Streaming", R.string.cat_d_stremiok,
        ),
        CatalogApp(
            "aiostreams", "AIOStreams", "AIOStreams-tizen6.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/AIOStreams-tizen6.wgt",
            "2.3", "Streaming", R.string.cat_d_aiostreams,
        ),
        CatalogApp(
            "torrstreaming", "TorrStreaming", "TorrStreaming.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/TorrStreaming.wgt",
            "2.4", "Utility", R.string.cat_d_torrstreaming,
        ),
        CatalogApp(
            "yt55", "YouTube (no ads)", "tizen-youtube-tizen-5.5.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/tizen-youtube-tizen-5.5.wgt",
            "5.5", "Streaming", R.string.cat_d_yt55,
        ),
        CatalogApp(
            "yt50", "YouTube (no ads, 2019)", "tizen-youtube-tizen-5.0.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/tizen-youtube-tizen-5.0.wgt",
            "5.0", "Streaming", R.string.cat_d_yt50,
        ),
        CatalogApp(
            "tubearchivist", "TubeArchivist", "TubeArchivist-TV.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/TubeArchivist-TV.wgt",
            "2.4", "Streaming", R.string.cat_d_tubearchivist,
        ),
        CatalogApp(
            "bonfire", "Jellyfin Bonfire", "Jellyfin-Bonfire.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Jellyfin-Bonfire.wgt",
            "2.3", "Streaming", R.string.cat_d_bonfire,
        ),
        CatalogApp(
            "reiverr", "Reiverr", "Reiverr.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Reiverr.wgt",
            "2.3", "Streaming", R.string.cat_d_reiverr,
        ),
        CatalogApp(
            "nuvio", "Nuvio TV", "NuvioTV-Official.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/NuvioTV-Official.wgt",
            "2.3", "Streaming", R.string.cat_d_nuvio,
        ),
        CatalogApp(
            "tessel", "Tessel Player", "Tessel.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Tessel.wgt",
            "4.0", "Streaming", R.string.cat_d_tessel,
        ),
        CatalogApp(
            "ottplay", "OTTplay", "OTTplay-FOSS.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/OTTplay-FOSS.wgt",
            "2.3", "IPTV", R.string.cat_d_ottplay,
        ),
        CatalogApp(
            "floh", "FLOH IPTV", "FLOH-IPTV.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/FLOH-IPTV.wgt",
            "6.0", "IPTV", R.string.cat_d_floh,
        ),
        CatalogApp(
            "retroarch", "RetroArch", "RetroArch.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/RetroArch.wgt",
            "6.5", "Gaming", R.string.cat_d_retroarch,
        ),
        CatalogApp(
            "steamlink", "Steam Link", "TizenSteamLink.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/TizenSteamLink.wgt",
            "5.5", "Gaming", R.string.cat_d_steamlink,
        ),
        CatalogApp(
            "tizenutility", "TizenUtility",             "TizenUtility.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/TizenUtility.wgt",
            "1.0", "Utility", R.string.cat_d_tizenutility,
        ),
        CatalogApp(
            "appmanager", "AppManager", "AppManager.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/AppManager.wgt",
            "2.4", "Utility", R.string.cat_d_appmanager,
        ),
        CatalogApp(
            "go2rtc", "go2rtc Camere", "go2rtc-tizen.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/go2rtc-tizen.wgt",
            "3.0", "Utility", R.string.cat_d_go2rtc,
        ),
        CatalogApp(
            "sonance", "Sonance Musica", "Sonance3.wgt",
            "https://github.com/Apps2Samsung/tizen-community-packages/releases/latest/download/Sonance3.wgt",
            "5.0", "Streaming", R.string.cat_d_sonance,
        ),
    )

    val categories: List<String> = all.map { it.category }.distinct()
}
