package com.github.gotify.init;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.github.gotify.NotificationSupport;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.Callback;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.model.User;
import com.github.gotify.client.model.VersionInfo;
import com.github.gotify.log.Log;
import com.github.gotify.log.UncaughtExceptionHandler;
import com.github.gotify.login.LoginActivity;
import com.github.gotify.messages.MessagesActivity;
import com.github.gotify.service.WebSocketService;

import static com.github.gotify.api.Callback.callInUI;

public class InitializationActivity extends AppCompatActivity {
    private Settings settings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.init(this);
        setContentView(R.layout.splash);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationSupport.createChannels(
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE));
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
