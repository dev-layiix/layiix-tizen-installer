# LΛYIIX // Tizen Bridge

![Tizen Bridge banner](assets/banner.png)

**CONNECT. INSTALL. ENJOY.**

Tizen Bridge installs Tizen packages (`.wgt` / `.tpk`) on Samsung TVs straight from an Android phone over Wi-Fi. No PC, no Tizen Studio, no cables.

## What it does

- Pushes a `.wgt` or `.tpk` to the TV over the local network (SDB, port 26101) and runs the on-TV installer.
- Ships a curated catalog of 38 verified community apps (Jellyfin, Stremio, Moonlight, Kodi remotes, IPTV players, emulators). Packages without a signature are signed inside the app before upload.
- Validates every package before sending it: extension, ZIP integrity, manifest, app ID, required Tizen version against the connected TV.
- First-run wizard asks for the TV brand and model year, estimates the Tizen version, and blocks installation on platforms the app can't serve (LG webOS, Android TV, pre-2015 Orsay).
- Speaks five languages (English, Italian, Spanish, Portuguese, Russian), picked up from the system and overridable in Settings.
- Keeps diagnostics on the device: connection log, LAN scan, SDB capability readout. No account, no analytics, no uploads.

## Requirements

- A Samsung Smart TV (Tizen) with Developer Mode on, on the same Wi-Fi as the phone.
- Android 8.0+ (minSdk 26).

## Install the APK

Download: **https://workspace.layiix.com/layiix-tizen-installer.apk**

Open the link on the phone and confirm the install. Android asks once to allow installs from unknown sources.

## Pair the TV

1. On the TV, open *Apps* and press `1 2 3 4 5` on the remote. Switch *Developer Mode* on.
2. Set the *Host IP* to the phone's Wi-Fi address. The app shows it under *More → Diagnostics*.
3. Hold Power for 5 seconds to reboot the TV, then accept the connection prompt on screen.
4. In the app, open the *TV* tab and enter the TV's IP (port defaults to `26101`), or run *Scan network*. The status card reads *ONLINE* only after a real SDB handshake.

The phone and the TV must stay on the same Wi-Fi network.

## Install an app

Pick the *Catalog* tab for a one-tap download, or *Install* to choose a local `.wgt` / `.tpk` file. The app validates the package, signs it if needed, uploads it, and reports what the TV answers — including the raw installer output when something fails.

## Build from source

Prerequisites: JDK 17 or newer (tested with Temurin 21), Android SDK with platform 35 and build-tools 35.0.0. The Gradle wrapper downloads Gradle 8.7 on its own.

```bat
cd layiix-tizen-installer
gradlew.bat assembleDebug
```

APK output: `app\build\outputs\apk\debug\app-debug.apk`

Unit tests (52, JVM-only, no device needed):

```bat
gradlew.bat testDebugUnitTest
```

Release builds are signed with a local keystore (`keystore/` + `keystore.properties`, intentionally not committed).

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Closed port / timeout | Developer Mode off, wrong IP | Follow the in-app guide on the TV tab |
| Handshake refused | Authorization prompt ignored | Accept it on the TV screen |
| Error -11 (signature) | Package unsigned | The app signs automatically; for stubborn cases import your own Samsung certificate under Settings → Signing |
| Error -12 (certificate) | Certificate issued for another TV (DUID) | Use a package signed for this TV |
| Error 116 | TV storage full | Settings → Support → Device Care → Manage Storage, then retry |
| Error -14 | Partner/platform privileges | Can't be sideloaded; pick a compatible app |
| Install "unconfirmed" | TV didn't register the app | Reboot the TV, free some space, retry |

## Known limits

- Signature checks run on the TV, not in the app. Bundled community certificates cover the catalog; packages tied to another TV's DUID still need their own certificate.
- Files are held in memory during upload (phone-side cap around 250 MB).
- LAN auto-scan is best-effort; some networks block it, so manual IP entry stays available.

## Stack

Kotlin, Jetpack Compose (Material 3), coroutines. Tizen package signing (XMLDSig) and the SDB client are implemented in-app; test coverage is unit-level over the pure logic (protocol framing, validation, signing, error mapping).

## License / credits

Own code: MIT (see `LICENSE`). The SDB protocol work was done studying the MIT-licensed projects `Suz41/Sideload-Tizen-by-Android` and `Apps2Samsung/Apps2Samsung`; attributions in `NOTICE`.

Built by [LΛYIIX](https://layiix.com).
