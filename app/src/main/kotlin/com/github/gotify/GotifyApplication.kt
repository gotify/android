package com.github.gotify

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import androidx.preference.PreferenceManager
import com.github.gotify.api.CertUtils
import com.github.gotify.log.LoggerHelper
import com.github.gotify.log.UncaughtExceptionHandler
import com.github.gotify.settings.ThemeHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.tinylog.kotlin.Logger

class GotifyApplication : Application() {
    override fun onCreate() {
        LoggerHelper.init(this)
        UncaughtExceptionHandler.registerCurrentThread()
        Logger.info("${javaClass.simpleName}: onCreate")

        val theme = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.setting_key_theme), getString(R.string.theme_default))!!
        ThemeHelper.setTheme(this, theme)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationSupport.createForegroundChannel(
                this,
                this.getSystemService(NotificationManager::class.java)
            )
        }

        val settings = Settings(this)
        if (settings.legacyCert != null) {
            Logger.info("Migrating legacy CA cert to new location")
            try {
                val legacyCert = settings.legacyCert
                settings.legacyCert = null
                val caCertFile = File(settings.filesDir, CertUtils.CA_CERT_NAME)
                FileOutputStream(caCertFile).use {
                    it.write(legacyCert?.encodeToByteArray())
                }
                settings.caCertPath = caCertFile.absolutePath
                Logger.info("Migration of legacy CA cert succeeded")
            } catch (e: IOException) {
                Logger.error(e, "Migration of legacy CA cert failed")
            }
        }
        super.onCreate()
    }
}
