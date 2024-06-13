package com.github.gotify.init

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.github.gotify.R
import com.github.gotify.Settings
import com.github.gotify.api.ApiException
import com.github.gotify.api.Callback
import com.github.gotify.api.Callback.SuccessCallback
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.model.User
import com.github.gotify.client.model.VersionInfo
import com.github.gotify.login.LoginActivity
import com.github.gotify.messages.MessagesActivity
import com.github.gotify.service.WebSocketService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.livinglifetechway.quickpermissionskotlin.runWithPermissions
import com.livinglifetechway.quickpermissionskotlin.util.QuickPermissionsOptions
import com.livinglifetechway.quickpermissionskotlin.util.QuickPermissionsRequest
import org.tinylog.kotlin.Logger

internal class InitializationActivity : AppCompatActivity() {

    private lateinit var settings: Settings
    private var splashScreenActive = true

    @RequiresApi(Build.VERSION_CODES.S)
    private val activityResultLauncher =
        registerForActivityResult(StartActivityForResult()) {
            requestAlarmPermissionOrAuthenticate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        Logger.info("Entering ${javaClass.simpleName}")

        installSplashScreen().setKeepOnScreenCondition { splashScreenActive }

        if (settings.tokenExists()) {
            runWithPostNotificationsPermission {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                    // Android 14 and above
                    requestAlarmPermissionOrAuthenticate()
                } else {
                    // Android 13 and below
                    tryAuthenticate()
                }
            }
        } else {
            showLogin()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestAlarmPermissionOrAuthenticate() {
        val manager = ContextCompat.getSystemService(this, AlarmManager::class.java)
        if (manager?.canScheduleExactAlarms() == true) {
            tryAuthenticate()
        } else {
            stopSlashScreen()
            alarmDialog()
        }
    }

    private fun showLogin() {
        splashScreenActive = false
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
        stopSlashScreen()
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
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.oops)
            .setMessage(message)
            .setPositiveButton(R.string.retry) { _, _ -> tryAuthenticate() }
            .setNegativeButton(R.string.logout) { _, _ -> showLogin() }
            .setCancelable(false)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun alarmDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.permissions_alarm_prompt))
            .setPositiveButton(getString(R.string.permissions_dialog_grant)) { _, _ ->
                Intent(
                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                ).apply {
                    activityResultLauncher.launch(this)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun authenticated(user: User) {
        Logger.info("Authenticated as ${user.name}")

        settings.setUser(user.name, user.isAdmin)
        requestVersion {
            splashScreenActive = false
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
                Logger.info("Server version: ${version.version}@${version.buildDate}")
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
        ClientFactory.versionApi(settings)
            .version
            .enqueue(Callback.callInUI(this, callback, errorCallback))
    }

    private fun runWithPostNotificationsPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            val quickPermissionsOption = QuickPermissionsOptions(
                handleRationale = true,
                handlePermanentlyDenied = true,
                preRationaleAction = { stopSlashScreen() },
                rationaleMethod = { req -> processPermissionRationale(req) },
                permissionsDeniedMethod = { req -> processPermissionRationale(req) },
                permanentDeniedMethod = { req -> processPermissionsPermanentDenied(req) }
            )
            runWithPermissions(
                Manifest.permission.POST_NOTIFICATIONS,
                options = quickPermissionsOption,
                callback = action
            )
        } else {
            // Android 12 and below
            action()
        }
    }

    private fun stopSlashScreen() {
        splashScreenActive = false
        setContentView(R.layout.splash)
    }

    private fun processPermissionRationale(req: QuickPermissionsRequest) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.permissions_notification_denied_temp))
            .setPositiveButton(getString(R.string.permissions_dialog_grant)) { _, _ ->
                req.proceed()
            }
            .setCancelable(false)
            .show()
    }

    private fun processPermissionsPermanentDenied(req: QuickPermissionsRequest) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.permissions_notification_denied_permanent))
            .setPositiveButton(getString(R.string.permissions_dialog_grant)) { _, _ ->
                req.openAppSettings()
            }
            .setCancelable(false)
            .show()
    }
}
