package com.github.gotify.settings

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.github.gotify.R

internal object ThemeHelper {
    fun setTheme(context: Context, newTheme: String) {
        AppCompatDelegate.setDefaultNightMode(ofKey(context, newTheme))
    }

    private fun ofKey(context: Context, newTheme: String): Int {
        return if (context.getString(R.string.theme_dark) == newTheme) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else if (context.getString(R.string.theme_light) == newTheme) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
