package com.layiix.tizenbridge.ui

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layiix.tizenbridge.BuildConfig
import com.layiix.tizenbridge.R
import com.layiix.tizenbridge.catalog.CatalogApps
import com.layiix.tizenbridge.log.AppLogStore
import com.layiix.tizenbridge.net.NetworkUtils
import com.layiix.tizenbridge.pkg.PackageValidator
import com.layiix.tizenbridge.prefs.CertStore
import com.layiix.tizenbridge.prefs.LangPrefs
import com.layiix.tizenbridge.prefs.TvProfile
import com.layiix.tizenbridge.pkg.TizenSigner
import com.layiix.tizenbridge.sdb.SdbClient
import com.layiix.tizenbridge.sdb.TvErrorMapper
import com.layiix.tizenbridge.state.ConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

enum class Dest(val icon: ImageVector) {
    HOME(Icons.Filled.Home),
    TV(Icons.Filled.PlayArrow),
    INSTALL(Icons.Filled.Send),
    CATALOG(Icons.Filled.ShoppingCart),
    MORE(Icons.Filled.MoreVert),
}

@Composable
fun destLabel(d: Dest): String = when (d) {
    Dest.HOME -> stringResource(R.string.nav_home)
    Dest.TV -> stringResource(R.string.nav_tv)
    Dest.INSTALL -> stringResource(R.string.nav_install)
    Dest.CATALOG -> stringResource(R.string.nav_catalog)
    Dest.MORE -> stringResource(R.string.nav_more)
}

enum class MoreDest {
    MENU,
    DIAG,
    SETTINGS,
    INFO,
    LOGS,
}

@Composable
fun moreLabel(m: MoreDest): String = when (m) {
    MoreDest.MENU -> stringResource(R.string.more_menu)
    MoreDest.DIAG -> stringResource(R.string.more_diag)
    MoreDest.SETTINGS -> stringResource(R.string.more_settings)
    MoreDest.INFO -> stringResource(R.string.more_info)
    MoreDest.LOGS -> stringResource(R.string.more_logs)
}

/** Stringhe localizzate per il mapper errori (contesto Compose). */
@Composable
fun errUi(): TvErrorMapper.ErrStrings = TvErrorMapper.ErrStrings(
    sig = stringResource(R.string.err_sig),
    cert = stringResource(R.string.err_cert),
    priv = stringResource(R.string.err_priv),
    conflict = stringResource(R.string.err_conflict),
    file = stringResource(R.string.err_file),
    net = stringResource(R.string.err_net),
    gen = stringResource(R.string.err_gen),
    none = stringResource(R.string.err_none),
    sigSteps = listOf(stringResource(R.string.esig1), stringResource(R.string.esig2), stringResource(R.string.esig3), stringResource(R.string.esig4)),
    certSteps = listOf(stringResource(R.string.ecert1), stringResource(R.string.ecert2), stringResource(R.string.esig4)),
    privSteps = listOf(stringResource(R.string.epriv1), stringResource(R.string.epriv2), stringResource(R.string.epriv3)),
    confSteps = listOf(stringResource(R.string.econf1), stringResource(R.string.econf2), stringResource(R.string.econf3)),
    fileSteps = listOf(stringResource(R.string.efile1), stringResource(R.string.efile2)),
    netSteps = listOf(stringResource(R.string.enet1), stringResource(R.string.enet2), stringResource(R.string.enet3), stringResource(R.string.enet4)),
    genSteps = listOf(stringResource(R.string.egen1), stringResource(R.string.egen2), stringResource(R.string.egen3)),
    dl116 = stringResource(R.string.err_dl116),
    dl116Steps = listOf(stringResource(R.string.edl1), stringResource(R.string.edl2), stringResource(R.string.edl3)),
)

/** Stringhe localizzate per il validatore pacchetti (contesto Compose). */
@Composable
fun pkgUi(): PackageValidator.PkgStrings = PackageValidator.PkgStrings(
    apk = stringResource(R.string.pkg_apk),
    ext = stringResource(R.string.pkg_ext),
    empty = stringResource(R.string.pkg_empty),
    big = stringResource(R.string.pkg_big),
    nozip = stringResource(R.string.pkg_nozip),
    many = stringResource(R.string.pkg_many),
    noconfig = stringResource(R.string.pkg_noconfig),
    nomanifest = stringResource(R.string.pkg_nomanifest),
    nosignWarn = stringResource(R.string.pkg_nosign_warn),
    noidWarn = stringResource(R.string.pkg_noid_warn),
)

/** Badge mono stile sito (es. ONLINE / OFFLINE). */
@Composable
fun Pill(text: String, color: Color) {
    Box(
        modifier = Modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.45f),
            shape = RoundedCornerShape(3.dp),
        ).padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            ),
        )
    }
}

/** Footer brand: apre il portfolio layiix.com. */
@Composable
fun DevFooter() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "LΛYIIX // TIZEN BRIDGE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                color = LayiixMuted,
            ),
        )
        TextButton(onClick = { uriHandler.openUri("https://layiix.com") }) {
            Text(
                "${stringResource(R.string.dev_by)} · ${stringResource(R.string.dev_open)} ↗",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = LayiixPrimary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayiixShell(manager: ConnectionManager, logs: AppLogStore) {
    var dest by remember { mutableStateOf(Dest.HOME) }
    var more by remember { mutableStateOf(MoreDest.MENU) }
    val shellContext = LocalContext.current
    var showWizard by remember { mutableStateOf(!TvProfile.isDone(shellContext)) }

    BackHandler(enabled = (dest == Dest.MORE && more != MoreDest.MENU)) {
        more = MoreDest.MENU
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (dest) {
                            Dest.MORE -> moreLabel(more)
                            else -> stringResource(R.string.brand_title)
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LayiixSurface),
            )
        },
        bottomBar = {
            if (!showWizard) {
                NavigationBar(containerColor = LayiixSurface) {
                    Dest.values().forEach { d ->
                        NavigationBarItem(
                            selected = dest == d,
                            onClick = { dest = d },
                            icon = { Icon(d.icon, contentDescription = destLabel(d)) },
                            label = { Text(destLabel(d)) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (showWizard) {
                TvWizardScreen(onDone = { showWizard = false })
            } else {
                when (dest) {
                Dest.HOME -> HomeScreen(manager, onGo = { dest = it }, onWizard = { showWizard = true })
                Dest.TV -> TvScreen(manager)
                Dest.INSTALL -> InstallScreen(manager, onChangeTv = { showWizard = true })
                Dest.CATALOG -> CatalogScreen(manager, onInstall = { dest = Dest.INSTALL })
                Dest.MORE -> MoreScreen(manager, logs, more, onMore = { more = it }, onRetakeTv = { showWizard = true })
                }
            }
        }
    }
}

// ---------- HOME ----------

@Composable
fun HomeScreen(manager: ConnectionManager, onGo: (Dest) -> Unit, onWizard: () -> Unit = {}) {
    val status by manager.status.collectAsState()
    val selected by manager.selected.collectAsState()
    val homeContext = LocalContext.current
    val scroll = rememberScrollState()

    Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(
            painter = painterResource(R.drawable.hero_bridge),
            contentDescription = stringResource(R.string.brand_title),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.656f)
                .clip(RoundedCornerShape(16.dp)),
        )
        TvProfileCard(homeContext, onWizard)
        Kicker("00 // ${stringResource(R.string.k_status)}")
        StatusCard(status)
        Kicker("01 // ${stringResource(R.string.k_actions)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeButton(stringResource(R.string.home_go_tv), Icons.Filled.PlayArrow) { onGo(Dest.TV) }
                HomeButton(stringResource(R.string.home_go_install), Icons.Filled.Send) { onGo(Dest.INSTALL) }
                HomeButton(stringResource(R.string.home_go_logs), Icons.Filled.List) { onGo(Dest.MORE) }
                HomeButton(stringResource(R.string.home_go_settings), Icons.Filled.Settings) { onGo(Dest.MORE) }
            }
        }
        if (selected != null) {
            Kicker("02 // ${stringResource(R.string.k_pkg)}")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
                border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.home_pkg_ready), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(selected!!.validation.fileName)
                    Text(
                        stringResource(
                            R.string.install_meta,
                            PackageValidator.formatSize(selected!!.validation.sizeBytes),
                            selected!!.validation.appId ?: stringResource(R.string.install_noid),
                            if (selected!!.validation.signed) stringResource(R.string.install_signed_yes)
                            else stringResource(R.string.install_signed_no),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Text(stringResource(R.string.home_note), style = MaterialTheme.typography.bodySmall)
        DevFooter()
    }
}

/** Riepilogo profilo TV: marca, Tizen stimato o invito a configurare. */
@Composable
fun tvSummaryText(context: Context): String {
    if (!TvProfile.isDone(context)) return stringResource(R.string.home_tv_none)
    val brand = TvProfile.brand(context)
    val year = TvProfile.year(context)
    val label = when (brand) {
        TvProfile.SAMSUNG -> "Samsung"
        TvProfile.LG -> "LG"
        TvProfile.ANDROID -> "Android TV"
        TvProfile.OTHER -> stringResource(R.string.wiz_other)
        else -> return stringResource(R.string.home_tv_none)
    }
    if (TvProfile.isBlocked(brand, year)) return stringResource(R.string.home_tv_nok, label)
    val tizen = TvProfile.tizenForYear(year)
    if (tizen != null) return stringResource(R.string.home_tv_ok, label, tizen)
    return stringResource(R.string.home_tv_nover, label)
}

@Composable
fun TvProfileCard(context: Context, onWizard: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
        border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = LayiixPrimary)
            Text(
                tvSummaryText(context),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onWizard) {
                Text(stringResource(R.string.set_tv_change).uppercase())
            }
        }
    }
}

@Composable
fun HomeButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(label.uppercase())
    }
}

@Composable
fun StatusCard(status: ConnectionManager.ConnStatus) {
    val (text, color) = when (status) {
        is ConnectionManager.ConnStatus.Disconnected -> stringResource(R.string.status_disconnected) to LayiixMuted
        is ConnectionManager.ConnStatus.Connecting -> stringResource(R.string.status_connecting) to LayiixWarn
        is ConnectionManager.ConnStatus.Connected ->
            stringResource(R.string.status_connected, status.host, status.port) to LayiixOk
        is ConnectionManager.ConnStatus.Failed -> stringResource(R.string.status_failed, status.reason) to LayiixError
    }
    val pill = when (status) {
        is ConnectionManager.ConnStatus.Connected -> "ONLINE" to LayiixOk
        is ConnectionManager.ConnStatus.Connecting -> "···" to LayiixWarn
        is ConnectionManager.ConnStatus.Failed -> "ERROR" to LayiixError
        else -> "OFFLINE" to LayiixMuted
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
        border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = when (status) {
                is ConnectionManager.ConnStatus.Connected -> Icons.Filled.CheckCircle
                is ConnectionManager.ConnStatus.Failed -> Icons.Filled.Warning
                is ConnectionManager.ConnStatus.Connecting -> Icons.Filled.Refresh
                else -> Icons.Filled.PlayArrow
            }
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.status_tv_label), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(2.dp))
                Text(text, color = color)
                if (status is ConnectionManager.ConnStatus.Connected) {
                    Text(
                        stringResource(
                            R.string.status_model,
                            status.model.ifBlank { "Samsung TV" },
                            status.tizen.ifBlank { stringResource(R.string.diag_na) },
                        ) + if (status.duidShort.isNotBlank()) {
                            stringResource(R.string.status_duid, status.duidShort)
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Pill(pill.first, pill.second)
        }
    }
}

// ---------- TV / CONNESSIONE ----------

@Composable
fun TvScreen(manager: ConnectionManager) {
    val status by manager.status.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ui = errUi()
    var host by remember { mutableStateOf(manager.hostInput) }
    var port by remember { mutableStateOf(manager.portInput) }
    var scanning by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanMsg by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Kicker("01 // ${stringResource(R.string.k_conn)}")
        StatusCard(status)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.tv_manual), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it; manager.hostInput = it },
                    label = { Text(stringResource(R.string.tv_ip_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = host.isNotBlank() && !NetworkUtils.isValidIpv4(host),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }.take(5); manager.portInput = port },
                    label = { Text(stringResource(R.string.tv_port_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = port.isNotBlank() && !NetworkUtils.isValidPort(port),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        manager.hostInput = host
                        manager.portInput = if (port.isBlank()) "26101" else port
                        manager.connect()
                    }) { Text(stringResource(R.string.tv_connect).uppercase()) }
                    OutlinedButton(onClick = { manager.disconnect() }) { Text(stringResource(R.string.tv_disconnect).uppercase()) }
                }
                if (status is ConnectionManager.ConnStatus.Failed) {
                    val mapped = TvErrorMapper.map((status as ConnectionManager.ConnStatus.Failed).reason, ui)
                    ErrorBox(mapped.title, mapped.fixSteps)
                }
            }
        }
        Kicker("02 // ${stringResource(R.string.k_search)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.tv_scan_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.tv_scan_desc), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            scanning = true
                            scanMsg = ""
                            found = emptyList()
                            try {
                                val local = withContext(Dispatchers.IO) { NetworkUtils.localIpAddress() }
                                val base = local?.let { NetworkUtils.subnetBaseOf(it) }
                                if (base == null) {
                                    scanMsg = context.getString(R.string.tv_no_ip)
                                } else {
                                    val res = NetworkUtils.scanSubnet(base) { }
                                    found = res
                                    scanMsg = if (res.isEmpty()) {
                                        context.getString(R.string.tv_none, base)
                                    } else {
                                        context.getString(R.string.tv_found, res.size)
                                    }
                                }
                            } finally {
                                scanning = false
                            }
                        }
                    }, enabled = !scanning) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(if (scanning) stringResource(R.string.tv_scanning) else stringResource(R.string.tv_scan_btn))
                    }
                }
                if (scanning) CircularProgressIndicator()
                if (scanMsg.isNotBlank()) Text(scanMsg, style = MaterialTheme.typography.bodySmall)
                found.forEach { ip ->
                    TextButton(onClick = { host = ip; manager.hostInput = ip }) {
                        Text(stringResource(R.string.tv_use, ip))
                    }
                }
            }
        }
        Kicker("03 // ${stringResource(R.string.k_guide)}")
        GuideCard()
    }
}

@Composable
fun GuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
        border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleMedium)
            listOf(
                stringResource(R.string.guide_s1),
                stringResource(R.string.guide_s2),
                stringResource(R.string.guide_s3),
                stringResource(R.string.guide_s4),
                stringResource(R.string.guide_s5),
                stringResource(R.string.guide_s6),
                stringResource(R.string.guide_s7),
            ).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(
                stringResource(R.string.guide_sign),
                style = MaterialTheme.typography.bodySmall,
                color = LayiixWarn,
            )
        }
    }
}

@Composable
fun ErrorBox(title: String, steps: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1216)),
        border = androidx.compose.foundation.BorderStroke(1.dp, LayiixError.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = LayiixError)
                Spacer(Modifier.width(8.dp))
                Text(title, color = LayiixError)
            }
            steps.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

// ---------- INSTALLAZIONE ----------

const val MAX_READ_BYTES = 250L * 1024L * 1024L // limite lettura in memoria

@Composable
fun InstallScreen(manager: ConnectionManager, onChangeTv: () -> Unit = {}) {
    val context = LocalContext.current
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val status by manager.status.collectAsState()
    val selected by manager.selected.collectAsState()
    val install by manager.install.collectAsState()
    val scope = rememberCoroutineScope()
    val msgs = pkgUi()
    var reading by remember { mutableStateOf(false) }
    var readError by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    var signing by remember { mutableStateOf(false) }
    var signError by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        reading = true
        readError = ""
        scope.launch {
            try {
                val pkg = withContext(Dispatchers.IO) { readPackage(context, uri, msgs) }
                manager.setSelected(pkg.first, pkg.second, pkg.third)
            } catch (e: Exception) {
                readError = e.message ?: context.getString(R.string.read_open)
            } finally {
                reading = false
            }
        }
    }

    val tvBrand = TvProfile.brand(context)
    val tvYear = TvProfile.year(context)
    if (TvProfile.isDone(context) && TvProfile.isBlocked(tvBrand, tvYear)) {
        val blockedDesc = when {
            tvBrand == TvProfile.LG -> stringResource(R.string.wiz_lg_desc)
            tvBrand == TvProfile.ANDROID -> stringResource(R.string.wiz_android_desc)
            tvBrand == TvProfile.SAMSUNG -> stringResource(R.string.wiz_legacy_desc)
            else -> stringResource(R.string.wiz_other_desc)
        }
        Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Kicker("01 // ${stringResource(R.string.k_tv)}")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1216)),
                border = androidx.compose.foundation.BorderStroke(1.dp, LayiixError.copy(alpha = 0.4f)),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.inst_blocked_title), style = MaterialTheme.typography.titleMedium, color = LayiixError)
                    Text(tvSummaryText(context), style = MaterialTheme.typography.bodySmall)
                    Text(blockedDesc, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onChangeTv) {
                        Text(stringResource(R.string.set_tv_change).uppercase())
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        return
    }

    Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Kicker("01 // ${stringResource(R.string.k_state)}")
        StatusCard(status)
        Kicker("02 // ${stringResource(R.string.k_pkg)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.install_pkg_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.install_pkg_desc), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !reading) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(if (reading) stringResource(R.string.install_reading) else stringResource(R.string.install_pick))
                }
                if (reading) CircularProgressIndicator()
                if (readError.isNotBlank()) Text(readError, color = LayiixError)
                selected?.let { sel ->
                    Text(sel.validation.fileName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.install_meta,
                            PackageValidator.formatSize(sel.validation.sizeBytes),
                            sel.validation.appId ?: stringResource(R.string.install_noid),
                            if (sel.validation.signed) stringResource(R.string.install_signed_yes)
                            else stringResource(R.string.install_signed_no),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    sel.validation.errors.forEach { Text("• $it", color = LayiixError) }
                    sel.validation.warnings.forEach { Text("• $it", color = LayiixWarn) }
                    sel.validation.requiredVersion?.let { req ->
                        Text(
                            stringResource(R.string.install_req, req),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val tvVer = (status as? ConnectionManager.ConnStatus.Connected)?.tizen
                        if (PackageValidator.isCompatible(req, tvVer) == false) {
                            Text(
                                stringResource(R.string.install_incompat, req, tvVer.orEmpty()),
                                color = LayiixError,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { manager.clearSelected() }) { Text(stringResource(R.string.install_remove).uppercase()) }
                    }
                    if (!sel.validation.signed && CertStore.hasEffective(context) && !signing) {
                        Button(onClick = {
                            signing = true
                            signError = ""
                            scope.launch {
                                try {
                                    val signed = withContext(Dispatchers.IO) {
                                        val (ab, ap) = CertStore.effective(context, CertStore.AUTHOR)
                                            ?: error("author.p12")
                                        val (db, dp) = CertStore.effective(context, CertStore.DIST)
                                            ?: error("distributor.p12")
                                        TizenSigner.signWgt(
                                            sel.bytes,
                                            TizenSigner.loadP12(ab, ap),
                                            TizenSigner.loadP12(db, dp),
                                        )
                                    }
                                    manager.setSelected(
                                        signed,
                                        sel.validation.fileName,
                                        validateBytes(signed, sel.validation.fileName, msgs),
                                    )
                                } catch (e: Exception) {
                                    signError = context.getString(R.string.install_sign_fail, e.message ?: "?")
                                } finally {
                                    signing = false
                                }
                            }
                        }) { Text(stringResource(R.string.install_sign_go).uppercase()) }
                    }
                    if (signing) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.install_signing), style = MaterialTheme.typography.bodySmall)
                    }
                    if (signError.isNotBlank()) Text(signError, color = LayiixError)
                }
            }
        }
        Kicker("03 // ${stringResource(R.string.k_install)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.install_title), style = MaterialTheme.typography.titleMedium)
                when (val st = install) {
                    is ConnectionManager.InstallStatus.Idle -> Text(
                        stringResource(R.string.install_idle),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    is ConnectionManager.InstallStatus.Running -> {
                        Text(st.phase)
                        if (st.total > 0) {
                            LinearProgressIndicator(
                                progress = { (st.sent.toFloat() / st.total.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "${PackageValidator.formatSize(st.sent)} / ${PackageValidator.formatSize(st.total)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                stringResource(R.string.install_indeterminate),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedButton(onClick = { manager.cancel() }) { Text(stringResource(R.string.install_cancel).uppercase()) }
                    }
                    is ConnectionManager.InstallStatus.Success -> {
                        Pill("OK", LayiixOk)
                        Text(stringResource(R.string.inst_success, st.appId), color = LayiixOk)
                        Text(st.detail, style = MaterialTheme.typography.bodySmall)
                    }
                    is ConnectionManager.InstallStatus.Error -> {
                        ErrorBox(st.title, st.steps)
                        if (st.raw.isNotBlank()) {
                            SelectionContainer {
                                Text(stringResource(R.string.install_rawdetail, st.raw.take(500)), style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(st.raw)) }) {
                                Text(stringResource(R.string.install_copy_detail).uppercase())
                            }
                        }
                    }
                }
                Button(
                    onClick = { confirm = true },
                    enabled = install !is ConnectionManager.InstallStatus.Running,
                ) { Text(stringResource(R.string.install_yes).uppercase()) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirm && selected != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.install_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.install_confirm_text,
                        selected!!.validation.fileName,
                        PackageValidator.formatSize(selected!!.validation.sizeBytes),
                        (status as? ConnectionManager.ConnStatus.Connected)?.host
                            ?: stringResource(R.string.inst_need_tv),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    manager.startInstall()
                }) { Text(stringResource(R.string.install_yes).uppercase()) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.install_no).uppercase()) } },
        )
    }
}

/** Legge un pacchetto via SAF con limiti anti-OOM e path traversal safe. */
fun readPackage(
    context: Context,
    uri: Uri,
    msgs: PackageValidator.PkgStrings,
): Triple<ByteArray, String, PackageValidator.ValidationResult> {
    val cr = context.contentResolver
    var displayName = "pacchetto"
    var size: Long = -1
    cr.query(uri, null, null, null, null)?.use { c ->
        val nameIdx = c.getColumnIndex("_display_name")
        val sizeIdx = c.getColumnIndex("_size")
        if (c.moveToFirst()) {
            if (nameIdx >= 0) c.getString(nameIdx)?.let { displayName = it }
            if (sizeIdx >= 0) size = c.getLong(sizeIdx)
        }
    }
    displayName = displayName.substringAfterLast('/').substringAfterLast('\\').take(128)
    if (displayName.isBlank()) displayName = "pacchetto"
    if (size > MAX_READ_BYTES) {
        throw IllegalArgumentException(
            context.getString(
                R.string.read_big,
                PackageValidator.formatSize(size),
                PackageValidator.formatSize(MAX_READ_BYTES),
            ),
        )
    }
    val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalArgumentException(context.getString(R.string.read_open))
    if (bytes.size.toLong() > MAX_READ_BYTES) {
        throw IllegalArgumentException(context.getString(R.string.read_big2))
    }
    return Triple(bytes, displayName, validateBytes(bytes, displayName, msgs))
}

/** Legge un file SAF con tetto anti-OOM. */
fun readCapped(cr: ContentResolver, uri: Uri, max: Long): ByteArray {
    val out = ByteArrayOutputStream()
    cr.openInputStream(uri)?.use { ins ->
        val buf = ByteArray(32768)
        var total = 0L
        while (true) {
            val n = ins.read(buf)
            if (n < 0) break
            total += n
            if (total > max) throw IllegalArgumentException("file troppo grande")
            out.write(buf, 0, n)
        }
    } ?: throw IllegalArgumentException("impossibile aprire il file")
    return out.toByteArray()
}

/** Valida byte grezzi di un pacchetto (usato dal selettore file e dal catalogo). */
fun validateBytes(
    bytes: ByteArray,
    displayName: String,
    msgs: PackageValidator.PkgStrings,
): PackageValidator.ValidationResult {
    val magic = if (bytes.size >= 2) bytes.copyOfRange(0, 2) else null
    val entries = mutableListOf<String>()
    var manifest: String? = null
    try {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zin ->
            var e = zin.nextEntry
            var count = 0
            while (e != null && count < PackageValidator.MAX_ENTRIES + 1) {
                val name = e.name.take(256)
                // Rifiuta entry con path traversal: non vengono mai scritte su disco, solo elencate.
                if (!name.contains("..")) {
                    entries.add(name)
                    if ((name == "config.xml" || name == "tizen-manifest.xml") && manifest == null) {
                        val buf = zin.readBytes()
                        if (buf.size <= 1024 * 1024) {
                            manifest = String(buf, Charsets.UTF_8)
                        }
                    }
                }
                count++
                zin.closeEntry()
                e = zin.nextEntry
            }
        }
    } catch (_: Exception) {
        // entries resta parziale: la validazione segnalerà l'archivio non valido.
    }
    val result = PackageValidator.validate(displayName, bytes.size.toLong(), magic, entries, manifest, msgs)
    return result
}

/** Nome categoria localizzato (chiavi interne: Framework/Streaming/Gaming/Utility/IPTV). */
@Composable
fun catLabel(cat: String): String = when (cat) {
    "Framework" -> stringResource(R.string.cat_n_framework)
    "Streaming" -> stringResource(R.string.cat_n_streaming)
    "Gaming" -> stringResource(R.string.cat_n_gaming)
    "Utility" -> stringResource(R.string.cat_n_utility)
    "IPTV" -> stringResource(R.string.cat_n_iptv)
    else -> cat.uppercase()
}

// ---------- CATALOGO ----------

private data class DlState(val progress: Float?, val error: String?)

/** Scarica un file con limite anti-OOM e progressivo. */
@Throws(IOException::class)
fun downloadBytes(url: String, maxBytes: Long, tooBigMsg: String, onProgress: (Long, Long) -> Unit): ByteArray {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000
        readTimeout = 30000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Layiix-TizenBridge/1.0")
    }
    try {
        if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
        val total = conn.contentLengthLong
        if (total > maxBytes) throw IOException(tooBigMsg)
        val out = ByteArrayOutputStream()
        conn.inputStream.use { input ->
            val buf = ByteArray(32768)
            var done = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                done += n
                if (done > maxBytes) throw IOException(tooBigMsg)
                onProgress(done, total)
            }
        }
        return out.toByteArray()
    } finally {
        conn.disconnect()
    }
}

@Composable
fun CatalogScreen(manager: ConnectionManager, onInstall: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val msgs = pkgUi()
    val states = remember { mutableStateMapOf<String, DlState>() }
    val scroll = rememberScrollState()

    fun download(app: com.layiix.tizenbridge.catalog.CatalogApp) {
        scope.launch {
            states[app.id] = DlState(0f, null)
            try {
                val bytes = withContext(Dispatchers.IO) {
                    downloadBytes(app.url, MAX_READ_BYTES, context.getString(R.string.read_big2)) { done, total ->
                        if (total > 0) states[app.id] = DlState(done.toFloat() / total, null)
                    }
                }
                var data = bytes
                var result = validateBytes(data, app.fileName, msgs)
                if (!result.signed) {
                    // Firma automatica con i certificati integrati (o quelli importati).
                    states[app.id] = DlState(0f, null)
                    data = withContext(Dispatchers.IO) {
                        val (ab, ap) = CertStore.effective(context, CertStore.AUTHOR)
                            ?: error("author.p12")
                        val (db, dp) = CertStore.effective(context, CertStore.DIST)
                            ?: error("distributor.p12")
                        TizenSigner.signWgt(
                            data,
                            TizenSigner.loadP12(ab, ap),
                            TizenSigner.loadP12(db, dp),
                        )
                    }
                    result = validateBytes(data, app.fileName, msgs)
                }
                manager.setSelected(data, app.fileName, result)
                states.remove(app.id)
                onInstall()
            } catch (e: Exception) {
                states[app.id] = DlState(null, context.getString(R.string.cat_dl_fail, e.message ?: "?"))
            }
        }
    }

    Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Kicker("01 // ${stringResource(R.string.k_catalog)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.cat_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.cat_desc), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.cat_autosign),
                    style = MaterialTheme.typography.bodySmall,
                    color = LayiixMuted,
                )
                OutlinedButton(onClick = { uriHandler.openUri(com.layiix.tizenbridge.catalog.CatalogApps.ARCHIVE_URL) }) {
                    Text(stringResource(R.string.cat_archive).uppercase())
                }
            }
        }
        for ((i, cat) in com.layiix.tizenbridge.catalog.CatalogApps.categories.withIndex()) {
            Kicker("${(i + 2).toString().padStart(2, '0')} // ${catLabel(cat)}")
            for (app in com.layiix.tizenbridge.catalog.CatalogApps.all.filter { it.category == cat }) {
                val st = states[app.id]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(app.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(app.descRes), style = MaterialTheme.typography.bodySmall)
                        Pill(stringResource(R.string.cat_min, app.minTizen), LayiixMuted)
                        when {
                            st == null -> Button(onClick = { download(app) }) {
                                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(stringResource(R.string.cat_download).uppercase())
                            }
                            st.error != null -> {
                                Text(st.error, color = LayiixError, style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = { download(app) }) {
                                    Text(stringResource(R.string.cat_download).uppercase())
                                }
                            }
                            else -> {
                                LinearProgressIndicator(
                                    progress = { (st.progress ?: 0f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    "${((st.progress ?: 0f) * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        DevFooter()
    }
}

// ---------- LOG ----------

@Composable
fun LogsScreen(logs: AppLogStore) {
    val entries by logs.entries.collectAsState()
    val clipboard: ClipboardManager = LocalClipboardManager.current
    Kicker("01 // ${stringResource(R.string.k_log)}")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(logs.copyText())) }) {
                Text(stringResource(R.string.logs_copy).uppercase())
            }
            OutlinedButton(onClick = { logs.clear() }) { Text(stringResource(R.string.logs_clear).uppercase()) }
            Text(
                stringResource(R.string.logs_count, entries.size),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
            items(entries.reversed()) { e ->
                val color = when (e.level) {
                    AppLogStore.Level.ERROR -> LayiixError
                    AppLogStore.Level.WARN -> LayiixWarn
                    AppLogStore.Level.DEBUG -> Color.Gray
                    AppLogStore.Level.INFO -> Color.Unspecified
                }
                Text(e.formatted(), style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}

// ---------- ALTRO: menu / diagnostica / impostazioni / info ----------

@Composable
fun MoreScreen(
    manager: ConnectionManager,
    logs: AppLogStore,
    more: MoreDest,
    onMore: (MoreDest) -> Unit,
    onRetakeTv: () -> Unit = {},
) {
    when (more) {
        MoreDest.MENU -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Kicker("00 // ${stringResource(R.string.k_menu)}")
            HomeButton(stringResource(R.string.more_diag), Icons.Filled.Search) { onMore(MoreDest.DIAG) }
            HomeButton(stringResource(R.string.more_settings), Icons.Filled.Settings) { onMore(MoreDest.SETTINGS) }
            HomeButton(stringResource(R.string.more_info), Icons.Filled.Info) { onMore(MoreDest.INFO) }
            HomeButton(stringResource(R.string.more_logs), Icons.Filled.List) { onMore(MoreDest.LOGS) }
            DevFooter()
        }
        MoreDest.DIAG -> DiagScreen(manager, logs)
        MoreDest.SETTINGS -> SettingsScreen(manager, onRetakeTv = onRetakeTv)
        MoreDest.INFO -> InfoScreen()
        MoreDest.LOGS -> LogsScreen(logs)
    }
}

@Composable
fun DiagScreen(manager: ConnectionManager, logs: AppLogStore) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var localIp by remember { mutableStateOf<String?>(null) }
    var wifi by remember { mutableStateOf("") }
    var reach by remember { mutableStateOf("") }
    var caps by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val status by manager.status.collectAsState()
    val scroll = rememberScrollState()

    fun refresh() {
        busy = true
        reach = ""
        caps = ""
        scope.launch {
            val ip = withContext(Dispatchers.IO) { NetworkUtils.localIpAddress() }
            localIp = ip
            wifi = readWifiInfo(context)
            val conn = status
            if (conn is ConnectionManager.ConnStatus.Connected) {
                reach = if (withContext(Dispatchers.IO) {
                        NetworkUtils.isPortOpen(conn.host, conn.port, 3000)
                    }
                ) {
                    context.getString(R.string.diag_reach_ok, conn.host, conn.port)
                } else {
                    context.getString(R.string.diag_reach_ko, conn.host, conn.port)
                }
                caps = withContext(Dispatchers.IO) {
                    try {
                        val m = com.layiix.tizenbridge.sdb.SdbClient(conn.host, conn.port, 5000).capability()
                        if (m.isEmpty()) context.getString(R.string.diag_caps_empty)
                        else m.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                    } catch (e: Exception) {
                        context.getString(R.string.diag_caps_fail, e.message ?: "")
                    }
                }
            } else {
                reach = context.getString(R.string.diag_reach_hint)
            }
            logs.info("diag", "Diagnostica aggiornata (IP locale ${ip ?: "n/d"}).")
            busy = false
        }
    }

    Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Kicker("01 // ${stringResource(R.string.k_diag)}")
        Button(onClick = { refresh() }, enabled = !busy) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(if (busy) stringResource(R.string.diag_busy) else stringResource(R.string.diag_run))
        }
        DiagRow(stringResource(R.string.diag_ip), localIp ?: stringResource(R.string.diag_na))
        DiagRow(stringResource(R.string.diag_wifi), wifi.ifBlank { stringResource(R.string.diag_na) })
        DiagRow(stringResource(R.string.diag_reach), reach.ifBlank { stringResource(R.string.diag_na) })
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.diag_caps), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text(caps.ifBlank { stringResource(R.string.diag_na) }, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DiagRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
        border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                ),
            )
            Spacer(Modifier.height(2.dp))
            SelectionContainer { Text(value) }
        }
    }
}

fun readWifiInfo(context: Context): String {
    return try {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        if (!wifi.isWifiEnabled) return context.getString(R.string.diag_wifi_off)
        val info = wifi.connectionInfo
        val ip = info?.ipAddress?.let {
            "${it and 0xFF}.${(it shr 8) and 0xFF}.${(it shr 16) and 0xFF}.${(it shr 24) and 0xFF}"
        } ?: "n/d"
        context.getString(R.string.diag_wifi_on, ip)
    } catch (_: Exception) {
        context.getString(R.string.diag_wifi_err)
    }
}

@Composable
fun SettingsScreen(manager: ConnectionManager, onRetakeTv: () -> Unit = {}) {
    var timeout by remember { mutableStateOf(manager.timeoutMs.toString()) }
    var port by remember { mutableStateOf(manager.portInput) }
    var saved by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lang by remember { mutableStateOf(LangPrefs.get(context)) }
    var certTick by remember { mutableStateOf(0) }
    var authorBytes by remember { mutableStateOf<ByteArray?>(null) }
    var authorPwd by remember { mutableStateOf("") }
    var distBytes by remember { mutableStateOf<ByteArray?>(null) }
    var distPwd by remember { mutableStateOf("") }
    var certMsg by remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    val hasCerts = remember(certTick) { CertStore.hasAll(context) }

    val authorPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                authorBytes = withContext(Dispatchers.IO) { readCapped(context.contentResolver, uri, 5L * 1024 * 1024) }
                certMsg = ""
            } catch (e: Exception) {
                certMsg = context.getString(R.string.set_sign_err, e.message ?: "?")
            }
        }
    }
    val distPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                distBytes = withContext(Dispatchers.IO) { readCapped(context.contentResolver, uri, 5L * 1024 * 1024) }
                certMsg = ""
            } catch (e: Exception) {
                certMsg = context.getString(R.string.set_sign_err, e.message ?: "?")
            }
        }
    }
    Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Kicker("01 // ${stringResource(R.string.k_net)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.set_net), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.set_port)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = timeout,
                    onValueChange = { timeout = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.set_timeout)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    val t = timeout.toIntOrNull()
                    if (t != null && t in 2000..30000) manager.timeoutMs = t
                    if (port.isBlank() || NetworkUtils.isValidPort(port)) {
                        manager.portInput = port.ifBlank { SdbClient.DEFAULT_SDB_PORT.toString() }
                    }
                    saved = "1"
                }) { Text(stringResource(R.string.set_save).uppercase()) }
                if (saved.isNotBlank()) Text(stringResource(R.string.set_saved), color = LayiixOk)
            }
        }
        Kicker("02 // ${stringResource(R.string.k_tv)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.set_tv), style = MaterialTheme.typography.titleMedium)
                Text(tvSummaryText(context), style = MaterialTheme.typography.bodySmall)
                Button(onClick = onRetakeTv) {
                    Text(stringResource(R.string.set_tv_change).uppercase())
                }
            }
        }
        Kicker("03 // ${stringResource(R.string.k_lang)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.set_lang), style = MaterialTheme.typography.titleMedium)
                LangOption(LangPrefs.SYSTEM, stringResource(R.string.set_lang_system), lang) {
                    pickLang(context, it) { lang = it }
                }
                LangOption("it", "Italiano", lang) { pickLang(context, it) { lang = it } }
                LangOption("en", "English", lang) { pickLang(context, it) { lang = it } }
                LangOption("es", "Español", lang) { pickLang(context, it) { lang = it } }
                LangOption("pt", "Português", lang) { pickLang(context, it) { lang = it } }
                LangOption("ru", "Русский", lang) { pickLang(context, it) { lang = it } }
            }
        }
        Kicker("04 // ${stringResource(R.string.k_sign)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.set_sign), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.set_sign_desc), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.set_sign_how),
                    style = MaterialTheme.typography.bodySmall,
                    color = LayiixMuted,
                )
                if (hasCerts) {
                    Text(
                        "author.p12: ${CertStore.info(context, CertStore.AUTHOR) ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "distributor.p12: ${CertStore.info(context, CertStore.DIST) ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = {
                        CertStore.clear(context)
                        certTick++
                    }) { Text(stringResource(R.string.set_sign_remove).uppercase()) }
                } else {
                    val bundledOk = remember(certTick) {
                        CertStore.bundled(context, CertStore.AUTHOR) != null &&
                            CertStore.bundled(context, CertStore.DIST) != null
                    }
                    if (bundledOk) {
                        Text(
                            stringResource(R.string.set_sign_bundled),
                            color = LayiixOk,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedTextField(
                        value = authorPwd,
                        onValueChange = { authorPwd = it.take(128) },
                        label = { Text("${stringResource(R.string.set_sign_author)} · ${stringResource(R.string.set_sign_pwd)}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { authorPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (authorBytes != null) "author.p12 ✓"
                            else stringResource(R.string.set_sign_pick, "author.p12").uppercase(),
                        )
                    }
                    OutlinedTextField(
                        value = distPwd,
                        onValueChange = { distPwd = it.take(128) },
                        label = { Text("${stringResource(R.string.set_sign_dist)} · ${stringResource(R.string.set_sign_pwd)}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { distPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (distBytes != null) "distributor.p12 ✓"
                            else stringResource(R.string.set_sign_pick, "distributor.p12").uppercase(),
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val ab = authorBytes ?: error("author.p12")
                                    val db = distBytes ?: error("distributor.p12")
                                    withContext(Dispatchers.IO) {
                                        TizenSigner.loadP12(ab, authorPwd)
                                        TizenSigner.loadP12(db, distPwd)
                                    }
                                    CertStore.save(context, CertStore.AUTHOR, ab, authorPwd)
                                    CertStore.save(context, CertStore.DIST, db, distPwd)
                                    authorBytes = null
                                    distBytes = null
                                    authorPwd = ""
                                    distPwd = ""
                                    certMsg = context.getString(R.string.set_sign_saved)
                                    certTick++
                                } catch (e: Exception) {
                                    certMsg = context.getString(R.string.set_sign_err, e.message ?: "?")
                                }
                            }
                        },
                        enabled = authorBytes != null && distBytes != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.set_sign_save).uppercase()) }
                    if (certMsg.isNotBlank()) Text(certMsg, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Kicker("05 // ${stringResource(R.string.k_privacy)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.set_privacy), style = MaterialTheme.typography.titleMedium)
                PrivacyRow(stringResource(R.string.priv1))
                PrivacyRow(stringResource(R.string.priv2))
                PrivacyRow(stringResource(R.string.priv3))
                PrivacyRow(stringResource(R.string.priv4))
            }
        }
    }
}

fun pickLang(context: Context, tag: String, onDone: (String) -> Unit) {
    LangPrefs.set(context, tag)
    onDone(LangPrefs.get(context))
    (context as? Activity)?.recreate()
}

@Composable
fun LangOption(tag: String, label: String, current: String, onPick: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onPick(tag) },
    ) {
        RadioButton(selected = current == tag, onClick = { onPick(tag) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun PrivacyRow(text: String) {
    Row {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LayiixOk)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun InfoScreen() {
    val scroll = rememberScrollState()
    var showLic by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Kicker("01 // ${stringResource(R.string.k_app)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixPrimary.copy(alpha = 0.35f)),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.info_name, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.slogan),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = LayiixCyan,
                    ),
                )
                Text(
                    stringResource(R.string.info_pkg, BuildConfig.APPLICATION_ID),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(stringResource(R.string.info_desc), style = MaterialTheme.typography.bodySmall)
            }
        }
        Kicker("02 // ${stringResource(R.string.k_limits)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.info_limits), style = MaterialTheme.typography.titleMedium)
                listOf(
                    stringResource(R.string.lim1),
                    stringResource(R.string.lim2),
                    stringResource(R.string.lim3),
                    stringResource(R.string.lim4),
                    stringResource(R.string.lim5),
                ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
        Kicker("03 // ${stringResource(R.string.k_credits)}")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LayiixSurface2),
            border = androidx.compose.foundation.BorderStroke(1.dp, LayiixLine),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.info_credits), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.info_credits_text), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.info_showlic), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = showLic, onCheckedChange = { showLic = it })
                }
                if (showLic) {
                    SelectionContainer {
                        Text(stringResource(R.string.info_lic), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        DevFooter()
    }
}
