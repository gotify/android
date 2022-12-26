package com.github.gotify.init

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.github.gotify.NotificationSupport
import com.github.gotify.R
import com.github.gotify.Settings
import com.github.gotify.api.ApiException
import com.github.gotify.api.Callback
import com.github.gotify.api.Callback.SuccessCallback
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.model.User
import com.github.gotify.client.model.VersionInfo
import com.github.gotify.log.Log
import com.github.gotify.log.UncaughtExceptionHandler
import com.github.gotify.login.LoginActivity
import com.github.gotify.messages.MessagesActivity
import com.github.gotify.service.WebSocketService
import com.github.gotify.settings.ThemeHelper

internal class InitializationActivity : AppCompatActivity() {

    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.init(this)
        val theme = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.setting_key_theme), getString(R.string.theme_default))!!
        ThemeHelper.setTheme(this, theme)

        setContentView(R.layout.splash)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationSupport.createChannels(
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            )
        }
        UncaughtExceptionHandler.registerCurrentThread()
        settings = Settings(this)
        Log.i("Entering ${javaClass.simpleName}")

        if (settings.tokenExists()) {
            tryAuthenticate()
        } else {
            showLogin()
        }
    }

    private fun showLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun tryAuthenticate() {
        ClientFactory.userApiWithToken(settings)
                ?.currentUser()
                ?.enqueue(
                    Callback.callInUI(this, { if (it != null) authenticated(it) }) { apiException ->
                        failed(apiException)
                    }
                )
    }

    private fun failed(exception: ApiException) {
        when (exception.code) {
            0 -> {
                dialog(getString(R.string.not_available, settings.url))
                return
            }
            401 -> {
                dialog(getString(R.string.auth_failed))
                return
            }
        }

        var response = exception.body
        response = response.substring(0, 200.coerceAtMost(response.length))
        dialog(getString(R.string.other_error, settings.url, exception.code, response))
    }

    private fun dialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.oops)
            .setMessage(message)
            .setPositiveButton(R.string.retry) { _, _ -> tryAuthenticate() }
            .setNegativeButton(R.string.logout) { _, _ -> showLogin() }
            .show()
    }

    private fun authenticated(user: User) {
        Log.i("Authenticated as ${user.name}")

        settings.setUser(user.name, user.isAdmin)
        requestVersion {
            startActivity(Intent(this, MessagesActivity::class.java))
            finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, WebSocketService::class.java))
        } else {
            startService(Intent(this, WebSocketService::class.java))
        }
    }

    private fun requestVersion(runnable: Runnable) {
        requestVersion({ version: VersionInfo? ->
            if (version != null) {
                Log.i("Server version: ${version.version}@${version.buildDate}")
                settings.serverVersion = version.version
            }
            runnable.run()
        }) { runnable.run() }
    }

    private fun requestVersion(
        callback: SuccessCallback<VersionInfo>,
        errorCallback: Callback.ErrorCallback
    ) {
        ClientFactory.versionApi(settings.url, settings.sslSettings())
            ?.version
            ?.enqueue(Callback.callInUI(this, callback, errorCallback))
    }
}
