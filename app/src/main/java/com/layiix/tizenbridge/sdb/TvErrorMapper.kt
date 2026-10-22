package com.layiix.tizenbridge.sdb

/**
 * Classificazione degli errori TV/rete in categorie stabili + messaggi
 * utente in italiano e passi di risoluzione. Testabile su JVM pura.
 */
object TvErrorMapper {

    enum class Category {
        NETWORK,
        DEVMODE_OR_AUTH,
        SIGNATURE_11,
        CERT_12,
        PRIVILEGE_14,
        PKG_REJECTED_116,
        CONFLICT_STORAGE,
        FILE_MISSING,
        INSTALL_GENERIC,
        NONE,
    }

    data class MappedError(
        val category: Category,
        val title: String,
        val fixSteps: List<String>,
    )

    /** Messaggi utente. Default italiano (per test JVM); l'app passa quelli localizzati. */
    data class ErrStrings(
        val sig: String,
        val cert: String,
        val priv: String,
        val conflict: String,
        val file: String,
        val net: String,
        val gen: String,
        val dl116: String,
        val none: String,
        val sigSteps: List<String>,
        val certSteps: List<String>,
        val privSteps: List<String>,
        val dl116Steps: List<String>,
        val confSteps: List<String>,
        val fileSteps: List<String>,
        val netSteps: List<String>,
        val genSteps: List<String>,
    ) {
        companion object {
            fun it() = ErrStrings(
                sig = "Firma del pacchetto rifiutata (errore -11).",
                cert = "Certificato non valido per questa TV (errore -12).",
                priv = "Permessi limitati del pacchetto (errore -14).",
                conflict = "Conflitto con un'app esistente o spazio insufficiente.",
                file = "File non trovato.",
                net = "TV non raggiungibile o connessione rifiutata.",
                gen = "Errore generico della TV.",
                dl116 = "Pacchetto rifiutato dalla TV (errore 116).",
                none = "Nessun errore.",
                sigSteps = listOf(
                    "Usa un pacchetto già firmato per la tua TV (es. build pre-firmate della community).",
                    "I pacchetti non firmati richiedono un certificato Samsung valido per il DUID della TV.",
                    "Questa app non crea certificati: segui la procedura ufficiale Samsung/Tizen Studio.",
                    "Alternativa: crea un certificato Samsung gratuito con Tizen Studio " +
                        "(Certificate Manager con il DUID della TV + 'Permit to install applications'), " +
                        "firma il .wgt e reinstalla.",
                ),
                certSteps = listOf(
                    "Il certificato del pacchetto appartiene a un'altra TV (DUID diverso).",
                    "Reinstalla un pacchetto firmato con certificato valido per questa TV.",
                    "Alternativa: crea un certificato Samsung gratuito con Tizen Studio " +
                        "(Certificate Manager con il DUID della TV + 'Permit to install applications'), " +
                        "firma il .wgt e reinstalla.",
                ),
                privSteps = listOf(
                    "L'app richiede privilegi Partner/Platform non concedibili in sideload.",
                    "Verifica che la TV sia in Developer Mode con l'IP host corretto.",
                    "Spegni del tutto la TV (stacca l'alimentazione 10 s) e riprova.",
                ),
                confSteps = listOf(
                    "Disinstalla la versione precedente dell'app dalla TV.",
                    "Controlla lo spazio libero della TV.",
                    "Riavvia la TV e ripeti l'installazione.",
                ),
                fileSteps = listOf(
                    "Riseleziona il pacchetto .wgt/.tpk dal selettore file.",
                    "Non spostare o rinominare il file durante l'installazione.",
                ),
                netSteps = listOf(
                    "Su TV: app Apps → premi 1 2 3 4 5 → Developer Mode ON.",
                    "Imposta come Host IP l'IP Wi-Fi del telefono (vedi Diagnostica).",
                    "Riavvia la TV (tieni premuto Power 5 s) e accetta la richiesta di connessione.",
                    "Telefono e TV devono stare sulla stessa rete Wi-Fi.",
                ),
                genSteps = listOf(
                    "Riavvia la TV (tieni premuto Power 5 s).",
                    "Controlla il Wi-Fi della TV e riprova.",
                    "Copia il log e verifica il messaggio grezzo della TV.",
                ),
                dl116Steps = listOf(
                    "La TV non ha spazio libero: è il motivo documentato dell'errore 116. " +
                        "Libera spazio (Impostazioni → Supporto → Device Care → Gestisci spazio) e riprova.",
                    "Disinstalla le app che non usi; quelle preinstallate si rimuovono solo in parte.",
                    "Se non basta: reset dello Smart Hub (Impostazioni → Supporto → Autodiagnosi → Reset), poi reinstalla.",
                ),
            )
        }
    }

    fun map(raw: String?, ui: ErrStrings = ErrStrings.it()): MappedError {
        val e = (raw ?: "").lowercase()
        if (e.isBlank()) {
            return MappedError(Category.NONE, ui.none, emptyList())
        }
        // Errori di firma Tizen.
        if ("failed[-11]" in e || "invalid signature" in e || "signature verification" in e ||
            "signature missing" in e
        ) {
            return MappedError(Category.SIGNATURE_11, ui.sig, ui.sigSteps)
        }
        if ("failed[-12]" in e || "-12]" in e || "failed[118" in e || "author certificate" in e ||
            "duid mismatch" in e || "check certificate" in e
        ) {
            return MappedError(Category.CERT_12, ui.cert, ui.certSteps)
        }
        if ("failed[-14]" in e || "privilege" in e || "partner" in e && "permission" in e) {
            return MappedError(Category.PRIVILEGE_14, ui.priv, ui.privSteps)
        }
        if ("failed[116]" in e || "download failed" in e) {
            return MappedError(Category.PKG_REJECTED_116, ui.dl116, ui.dl116Steps)
        }
        if ("failed[-1]" in e || "failed[-4]" in e || "already exists" in e || "duplicate" in e) {
            return MappedError(Category.CONFLICT_STORAGE, ui.conflict, ui.confSteps)
        }
        if ("not found" in e || "no such file" in e) {
            return MappedError(Category.FILE_MISSING, ui.file, ui.fileSteps)
        }
        if ("connection failed" in e || "cannot connect" in e || "timed out" in e ||
            "errno 111" in e || "refused" in e || "no route" in e ||
            "reset by peer" in e || "connection reset" in e || "broken pipe" in e || "errno 104" in e ||
            "network is unreachable" in e || "nessuna risposta" in e ||
            "canale chiuso" in e || "socket" in e
        ) {
            return MappedError(Category.DEVMODE_OR_AUTH, ui.net, ui.netSteps)
        }
        return MappedError(Category.INSTALL_GENERIC, ui.gen, ui.genSteps)
    }
}
