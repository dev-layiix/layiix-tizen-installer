# BUILD REPORT — Layiix Tizen Installer v1.0.0

Data: 20/10/2026 · Progetto: `layiix-tizen-installer/` (sorgenti reali, nessun placeholder)

## Ambiente verificato

- OS: Windows 10 Pro 64 bit · JDK Temurin 21.0.10 (`JAVA_HOME` Eclipse Adoptium)
- Android SDK: `C:\Users\Layiix\AppData\Local\Android\Sdk`
  - platform-tools 37.0.1 · build-tools 35.0.0 · platforms android-35/36 · licenze accettate
- **Android Studio NON trovato** (contrariamente al brief): build eseguita da riga
  di comando con **Gradle 8.7** (distribuzione ufficiale, via BITS),
  AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.10.00.
- Nota agenti: ambiente senza subagenti — lavoro svolto in 4 fasi equivalenti
  (ispezione → ricerca → implementazione → QA/build → revisione → consegna).

## Ricerca tecnica (Fase 2)

- `Apps2Samsung/Apps2Samsung` (MIT, ~2.3k star): esiste, installer cross-platform
  con mobile head, gestione certificati. Riferimento UX/flusso.
- `Suz41/Sideload-Tizen-by-Android` (MIT): esiste; client Python/Termux con socket
  raw verso SDB 26101 — base documentata del formato wire reimplementato in Kotlin
  (CNXN/OPEN/WRTE/OKAY/CLSE, servizi shell/sync/capability, `vd_appinstall`/`pkgcmd`).
- `TizenTeam/vlc-tizen`: citato come esempio, nessun codice incorporato.
- Nessun codice copiato: solo formato protocollo (interoperabilità) + attribuzioni in NOTICE.

## Comandi eseguiti

```
gradle.bat assembleDebug        -> BUILD SUCCESSFUL
gradle.bat testDebugUnitTest    -> 29/29 passati
aapt dump badging app-debug.apk -> package com.layiix.tizeninstaller.debug, label ok,
                                   permessi solo INTERNET/ACCESS_NETWORK_STATE/ACCESS_WIFI_STATE
adb devices                     -> nessun dispositivo collegato (install-test non possibile)
```

## Test: 29/29 passati, 0 falliti

| Suite | Test |
|---|---|
| NetworkUtilsTest | 5 (IPv4/porta/subnet/raggiungibilità localhost) |
| PackageValidatorTest | 10 (wgt/tpk, firma, APK rifiutato, ZIP, limiti, manifest) |
| SdbProtocolTest | 6 (magic/checksum/roundtrip/sync payloads) |
| TvErrorMapperTest | 5 (-11/-12/-14/rete/vuoto) |
| TvRestApiTest | 3 (parse device/dev-mode/vuoto) |

Unico fix in corso d'opera: `formatSize` con `Locale.US` (virgola italiana nei test).

## Output

- Release firmata: `app/build/outputs/apk/release/app-release.apk` (1,2 MB)
- Percorso pubblico: `A:\zietnextgen\layiix.com\layiix-tizen-installer.apk`
  → https://workspace.layiix.com/layiix-tizen-installer.apk
- Versione: 1.11.0 (versionCode 21): catalogo 18 → 38 app — 20 nuove voci
  verificate (HEAD 200 + manifest reale: firma e required_version letti dai
  file; Kodi/VideoPlayer esclusi: 60-80 MB); firma automatica in download
  per le non firmate; nuova categoria IPTV. Test 52/52.
- SHA-256: `5B325735A3C294B85B4AA93776F2461749B80DDA115C5A4B4F353A7EBA1F8326`
  (link pubblico verificato via Range 206).
- Versione: 1.10.0 (versionCode 20): certificati condivisi integrati —
  author+distributor community già dentro l'app (assets, zero setup),
  usati in automatico se l'utente non importa i suoi; rimossa la UI
  "Crea certificati" (flusso server non affidabile). Test 52/52.
- SHA-256: `C1BE46B2A3949CB8A16DB97CFC02DE7731BAD2E5F54FCB2A845884E4B36ECB0A`
  (link pubblico verificato via Range 206).
- Versione: 1.9.4 (versionCode 19): fix DUID distributor — prefisso versione
  automatico (2.0#) quando manca (era la causa del "No subjectAltNames on
  the certificate match"); CSR verificato byte-identico al riferimento
  BouncyCastle; fallback v1 se la v3 non è PEM. Test 51/51.
- SHA-256: `56F854D4950B4F1AE669CFC56F86FAD4FDC7648769C28253B97B004DC8CB6495`
  (link pubblico verificato via Range 206).
- Versione: 1.9.3 (versionCode 18): DNS-over-HTTPS di fallback per la CA
  Samsung — se il DNS del telefono non risolve svdca.samsungqbe.com
  (ad-block/DNS privato), l'app risolve via DoH e pinna l'IP con TLS sempre
  verificato (SNI + hostname). Messaggio dedicato se resta bloccato. Test 49/49.
- SHA-256: `4248106D668D36512B800E5905AFACF0BD9CDAD9079F691F9BC7FABE51F7C464`
  (link pubblico verificato via Range 206).
- Versione: 1.9.2 (versionCode 17): fix OAuth verificato empiricamente —
  il server Samsung accetta SOLO http://localhost:4794/signin/callback
  (altre porte/host = 400). Niente fallback: se la 4794 è occupata, messaggio
  dedicato. Test 47/47.
- SHA-256: `430D44A4A93B565E45C8ED235E872A196E5D2D7477022DCAD2B3E89281D52437`
  (link pubblico verificato via Range 206).
- Versione: 1.9.1 (versionCode 16): fix OAuth parziale (localhost ma porta
  dinamica) — superato dalla 1.9.2, non pubblicata.
- Versione: 1.9.0 (versionCode 15): creazione certificati Samsung dal
  telefono (niente PC) — login OAuth nel browser + CSR PKCS#10 + CA online
  Samsung (author v3 + distributor v1/v3) + packaging .p12, flusso ufficiale
  replicato da repo Samsung; DUID letto dalla TV o manuale. Test 47/47.
- SHA-256: `A9001ABBC84A6C17B924ADFBF58D0A1D39BE410A0CD54A9708904E054CA61C7D`
  (link pubblico verificato via Range 206).
- Versione: 1.8.0 (versionCode 14): firma .wgt in-app come Tizen Studio —
  TizenSigner (XMLDSig: SHA-512 refs ordinate, RSA-SHA512 su exc-C14N,
  #prop in C14N11, formato verificato byte-identico su Moonlight reale),
  import author.p12+distributor.p12 in Impostazioni → Firma, pulsante
  "Firma e installa" sui pacchetti non firmati. Test 43/43 (round-trip con
  verifica indipendente). R8: esclusi rami StAX/JAXB di Santuario.
- SHA-256: `C533873C98BFC90BFBDD7C2E07F29CF633767388F1AB75C27D10B624DBD4B045`
  (link pubblico verificato via Range 206, 2.21 MB).
- Versione: 1.7.2 (versionCode 13): diagnosi errori TV corretta con fonti
  community/Apps2Samsung — 116 = spazio esaurito sulla TV (passi: Device Care
  → Gestisci spazio, disinstalla, reset Smart Hub), [118,-12] = certificato
  (nuovi pattern `-12]`/`failed[118`, prima finiva in "generico"); passi
  certificato con guida Tizen Studio gratuita (Certificate Manager + DUID +
  Permit to install). Test 40/40.
- SHA-256: `CED861A838C8575756C06C210BD7CF1D02D841F41BFCC553B239ED03FA92D1FC`
  (link pubblico verificato via Range 206).
- Versione: 1.7.1 (versionCode 12): auto-completamento profilo TV —
  alla connessione l'anno viene dedotto dal Tizen rilevato (mapping
  biunivoco 5.5→2020 ecc.) e salvato; la card Home mostra subito
  "Samsung · Tizen 5.5". Test 39/39.
- SHA-256: `D6BE068E9A1A2C868E577403D2741794D8D26CA0157DD32565FFE5FD87A37222`
  (link pubblico verificato via Range 206).
- Versione: 1.7.0 (versionCode 11): wizard primo avvio "Che TV hai?"
  (marca → anno Samsung → esito; mapping anno→Tizen da specifiche ufficiali
  Samsung: 2025=9.0 … 2015=2.3, ≤2014=Orsay non-Tizen), blocco installazione
  per LG webOS/Android TV/altre/Orsay con spiegazione dedicata, card profilo
  TV in Home e Impostazioni con cambio. Test 38/38.
- SHA-256: `777E9084D4CEF459C4760481C221DBBB45F4F309D8F4ADA1099F608B26C93613`
  (link pubblico verificato via Range 206).
- Chiave release in `keystore/` + `keystore.properties`: LOCALI, mai pubblicare,
  fare backup — senza si perdono gli aggiornamenti futuri.
- v1.0.1: versione visibile in app (Altro → Informazioni) per smascherare cache stantie.
- Fix ritmo SDB (4 subagenti, 16:00): cooldown 400 ms pre-connect, retry 3x con
  backoff, validazione risposta CNXN (CLSE→errore chiaro), CLSE in chiusura,
  trace sdb nei log, pause 500/1200 ms tra fasi install. Niente auth RSA:
  le TV Samsung usano dev-mode+Host IP, non chiavi. Test 31/31.
- VERIFICA SU TV REALE (21/10): Samsung UE50TU7170 (Tizen 5.5), handshake SDB
  riuscito e pacchetto installato sulla TV dall'app. Pacchetto "Layiix TV"
  (.wgt creato dall'utente stesso). La causa del reset era il burst di
  connessioni + Host IP; fix confermato sul campo.
- Novità v1.0.0 (15:30): pre-check REST (porta 8001) prima dell'SDB — se il
  Developer Mode è spento o l'Host IP non coincide col telefono, l'app lo dice
  subito con entrambi gli IP; "connection reset" mappato alla guida dev-mode.
- Bug reale trovato e fixato (15:45): l'ID canale remoto nell'OKAY di risposta
  a OPEN è arg0, non arg1 — prima la TV resettava ogni handshake. Test 31/31.
- Novità v1.0.0 (15:30): pre-check REST (porta 8001) prima dell'SDB — se il
  Developer Mode è spento o l'Host IP non coincide col telefono, l'app lo dice
  subito con entrambi gli IP; "connection reset" mappato alla guida dev-mode.
- Report test: `app/build/reports/tests/testDebugUnitTest/index.html`

## Fix server download (21/10, pomeriggio)

Problema: download APK da `workspace.layiix.com` veloce all'inizio poi fermo.
Diagnosi (`SERVER_DIAGNOSI.md` in root sito): rete casa OK (72 Mbps up),
server OK in locale, ma niente supporto `Range` → niente resume su mobile.
Fix in `server.py` (verificati): `206 Partial Content` + `Accept-Ranges: bytes`
su file statici, `HEAD` generico, MIME `.apk` dedicato. Riavviato via
`start-layiix.bat`, testato: Range→206/1024 byte, HEAD→200.

## Revisione (Fase 5)

- Permessi manifest minimi e verificati via aapt; `usesCleartextTraffic` solo per HTTP TV locale (porta 8001).
- Sanitizzazione: IP/porta validati, ID app dal manifest filtrato a `[A-Za-z0-9._-]`,
  nomi file troncati, anti path-traversal, cap lettura 250 MB / log 300 voci / 200 KB.
- Nessun segreto, nessuna analytics, nessuna porta server sul telefono, conferma pre-installazione.
- Messaggi di successo solo a registro TV confermato; altrimenti errore esplicito "non confermata".

## Limiti e verifiche mancanti (serve TV Samsung fisica)

1. Handshake SDB reale contro TV in Developer Mode.
2. Push sync + `vd_appinstall`/`pkgcmd` end-to-end e codici -11/-12/-14 reali.
3. Avvio app su telefono/emulatore (nessun dispositivo collegato al PC).
4. REST 8001 su modelli/firmware vari (solo modelli/prove documentate: nessuna).
