package com.github.gotify.login;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.github.gotify.R;
import com.github.gotify.SSLSettings;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.Callback;
import com.github.gotify.api.CertUtils;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.ClientApi;
import com.github.gotify.client.api.UserApi;
import com.github.gotify.client.model.Client;
import com.github.gotify.client.model.VersionInfo;
import com.github.gotify.init.InitializationActivity;
import com.github.gotify.log.Log;
import com.github.gotify.log.LogsActivity;
import com.github.gotify.log.UncaughtExceptionHandler;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import okhttp3.HttpUrl;

import static com.github.gotify.api.Callback.callInUI;

public class LoginActivity extends AppCompatActivity {

    // return value from startActivityForResult when choosing a certificate
    private final int CA_FILE_SELECT_CODE = 1;
    private final int CLI_CERT_FILE_SELECT_CODE = 2;

    @BindView(R.id.username)
    EditText usernameField;

    @BindView(R.id.gotify_url)
    EditText urlField;

    @BindView(R.id.password)
    EditText passwordField;

    @BindView(R.id.advanced_settings)
    ImageView toggleAdvanced;

    @BindView(R.id.checkurl)
    Button checkUrlButton;

    @BindView(R.id.login)
    Button loginButton;

    @BindView(R.id.checkurl_progress)
    ProgressBar checkUrlProgress;

    @BindView(R.id.login_progress)
    ProgressBar loginProgress;

    private Settings settings;

    private boolean disableSSLValidation;
    private String caCertContents;
    private String clientCertContents;
    private String clientCertPassword;
    private String clientCertUri;
    private AdvancedDialog advancedDialog;

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
        invalidateUrl();
    }

    private void invalidateUrl() {
        usernameField.setVisibility(View.GONE);
        passwordField.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);
        checkUrlButton.setText(getString(R.string.check_url));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @OnClick(R.id.checkurl)
    public void doCheckUrl() {
        String url = urlField.getText().toString();
        HttpUrl parsedUrl = HttpUrl.parse(url);
        if (parsedUrl == null) {
            Utils.showSnackBar(LoginActivity.this, "Invalid URL (include http:// or https://)");
            return;
        }

        if ("http".equals(parsedUrl.scheme())) {
            showHttpWarning();
        }

        checkUrlProgress.setVisibility(View.VISIBLE);
        checkUrlButton.setVisibility(View.GONE);

        final String trimmedUrl = url.trim();
        final String fixedUrl =
                trimmedUrl.endsWith("/")
                        ? trimmedUrl.substring(0, trimmedUrl.length() - 1)
                        : trimmedUrl;

        try {
            ClientFactory.versionApi(fixedUrl, tempSSLSettings())
                    .getVersion()
                    .enqueue(callInUI(this, onValidUrl(fixedUrl), onInvalidUrl(fixedUrl)));
        } catch (Exception e) {
            checkUrlProgress.setVisibility(View.GONE);
            checkUrlButton.setVisibility(View.VISIBLE);
            String errorMsg =
                    getString(R.string.version_failed, fixedUrl + "/version", e.getMessage());
            Utils.showSnackBar(LoginActivity.this, errorMsg);
        }
    }

    public void showHttpWarning() {
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_Dialog))
                .setTitle(R.string.warning)
                .setCancelable(true)
                .setMessage(R.string.http_warning)
                .setPositiveButton(R.string.i_understand, (a, b) -> {})
                .show();
    }

    @OnClick(R.id.open_logs)
    public void openLogs() {
        startActivity(new Intent(this, LogsActivity.class));
    }

    @OnClick(R.id.advanced_settings)
    void toggleShowAdvanced() {
        String selectedCertName =
                caCertContents != null ? getNameOfCertContent(caCertContents) : null;
        clientCertUri = settings.clientCertUri();
        clientCertContents = settings.clientCert();
        clientCertPassword = settings.clientCertPass();

        advancedDialog =
                new AdvancedDialog(this)
                        .onDisableSSLChanged(
                                (ignored, disable) -> {
                                    invalidateUrl();
                                    disableSSLValidation = disable;
                                })
                        .onClickSelectCaCertificate(
                                () -> {
                                    invalidateUrl();
                                    doSelectCertificate(R.string.select_ca_file, CA_FILE_SELECT_CODE);
                                })
                        .onClickRemoveCaCertificate(
                                () -> {
                                    invalidateUrl();
                                    caCertContents = null;
                                })
                        .onClickSelectClientCertificate(() ->
                                doSelectCertificate(R.string.select_client_cert_file, CLI_CERT_FILE_SELECT_CODE))
                        .onClickRemoveClientCertificate(() -> {
                            clientCertContents = null;
                            settings.clientCertPass("");
                            clientCertPassword = "";
                            settings.clientCertUri(null);
                            clientCertUri = null;

                        })
                        .show(disableSSLValidation, selectedCertName, clientCertUri, clientCertPassword, settings);
    }

    private void doSelectCertificate(int intentChooserDescriptionCode, int FILE_SELECT_CODE) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // we don't really care what kind of file it is as long as we can parse it
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, getString(intentChooserDescriptionCode)),
                    FILE_SELECT_CODE);
        } catch (ActivityNotFoundException e) {
            // case for user not having a file browser installed
            Utils.showSnackBar(this, getString(R.string.please_install_file_browser));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == CA_FILE_SELECT_CODE || requestCode ==  CLI_CERT_FILE_SELECT_CODE) {
                if (resultCode != RESULT_OK) {
                    throw new IllegalArgumentException(String.format("result was %d", resultCode));
                } else if (data == null) {
                    throw new IllegalArgumentException("file path was null");
                }

                Uri uri = data.getData();
                if (uri == null) {
                    throw new IllegalArgumentException("file path was null");
                }

                InputStream fileStream = getContentResolver().openInputStream(uri);
                if (fileStream == null) {
                    throw new IllegalArgumentException("file path was invalid");
                }

                if (requestCode == CA_FILE_SELECT_CODE) {
                    String content = Utils.readFileFromStream(fileStream);
                    String name = getNameOfCertContent(content);
                    // temporarily set the contents (don't store to settings until they decide to login)
                    caCertContents = content;
                    advancedDialog.showRemoveCACertificate(name);
                } else if (requestCode ==  CLI_CERT_FILE_SELECT_CODE) {
                    String content = Utils.binaryFileToBase64(fileStream);

                    clientCertContents = content;
                    String path = uri.getLastPathSegment();

                    settings.clientCert(clientCertContents);
                    settings.clientCertUri(path);
                    advancedDialog.showRemoveClientCertificate(path);
                }
            }
        } catch (Exception e) {
            Utils.showSnackBar(this, getString(R.string.select_ca_failed, e.getMessage()));
        }
    }

    private String getNameOfCertContent(String content) {
        Certificate ca = CertUtils.parseCertificate(content);
        return ((X509Certificate) ca).getSubjectDN().getName();
    }

    private Callback.SuccessCallback<VersionInfo> onValidUrl(String url) {
        return (version) -> {
            settings.url(url);
            checkUrlProgress.setVisibility(View.GONE);
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
            checkUrlProgress.setVisibility(View.GONE);
            checkUrlButton.setVisibility(View.VISIBLE);
            Utils.showSnackBar(LoginActivity.this, versionError(url, exception));
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @OnClick(R.id.login)
    public void doLogin() {
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        loginButton.setVisibility(View.GONE);
        loginProgress.setVisibility(View.VISIBLE);

        ApiClient client =
                ClientFactory.basicAuth(settings.url(), tempSSLSettings(), username, password);
        client.createService(UserApi.class)
                .currentUser()
                .enqueue(callInUI(this, (user) -> newClientDialog(client), this::onInvalidLogin));
    }

    private void onInvalidLogin(ApiException e) {
        loginButton.setVisibility(View.VISIBLE);
        loginProgress.setVisibility(View.GONE);
        Utils.showSnackBar(this, getString(R.string.wronguserpw));
    }

    private void newClientDialog(ApiClient client) {
        EditText clientName = new EditText(this);
        clientName.setText(Build.MODEL);

        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_Dialog))
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
            client.createService(ClientApi.class)
                    .createClient(newClient)
                    .enqueue(callInUI(this, this::onCreatedClient, this::onFailedToCreateClient));
        };
    }

    private void onCreatedClient(Client client) {
        settings.token(client.getToken());
        settings.validateSSL(!disableSSLValidation);
        settings.cert(caCertContents);
        settings.clientCert(clientCertContents);

        Utils.showSnackBar(this, getString(R.string.created_client));
        startActivity(new Intent(this, InitializationActivity.class));
        finish();
    }

    private void onFailedToCreateClient(ApiException e) {
        Utils.showSnackBar(this, getString(R.string.create_client_failed));
        loginProgress.setVisibility(View.GONE);
        loginButton.setVisibility(View.VISIBLE);
    }

    private void onCancelClientDialog(DialogInterface dialog, int which) {
        loginProgress.setVisibility(View.GONE);
        loginButton.setVisibility(View.VISIBLE);
    }

    private String versionError(String url, ApiException exception) {
        return getString(R.string.version_failed_status_code, url + "/version", exception.code());
    }

    private SSLSettings tempSSLSettings() {
        return new SSLSettings(!disableSSLValidation, caCertContents, clientCertContents, clientCertPassword);
    }
}
