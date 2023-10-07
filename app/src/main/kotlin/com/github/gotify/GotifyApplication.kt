package com.github.gotify

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import androidx.preference.PreferenceManager
import com.github.gotify.log.LoggerHelper
import com.github.gotify.log.UncaughtExceptionHandler
import com.github.gotify.settings.ThemeHelper
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

        super.onCreate()
    }
}
