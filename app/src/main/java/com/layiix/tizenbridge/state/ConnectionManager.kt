package com.layiix.tizenbridge.state

import android.content.Context
import com.layiix.tizenbridge.R
import com.layiix.tizenbridge.log.AppLogStore
import com.layiix.tizenbridge.net.NetworkUtils
import com.layiix.tizenbridge.pkg.PackageValidator
import com.layiix.tizenbridge.prefs.TvProfile
import com.layiix.tizenbridge.samsung.SamsungCert
import com.layiix.tizenbridge.sdb.SdbClient
import com.layiix.tizenbridge.sdb.TvErrorMapper
import com.layiix.tizenbridge.sdb.TvRestApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stato di connessione TV + orchestrazione installazione.
 * Tutte le operazioni di rete girano su Dispatchers.IO, cancellabili.
 * Messaggi utente localizzati (IT/EN/ES/PT/RU) via resources Android.
 */
class ConnectionManager(private val logs: AppLogStore, private val ctx: Context) {

    sealed interface ConnStatus {
        data object Disconnected : ConnStatus
        data object Connecting : ConnStatus
        data class Connected(
            val host: String,
            val port: Int,
            val model: String,
            val tizen: String,
            val duidShort: String,
            val devMode: Boolean,
        ) : ConnStatus
        data class Failed(val reason: String) : ConnStatus
    }

    sealed interface InstallStatus {
        data object Idle : InstallStatus
        data class Running(val phase: String, val sent: Long = 0, val total: Long = 0) : InstallStatus
        data class Success(val appId: String, val detail: String) : InstallStatus
        data class Error(val title: String, val steps: List<String>, val raw: String) : InstallStatus
    }

    data class SelectedPackage(
        val displayName: String,
        val bytes: ByteArray,
        val validation: PackageValidator.ValidationResult,
    )

    private fun s(id: Int, vararg args: Any): String = ctx.getString(id, *args)

    private fun errUi() = TvErrorMapper.ErrStrings(
        sig = s(R.string.err_sig),
        cert = s(R.string.err_cert),
        priv = s(R.string.err_priv),
        conflict = s(R.string.err_conflict),
        file = s(R.string.err_file),
        net = s(R.string.err_net),
        gen = s(R.string.err_gen),
        none = s(R.string.err_none),
        dl116 = s(R.string.err_dl116),
        sigSteps = listOf(s(R.string.esig1), s(R.string.esig2), s(R.string.esig3), s(R.string.esig4)),
        certSteps = listOf(s(R.string.ecert1), s(R.string.ecert2), s(R.string.esig4)),
        privSteps = listOf(s(R.string.epriv1), s(R.string.epriv2), s(R.string.epriv3)),
        confSteps = listOf(s(R.string.econf1), s(R.string.econf2), s(R.string.econf3)),
        fileSteps = listOf(s(R.string.efile1), s(R.string.efile2)),
        netSteps = listOf(s(R.string.enet1), s(R.string.enet2), s(R.string.enet3), s(R.string.enet4)),
        genSteps = listOf(s(R.string.egen1), s(R.string.egen2), s(R.string.egen3)),
        dl116Steps = listOf(s(R.string.edl1), s(R.string.edl2), s(R.string.edl3)),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    private val _status = MutableStateFlow<ConnStatus>(ConnStatus.Disconnected)
    val status: StateFlow<ConnStatus> = _status.asStateFlow()

    private val _install = MutableStateFlow<InstallStatus>(InstallStatus.Idle)
    val install: StateFlow<InstallStatus> = _install.asStateFlow()

    private val _selected = MutableStateFlow<SelectedPackage?>(null)
    val selected: StateFlow<SelectedPackage?> = _selected.asStateFlow()

    var hostInput: String = ""
    var portInput: String = SdbClient.DEFAULT_SDB_PORT.toString()
    var timeoutMs: Int = SdbClient.DEFAULT_TIMEOUT_MS

    fun setSelected(bytes: ByteArray, displayName: String, validation: PackageValidator.ValidationResult) {
        _selected.value = SelectedPackage(displayName, bytes, validation)
        _install.value = InstallStatus.Idle
        logs.info("pkg", "Selezionato ${validation.fileName} (${PackageValidator.formatSize(validation.sizeBytes)})")
        validation.warnings.forEach { logs.warn("pkg", it) }
        validation.errors.forEach { logs.error("pkg", it) }
    }

    fun clearSelected() {
        _selected.value = null
        _install.value = InstallStatus.Idle
    }

    fun cancel() {
        job?.cancel()
        scope.coroutineContext.cancelChildren()
    }

    fun connect() {
        job?.cancel()
        val host = NetworkUtils.sanitizeHost(hostInput)
        if (!NetworkUtils.isValidIpv4(host)) {
            _status.value = ConnStatus.Failed(s(R.string.probe_bad_ip))
            logs.error("net", "Connessione rifiutata: IP non valido.")
            return
        }
        if (!NetworkUtils.isValidPort(portInput)) {
            _status.value = ConnStatus.Failed(s(R.string.probe_bad_port))
            logs.error("net", "Connessione rifiutata: porta non valida.")
            return
        }
        val port = portInput.trim().toInt()
        _status.value = ConnStatus.Connecting
        logs.info("net", "Connessione a $host:$port…")
        job = scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { probe(host, port, timeoutMs) }
                _status.value = result
                when (result) {
                    is ConnStatus.Connected -> {
                        // Auto-completa il profilo: anno dedotto dal Tizen rilevato.
                        try {
                            val pb = TvProfile.brand(ctx)
                            if (pb.isBlank() || (pb == TvProfile.SAMSUNG && TvProfile.year(ctx).isBlank())) {
                                TvProfile.yearForTizen(result.tizen)?.let { TvProfile.save(ctx, TvProfile.SAMSUNG, it) }
                            }
                        } catch (_: Exception) {
                        }
                        logs.info(
                            "net",
                            s(
                                R.string.probe_conn, host, port,
                                result.model.ifBlank { "Samsung TV" },
                                result.tizen.ifBlank { s(R.string.diag_na) },
                            ),
                        )
                    }
                    is ConnStatus.Failed -> logs.error("net", result.reason)
                    else -> Unit
                }
            } catch (e: CancellationException) {
                _status.value = ConnStatus.Disconnected
                logs.warn("net", "Connessione annullata.")
            } catch (e: Exception) {
                val mapped = TvErrorMapper.map(e.message, errUi())
                _status.value = ConnStatus.Failed(mapped.title)
                logs.error("net", mapped.title)
            }
        }
    }

    private fun probe(host: String, port: Int, timeoutMs: Int): ConnStatus {
        // 1) REST Samsung (porta 8001, niente SDB): dice dev-mode e Host IP impostato.
        val rest = TvRestApi.fetch(host)
        val phoneIp = NetworkUtils.localIpAddress()
        if (rest != null && !rest.isDevMode) {
            return ConnStatus.Failed(
                s(R.string.probe_devoff, phoneIp ?: s(R.string.diag_na)),
            ).also { logs.warn("net", "REST TV: developerMode=${rest.developerMode}, developerIP=${rest.developerIp}") }
        }
        if (rest != null && rest.developerIp.isNotBlank() && phoneIp != null && rest.developerIp != phoneIp) {
            return ConnStatus.Failed(s(R.string.probe_hostdiff, rest.developerIp, phoneIp))
        }
        if (!NetworkUtils.isPortOpen(host, port, minOf(timeoutMs, 4000))) {
            return ConnStatus.Failed(s(R.string.probe_unreach, host, port))
        }
        var tizen = ""
        var sdkPath = ""
        // La TV resetta le raffiche: pausa dopo il port-check + retry con backoff.
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return ConnStatus.Disconnected
        }
        var lastErr: Exception? = null
        var caps: Map<String, String>? = null
        for (attempt in 1..3) {
            try {
                caps = SdbClient(host, port, timeoutMs) { m -> logs.info("sdb", m) }.capability()
                lastErr = null
                break
            } catch (e: Exception) {
                lastErr = e
                logs.warn("sdb", "tentativo $attempt/3: ${e.message}")
                try {
                    Thread.sleep(600L * attempt)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return ConnStatus.Disconnected
                }
            }
        }
        val got = caps
        if (got == null) {
            val e = lastErr
            val hint = if (rest != null && rest.isDevMode) {
                s(R.string.probe_hint_dev)
            } else {
                s(R.string.probe_hint_gen)
            }
            return ConnStatus.Failed(s(R.string.probe_reset, hint, e?.message ?: ""))
        }
        tizen = got["platform_version"] ?: got["tizen_version"] ?: ""
        sdkPath = got["sdk_toolpath"] ?: ""
        val model = rest?.model ?: rest?.name ?: ""
        val duid = rest?.duid ?: ""
        val devMode = rest?.isDevMode ?: true // se REST assente, non dedurre il dev-mode
        return ConnStatus.Connected(
            host = host,
            port = port,
            model = model,
            tizen = tizen.ifBlank { s(R.string.diag_na) },
            duidShort = if (duid.length > 8) duid.take(8) + "…" else duid,
            devMode = devMode,
        ).also {
            if (sdkPath.isNotBlank()) {
                // Percorso di staging utile in diagnostica (non mostrato in chiaro all'utente).
            }
        }
    }

    fun disconnect() {
        job?.cancel()
        _status.value = ConnStatus.Disconnected
        logs.info("net", "Disconnesso.")
    }

    /** Legge il DUID dalla TV connessa (per i certificati), null se impossibile. */
    fun fetchDuid(onResult: (String?) -> Unit) {
        val conn = _status.value as? ConnStatus.Connected ?: run {
            onResult(null)
            return
        }
        scope.launch {
            val duid = withContext(Dispatchers.IO) {
                try {
                    val client = SdbClient(conn.host, conn.port, timeoutMs) { m -> logs.info("sdb", m) }
                    SamsungCert.readDuidFromTv { cmd -> runCatching { client.shell(cmd) }.getOrDefault("") }
                } catch (_: Exception) {
                    null
                }
            }
            onResult(duid)
        }
    }

    fun startInstall() {
        val conn = _status.value
        if (conn !is ConnStatus.Connected) {
            _install.value = InstallStatus.Error(
                s(R.string.inst_need_tv),
                listOf(s(R.string.inst_need_tv_step)),
                "",
            )
            return
        }
        val pkg = _selected.value
        if (pkg == null) {
            _install.value = InstallStatus.Error(
                s(R.string.inst_no_pkg),
                listOf(s(R.string.inst_no_pkg_step)),
                "",
            )
            return
        }
        if (!pkg.validation.ok) {
            _install.value = InstallStatus.Error(
                s(R.string.inst_invalid),
                pkg.validation.errors,
                "",
            )
            return
        }
        job?.cancel()
        job = scope.launch {
            try {
                runInstall(conn, pkg)
            } catch (e: CancellationException) {
                _install.value = InstallStatus.Idle
                logs.warn("install", "Installazione annullata.")
            } catch (e: Exception) {
                val mapped = TvErrorMapper.map(e.message, errUi())
                _install.value = InstallStatus.Error(mapped.title, mapped.fixSteps, e.message ?: "")
                logs.error("install", mapped.title)
            }
        }
    }

    private suspend fun runInstall(conn: ConnStatus.Connected, pkg: SelectedPackage) {
        val client = SdbClient(conn.host, conn.port, timeoutMs) { m -> logs.info("sdb", m) }
        val remoteName = pkg.validation.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        // Staging dir da capability quando disponibile, altrimenti default noto.
        val stagingDir = withContext(Dispatchers.IO) {
            try {
                client.capability()["sdk_toolpath"]?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            } ?: "/home/owner/share/tmp/sdk_tools"
        }
        // L'ID app deriva dal manifest del pacchetto (input non fidato):
        // consentiti solo caratteri sicuri per l'uso nei comandi shell SDB.
        fun safeId(raw: String, fallback: String): String {
            val clean = raw.filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }.take(128)
            return clean.ifBlank { fallback }
        }
        val remotePath = "$stagingDir/$remoteName"
        val pkgType = if (pkg.validation.kind == PackageValidator.Kind.TPK) "tpk" else "wgt"
        val appId = safeId(
            pkg.validation.appId ?: pkg.validation.fileName.substringBeforeLast('.'),
            "LayiixApp",
        )
        val packageId = safeId(pkg.validation.packageId ?: appId.substringBefore('.'), "LayiixPkg")

        _install.value = InstallStatus.Running(s(R.string.inst_sending, remoteName), 0, pkg.bytes.size.toLong())
        logs.info("install", "Invio $remoteName alla TV…")
        withContext(Dispatchers.IO) {
            client.pushFile(pkg.bytes, remotePath) { sent, total ->
                _install.value = InstallStatus.Running(s(R.string.inst_sending, remoteName), sent, total)
            }
        }
        logs.info("install", s(R.string.inst_sent))
        // Cooldown: la TV resetta i socket a raffica (come il client di riferimento).
        delay(500)

        _install.value = InstallStatus.Running(s(R.string.inst_running_tv))
        // vd_appinstall vuole: 0 vd_appinstall <AppID> <wgt_path>
        val r1 = withContext(Dispatchers.IO) {
            runCatching { client.shell("0 vd_appinstall $appId $remotePath") }.getOrElse { "" }
        }
        val r1b = if (r1.isBlank()) {
            withContext(Dispatchers.IO) {
                runCatching { client.shell("0 vd_appinstall $packageId $remotePath") }.getOrElse { "" }
            }
        } else {
            r1
        }
        val r2 = withContext(Dispatchers.IO) {
            runCatching { client.shell("0 pkgcmd -i -t $pkgType -p $remotePath") }.getOrElse { "" }
        }
        delay(500)
        val combined = "$r1b $r2"
        logs.info("install", s(R.string.inst_reply, combined.take(500)))

        // Pulizia staging (best-effort).
        withContext(Dispatchers.IO) {
            runCatching { client.shell("0 rmfile $remotePath") }
        }

        val failed = listOf("failed[", "signature error", "invalid signature", "failed to install", "not available")
            .any { combined.lowercase().contains(it) }
        if (failed) {
            val mapped = TvErrorMapper.map(combined, errUi())
            _install.value = InstallStatus.Error(mapped.title, mapped.fixSteps, combined.take(1000))
            logs.error("install", mapped.title)
            return
        }
        // Verifica registro app.
        _install.value = InstallStatus.Running(s(R.string.inst_verifying))
        delay(1200)
        val applist = withContext(Dispatchers.IO) {
            runCatching { client.shell("0 applist") }.getOrElse { "" } + "\n" +
                runCatching { client.shell("0 vd_applist") }.getOrElse { "" }
        }
        val registered = applist.lowercase().contains(appId.lowercase()) ||
            applist.lowercase().contains(packageId.lowercase())
        val cleanOk = !failed && listOf("install completed", "spend time for pkgcmd", "val=0", "passed")
            .any { combined.lowercase().contains(it) }
        if (registered || cleanOk) {
            withContext(Dispatchers.IO) {
                runCatching { client.shell("0 was_execute $appId") }
            }
            _install.value = InstallStatus.Success(appId, s(R.string.inst_registered))
            logs.info("install", s(R.string.inst_success, appId))
        } else {
            // NOTA ONESTA: senza conferma del registro TV non dichiariamo successo.
            val mapped = TvErrorMapper.map("failed[-1] app not found in TV registry after install", errUi())
            _install.value = InstallStatus.Error(
                s(R.string.inst_unconfirmed),
                mapped.fixSteps + listOf(s(R.string.inst_realtv)),
                combined.take(1000),
            )
            logs.error("install", s(R.string.inst_unconfirmed))
        }
    }
}
