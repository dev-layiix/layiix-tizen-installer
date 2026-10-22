package com.layiix.tizenbridge

import android.app.Application
import android.content.Context
import com.layiix.tizenbridge.log.AppLogStore
import com.layiix.tizenbridge.prefs.LangPrefs
import java.io.File

class LayiixApp : Application() {
    lateinit var logs: AppLogStore
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LangPrefs.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        val dir = File(filesDir, "logs")
        logs = AppLogStore(dir)
        logs.info("app", "Layiix TV Sideload avviato (v1.1.0).")
    }
}
