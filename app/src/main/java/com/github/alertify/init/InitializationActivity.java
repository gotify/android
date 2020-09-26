package com.github.alertify.init;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;


import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import com.github.alertify.NotificationSupport;
import com.github.alertify.R;
import com.github.alertify.Settings;
import com.github.alertify.api.ApiException;
import com.github.alertify.api.Callback;
import com.github.alertify.api.ClientFactory;
import com.github.alertify.client.model.User;
import com.github.alertify.client.model.VersionInfo;
import com.github.alertify.log.Log;
import com.github.alertify.log.UncaughtExceptionHandler;
import com.github.alertify.login.LoginActivity;
import com.github.alertify.init.messages.MessagesActivity;
import com.github.alertify.service.WebSocketService;
import com.github.alertify.settings.ThemeHelper;
import com.github.alertify.WEA.CellBroadcastAlertService;

import static com.github.alertify.api.Callback.callInUI;

public class InitializationActivity extends AppCompatActivity {
    private Settings settings;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.init(this);
        String theme =
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(
                                getString(R.string.setting_key_theme),
                                getString(R.string.theme_default));
        ThemeHelper.setTheme(this, theme);

        setContentView(R.layout.splash);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationSupport.createChannels(
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE));
            CellBroadcastAlertService.createNotificationChannels(this);
        }
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
        }

        UncaughtExceptionHandler.registerCurrentThread();
        settings = new Settings(this);
        Log.i("Entering " + getClass().getSimpleName());

        if (settings.tokenExists()) {
            tryAuthenticate();
        } else {
            showLogin();
        }
    }

    private void showLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void tryAuthenticate() {
        ClientFactory.userApiWithToken(settings)
                .currentUser()
                .enqueue(callInUI(this, this::authenticated, this::failed));
    }

    private void failed(ApiException exception) {
        if (exception.code() == 0) {
            dialog(getString(R.string.not_available, settings.url()));
            return;
        }

        if (exception.code() == 401) {
            dialog(getString(R.string.auth_failed));
            return;
        }

        String response = exception.body();
        response = response.substring(0, Math.min(200, response.length()));
        dialog(getString(R.string.other_error, settings.url(), exception.code(), response));
    }

    private void dialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.oops)
                .setMessage(message)
                .setPositiveButton(R.string.retry, (a, b) -> tryAuthenticate())
                .setNegativeButton(R.string.logout, (a, b) -> showLogin())
                .show();
    }

    private void authenticated(User user) {
        Log.i("Authenticated as " + user.getName());

        settings.user(user.getName(), user.isAdmin());
        requestVersion(
                () -> {
                    startActivity(new Intent(this, MessagesActivity.class));
                    finish();
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, WebSocketService.class));
        } else {
            startService(new Intent(this, WebSocketService.class));
        }
    }

    private void requestVersion(Runnable runnable) {
        requestVersion(
                (version) -> {
                    Log.i("Server version: " + version.getVersion() + "@" + version.getBuildDate());
                    settings.serverVersion(version.getVersion());
                    runnable.run();
                },
                (e) -> {
                    runnable.run();
                });
    }

    private void requestVersion(
            final Callback.SuccessCallback<VersionInfo> callback,
            final Callback.ErrorCallback errorCallback) {
        ClientFactory.versionApi(settings.url(), settings.sslSettings())
                .getVersion()
                .enqueue(callInUI(this, callback, errorCallback));
    }
}
