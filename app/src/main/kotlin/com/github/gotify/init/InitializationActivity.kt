package com.github.gotify.init

import android.Manifest
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
import com.livinglifetechway.quickpermissionskotlin.runWithPermissions
import com.livinglifetechway.quickpermissionskotlin.util.QuickPermissionsOptions
import com.livinglifetechway.quickpermissionskotlin.util.QuickPermissionsRequest

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
            runWithNeededPermissions {
                tryAuthenticate()
            }
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
            .currentUser()
            .enqueue(
                Callback.callInUI(
                    this,
                    onSuccess = Callback.SuccessBody { user -> authenticated(user) },
                    onError = { exception -> failed(exception) }
                )
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
        response = response.take(200)
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
        requestVersion(
            callback = Callback.SuccessBody { version: VersionInfo ->
                Log.i("Server version: ${version.version}@${version.buildDate}")
                settings.serverVersion = version.version
                runnable.run()
            },
            errorCallback = { runnable.run() }
        )
    }

    private fun requestVersion(
        callback: SuccessCallback<VersionInfo>,
        errorCallback: Callback.ErrorCallback
    ) {
        ClientFactory.versionApi(settings.url, settings.sslSettings())
            .version
            .enqueue(Callback.callInUI(this, callback, errorCallback))
    }

    private fun runWithNeededPermissions(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val quickPermissionsOption = QuickPermissionsOptions(
                handleRationale = true,
                handlePermanentlyDenied = true,
                rationaleMethod = { req -> processPermissionRationale(req) },
                permissionsDeniedMethod = { req -> processPermissionRationale(req) },
                permanentDeniedMethod = { req -> processPermissionsPermanentDenied(req) }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 and above
                runWithPermissions(
                    Manifest.permission.SCHEDULE_EXACT_ALARM,
                    Manifest.permission.POST_NOTIFICATIONS,
                    options = quickPermissionsOption,
                    callback = action
                )
            } else {
                // Android 12 and Android 12L
                runWithPermissions(
                    Manifest.permission.SCHEDULE_EXACT_ALARM,
                    options = quickPermissionsOption,
                    callback = action
                )
            }
        } else {
            // Android 11 and below
            action()
        }
    }

    private fun processPermissionRationale(req: QuickPermissionsRequest) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.permissions_denied_temp))
            .setPositiveButton(getString(R.string.permissions_dialog_grant)) { _, _ ->
                req.proceed()
            }
            .setCancelable(false)
            .show()
    }

    private fun processPermissionsPermanentDenied(req: QuickPermissionsRequest) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.permissions_denied_permanent))
            .setPositiveButton(getString(R.string.permissions_dialog_grant)) { _, _ ->
                req.openAppSettings()
            }
            .setCancelable(false)
            .show()
    }
}
