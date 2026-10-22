# LΛYIIX // Tizen Bridge

**CONNECT. INSTALL. ENJOY.**

Installa pacchetti Tizen (`.wgt`/`.tpk`) su TV Samsung **dal telefono Android**,
via rete Wi-Fi — **senza PC**, senza Tizen Studio, senza cavi.

- Package: `com.layiix.tizenbridge` (label Android: "Tizen Bridge") · versione 1.11.0
- Catalogo: 38 app verificate (le non firmate vengono firmate in automatico).
- Guida primo avvio: chiede marca/anno TV, verifica Tizen e blocca
  l'installazione su piattaforme non-Tizen (LG webOS, Android TV, Orsay).
- Lingua: automatica dal sistema, modificabile in Impostazioni
  (Altro → Impostazioni e info → Lingua).
- **Catalogo integrato**: 18 app pre-firmate verificate (Jellyfin, TizenBrew,
  Moonlight, Stremio…), download + installazione diretta, più link
  all'archivio community (50+ app).
- Stile layiix.com: kickers mono, badge di stato, card allineate, footer brand.
- "Sviluppato da LΛYIIX" in app con link al portfolio https://layiix.com.
- 5 lingue automatiche dal sistema: italiano, inglese, spagnolo, portoghese, russo.
- minSdk 26 (Android 8+) · targetSdk/compileSdk 35
- Solo rete locale, nessun account, nessuna analytics, nessun upload. Log solo sul telefono.
- Tema dark Layiix.

## Scarica

**https://workspace.layiix.com/layiix-tizen-installer.apk**

Apri il link dal telefono → Installa (consenti "app sconosciute" una volta sola).

## Prerequisiti

- JDK 17+ (testato: Temurin 21)
- Android SDK con platform 35 + build-tools 35.0.0
- Gradle 8.7 (`gradlew.bat` scarica la distribuzione da solo)

## Build

```bat
cd layiix-tizen-installer
gradlew.bat assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

Test unitari:

```bat
gradlew.bat testDebugUnitTest
```

Report: `app\build\reports\tests\testDebugUnitTest\index.html`

## Installazione APK

```bat
adb install app\build\outputs\apk\debug\app-debug.apk
```

Oppure copia l'APK sul telefono e aprilo (consenti "installa app sconosciute").

## Uso

1. **TV → Developer Mode**: apri *Apps*, premi `1 2 3 4 5`, attiva *Developer Mode*,
   imposta come *Host IP* l'IP Wi-Fi del telefono (lo vedi in *Altro → Diagnostica*),
   riavvia la TV tenendo premuto Power 5 s e accetta la richiesta di connessione.
2. **Connessione**: inserisci IP (e porta, default `26101`) oppure usa *Scansiona rete*.
   Lo stato mostra "Connesso" solo se l'handshake SDB riesce davvero.
3. **Installazione**: *Installa → Scegli file .wgt/.tpk* (viene validato: estensione,
   firma ZIP, manifest, firma). Premi *Installa sulla TV* e conferma.
4. **Log**: copia/cancella; **Diagnostica**: IP telefono, Wi-Fi, raggiungibilità,
   capability SDB.

Telefono e TV devono stare sulla **stessa rete Wi-Fi**.

## Troubleshooting

| Sintomo | Causa probabile | Soluzione |
|---|---|---|
| Porta chiusa / timeout | Developer Mode spento o IP errato | Guida in app (schermata TV) |
| Handshake fallito | Autorizzazione non concessa | Accetta il prompt sulla TV |
| Errore -11 | Pacchetto non firmato | Usa build pre-firmate / certificato Samsung |
| Errore -12 | Certificato di un'altra TV (DUID) | Pacchetto firmato per questa TV |
| Errore -14 | Privilegi Partner/Piattaforma | Non aggirabile: usa app compatibili |
| Installazione "non confermata" | La TV non registra l'app | Riavvia TV, spazio libero, riprova |

## Limiti noti (v1.0.0)

- **Nessun test su TV fisica**: connessione/handshake/trasferimento usano il
  protocollo SDB documentato, ma l'ultimo passo richiede verifica reale.
- La firma Tizen può bloccare i pacchetti: l'app non crea certificati.
- Nessun catalogo app: solo pacchetti locali scelti dall'utente.
- Scansione LAN best-effort (alcune reti la bloccano).
- File letti in memoria fino a 250 MB (limite telefono).

## Licenze / crediti

Codice proprio: MIT (vedi `LICENSE`). Il protocollo SDB è stato reimplementato
studiando i progetti MIT `Suz41/Sideload-Tizen-by-Android` e
`Apps2Samsung/Apps2Samsung`: attribuzioni in `NOTICE`.
