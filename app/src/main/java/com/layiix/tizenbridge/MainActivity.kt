package com.layiix.tizenbridge

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.layiix.tizenbridge.prefs.LangPrefs
import com.layiix.tizenbridge.state.ConnectionManager
import com.layiix.tizenbridge.ui.LayiixShell
import com.layiix.tizenbridge.ui.LayiixTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LangPrefs.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LayiixApp
        setContent {
            LayiixTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val manager = remember { ConnectionManager(app.logs, app.applicationContext) }
                    LayiixShell(manager = manager, logs = app.logs)
                }
            }
        }
    }
}
