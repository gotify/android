package com.github.gotify.login;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.api.Api;
import com.github.gotify.api.Callback;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.ApiException;
import com.github.gotify.client.api.TokenApi;
import com.github.gotify.client.api.UserApi;
import com.github.gotify.client.model.Client;
import com.github.gotify.client.model.VersionInfo;
import com.github.gotify.init.InitializationActivity;
import com.github.gotify.log.Log;
import com.github.gotify.log.UncaughtExceptionHandler;
import com.squareup.okhttp.HttpUrl;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.username)
    EditText usernameField;

    @BindView(R.id.gotify_url)
    EditText urlField;

    @BindView(R.id.password)
    EditText passwordField;

    @BindView(R.id.checkurl)
    Button checkUrlButton;

    @BindView(R.id.login)
    Button loginButton;

    @BindView(R.id.checkurl_progress)
    ProgressBar checkUrlProgress;

    @BindView(R.id.login_progress)
    ProgressBar loginProgress;

    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UncaughtExceptionHandler.registerCurrentThread();
        setContentView(R.layout.activity_login);
        Log.i("Entering " + getClass().getSimpleName());
        ButterKnife.bind(this);
        settings = new Settings(this);
    }

    @OnTextChanged(R.id.gotify_url)
    public void onUrlChange() {
        usernameField.setVisibility(View.INVISIBLE);
        passwordField.setVisibility(View.INVISIBLE);
        loginButton.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.checkurl)
    public void doCheckUrl() {
        String url = urlField.getText().toString();
        if (HttpUrl.parse(url) == null) {
            Utils.showSnackBar(LoginActivity.this, "Invalid URL (include http:// or https://)");
            return;
        }

        checkUrlProgress.setVisibility(View.VISIBLE);
        checkUrlButton.setVisibility(View.INVISIBLE);

        final String fixedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        Api.withLogging(ClientFactory.versionApi(fixedUrl)::getVersionAsync)
                .handleInUIThread(this, onValidUrl(fixedUrl), onInvalidUrl(fixedUrl));
    }

    private Callback.SuccessCallback<VersionInfo> onValidUrl(String url) {
        return (version) -> {
            settings.url(url);
            checkUrlProgress.setVisibility(View.INVISIBLE);
            checkUrlButton.setVisibility(View.VISIBLE);
            checkUrlButton.setText(getString(R.string.found_gotify_version, version.getVersion()));
            usernameField.setVisibility(View.VISIBLE);
            usernameField.requestFocus();
            passwordField.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.VISIBLE);
        };
    }

    private Callback.ErrorCallback onInvalidUrl(String url) {
        return (exception) -> {
            checkUrlProgress.setVisibility(View.INVISIBLE);
            checkUrlButton.setVisibility(View.VISIBLE);
            Utils.showSnackBar(LoginActivity.this, versionError(url, exception));
        };
    }

    @OnClick(R.id.login)
    public void doLogin() {
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        loginButton.setVisibility(View.INVISIBLE);
        loginProgress.setVisibility(View.VISIBLE);

        ApiClient client = ClientFactory.basicAuth(settings.url(), username, password);
        Api.withLogging(new UserApi(client)::currentUserAsync)
                .handleInUIThread(this, (user) -> newClientDialog(client), this::onInvalidLogin);
    }

    private void onInvalidLogin(ApiException e) {
        loginButton.setVisibility(View.VISIBLE);
        loginProgress.setVisibility(View.INVISIBLE);
        Utils.showSnackBar(this, getString(R.string.wronguserpw));
    }

    private void newClientDialog(ApiClient client) {
        EditText clientName = new EditText(this);
        clientName.setText(Build.MODEL);

        new AlertDialog.Builder(this)
                .setTitle(R.string.create_client_title)
                .setMessage(R.string.create_client_message)
                .setView(clientName)
                .setPositiveButton(R.string.create, doCreateClient(client, clientName))
                .setNegativeButton(R.string.cancel, this::onCancelClientDialog)
                .show();
    }

    public DialogInterface.OnClickListener doCreateClient(ApiClient client, EditText nameProvider) {
        return (a, b) -> {
            Client newClient = new Client().name(nameProvider.getText().toString());
            Api.<Client>withLogging((cb) -> new TokenApi(client).createClientAsync(newClient, cb))
                    .handleInUIThread(this, this::onCreatedClient, this::onFailedToCreateClient);
        };
    }

    private void onCreatedClient(Client client) {
        settings.token(client.getToken());
        Utils.showSnackBar(this, getString(R.string.created_client));
        startActivity(new Intent(this, InitializationActivity.class));
        finish();
    }

    private void onFailedToCreateClient(ApiException e) {
        Utils.showSnackBar(this, getString(R.string.create_client_failed));
        loginProgress.setVisibility(View.INVISIBLE);
        loginButton.setVisibility(View.VISIBLE);
    }

    private void onCancelClientDialog(DialogInterface dialog, int which) {
        loginProgress.setVisibility(View.INVISIBLE);
        loginButton.setVisibility(View.VISIBLE);
    }

    private String versionError(String url, ApiException exception) {
        return getString(R.string.version_failed, url + "/version", exception.getCode());
    }
}
