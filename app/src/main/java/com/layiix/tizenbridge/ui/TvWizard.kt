package com.layiix.tizenbridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.layiix.tizenbridge.R
import com.layiix.tizenbridge.prefs.TvProfile

/**
 * Wizard primo avvio: marca → (anno Samsung) → esito compatibilità.
 * Blocco installazione solo per piattaforme dichiaratamente non-Tizen.
 */
@Composable
fun TvWizardScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    var brand by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    fun finish(b: String, y: String) {
        TvProfile.save(context, b, y)
        onDone()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Kicker("GUIDA // TV")
        Text(
            stringResource(R.string.wiz_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            stringResource(R.string.wiz_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = LayiixMuted,
        )

        when (step) {
            0 -> {
                WizOption("Samsung", stringResource(R.string.wiz_samsung_d)) {
                    brand = TvProfile.SAMSUNG
                    step = 1
                }
                WizOption("LG", stringResource(R.string.wiz_lg_d)) {
                    brand = TvProfile.LG
                    step = 2
                }
                WizOption(
                    stringResource(R.string.wiz_android),
                    stringResource(R.string.wiz_android_d),
                ) {
                    brand = TvProfile.ANDROID
                    step = 2
                }
                WizOption(
                    stringResource(R.string.wiz_other),
                    stringResource(R.string.wiz_other_d),
                ) {
                    brand = TvProfile.OTHER
                    step = 2
                }
                TextButton(
                    onClick = { finish("", "") },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.wiz_skip)) }
            }
            1 -> {
                Text(
                    stringResource(R.string.wiz_year),
                    style = MaterialTheme.typography.titleMedium,
                )
                for (y in TvProfile.YEARS) {
                    val tizen = TvProfile.tizenForYear(y)
                    WizOption(y, "Tizen $tizen") {
                        year = y
                        step = 2
                    }
                }
                WizOption(
                    stringResource(R.string.wiz_legacy),
                    stringResource(R.string.wiz_legacy_d_short),
                ) {
                    year = TvProfile.LEGACY
                    step = 2
                }
                WizOption(
                    stringResource(R.string.wiz_year_unknown),
                    stringResource(R.string.wiz_unknown_d_short),
                ) {
                    year = TvProfile.UNKNOWN
                    step = 2
                }
                TextButton(onClick = { step = 0 }) { Text(stringResource(R.string.wiz_back)) }
            }
            else -> {
                val tizen = TvProfile.tizenForYear(year)
                when {
                    brand == TvProfile.SAMSUNG && tizen != null -> {
                        ResultCard(
                            title = stringResource(R.string.wiz_ok_title),
                            body = stringResource(R.string.wiz_ok_desc, tizen),
                            ok = true,
                        )
                        Button(
                            onClick = { finish(brand, year) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.wiz_start).uppercase()) }
                    }
                    brand == TvProfile.SAMSUNG && year == TvProfile.UNKNOWN -> {
                        ResultCard(
                            title = stringResource(R.string.wiz_year_unknown),
                            body = stringResource(R.string.wiz_unknown_desc),
                            ok = true,
                        )
                        Button(
                            onClick = { finish(brand, year) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.wiz_continue).uppercase()) }
                    }
                    brand == TvProfile.LG -> {
                        ResultCard(
                            title = stringResource(R.string.wiz_ko_title),
                            body = stringResource(R.string.wiz_lg_desc),
                            ok = false,
                        )
                        Button(
                            onClick = { finish(brand, "") },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.wiz_gotit).uppercase()) }
                    }
                    brand == TvProfile.ANDROID -> {
                        ResultCard(
                            title = stringResource(R.string.wiz_ko_title),
                            body = stringResource(R.string.wiz_android_desc),
                            ok = false,
                        )
                        Button(
                            onClick = { finish(brand, "") },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.wiz_gotit).uppercase()) }
                    }
                    brand == TvProfile.OTHER -> {
                        ResultCard(
                            title = stringResource(R.string.wiz_ko_title),
                            body = stringResource(R.string.wiz_other_desc),
                            ok = false,
                        )
                        Button(
                            onClick = { finish(brand, "") },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.wiz_gotit).uppercase()) }
                    }
                    else -> {
                        ResultCard(
                            title = stringResource(R.string.wiz_ko_title),
                            body = stringResource(R.string.wiz_legacy_desc),
                            ok = false,
                        )
                        Button(
                            onClick = { finish(brand, year) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.wiz_gotit).uppercase()) }
                    }
                }
                TextButton(onClick = { step = if (brand == TvProfile.SAMSUNG) 1 else 0 }) {
                    Text(stringResource(R.string.wiz_back))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun WizOption(title: String, desc: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = LayiixMuted,
            )
        }
    }
}

@Composable
fun ResultCard(title: String, body: String, ok: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) LayiixSurface2 else LayiixSurface2,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (ok) LayiixOk.copy(alpha = 0.5f) else LayiixError.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (ok) LayiixOk else LayiixError,
                textAlign = TextAlign.Center,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
