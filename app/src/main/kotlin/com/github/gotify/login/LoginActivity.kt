package com.github.gotify.login

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.github.gotify.R
import com.github.gotify.SSLSettings
import com.github.gotify.Settings
import com.github.gotify.Utils
import com.github.gotify.api.ApiException
import com.github.gotify.api.Callback
import com.github.gotify.api.Callback.SuccessCallback
import com.github.gotify.api.CertUtils
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.ApiClient
import com.github.gotify.client.api.ClientApi
import com.github.gotify.client.api.UserApi
import com.github.gotify.client.model.Client
import com.github.gotify.client.model.VersionInfo
import com.github.gotify.databinding.ActivityLoginBinding
import com.github.gotify.databinding.ClientNameDialogBinding
import com.github.gotify.init.InitializationActivity
import com.github.gotify.log.LogsActivity
import com.github.gotify.log.UncaughtExceptionHandler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.cert.X509Certificate
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.tinylog.kotlin.Logger

internal class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var settings: Settings

    private var disableSslValidation = false
    private var caCertCN: String? = null
    private var caCertPath: String? = null
    private var clientCertPath: String? = null
    private var clientCertPassword: String? = null
    private lateinit var advancedDialog: AdvancedDialog

    private val caDialogResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                require(result.resultCode == RESULT_OK) { "result was ${result.resultCode}" }
                requireNotNull(result.data) { "file path was null" }

                val uri = result.data!!.data ?: throw IllegalArgumentException("file path was null")
                val fileStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("file path was invalid")
                val destinationFile = File(filesDir, CertUtils.CA_CERT_NAME)
                copyStreamToFile(fileStream, destinationFile)

                // temporarily store it (don't store to settings until they decide to login)
                caCertCN = getNameOfCertContent(destinationFile) ?: "unknown"
                caCertPath = destinationFile.absolutePath
                advancedDialog.showRemoveCaCertificate(caCertCN!!)
            } catch (e: Exception) {
                Utils.showSnackBar(this, getString(R.string.select_ca_failed, e.message))
            }
        }

    private val clientCertDialogResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                require(result.resultCode == RESULT_OK) { "result was ${result.resultCode}" }
                requireNotNull(result.data) { "file path was null" }

                val uri = result.data!!.data ?: throw IllegalArgumentException("file path was null")
                val fileStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("file path was invalid")
                val destinationFile = File(filesDir, CertUtils.CLIENT_CERT_NAME)
                copyStreamToFile(fileStream, destinationFile)

                // temporarily store it (don't store to settings until they decide to login)
                clientCertPath = destinationFile.absolutePath
                advancedDialog.showRemoveClientCertificate()
            } catch (e: Exception) {
                Utils.showSnackBar(this, getString(R.string.select_client_failed, e.message))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UncaughtExceptionHandler.registerCurrentThread()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Logger.info("Entering ${javaClass.simpleName}")
        settings = Settings(this)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        binding.gotifyUrlEditext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                invalidateUrl()
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        binding.checkurl.setOnClickListener { doCheckUrl() }
        binding.openLogs.setOnClickListener { openLogs() }
        binding.advancedSettings.setOnClickListener { toggleShowAdvanced() }
        binding.login.setOnClickListener { doLogin() }
    }

    private fun invalidateUrl() {
        binding.username.visibility = View.GONE
        binding.password.visibility = View.GONE
        binding.login.visibility = View.GONE
        binding.checkurl.text = getString(R.string.check_url)
    }

    private fun doCheckUrl() {
        val url = binding.gotifyUrlEditext.text.toString().trim().trimEnd('/')
        val parsedUrl = url.toHttpUrlOrNull()
        if (parsedUrl == null) {
            Utils.showSnackBar(this, "Invalid URL (include http:// or https://)")
            return
        }

        if ("http" == parsedUrl.scheme) {
            showHttpWarning()
        }

        binding.checkurlProgress.visibility = View.VISIBLE
        binding.checkurl.visibility = View.GONE

        try {
            ClientFactory.versionApi(settings, tempSslSettings(), url)
                .version
                .enqueue(Callback.callInUI(this, onValidUrl(url), onInvalidUrl(url)))
        } catch (e: Exception) {
            binding.checkurlProgress.visibility = View.GONE
            binding.checkurl.visibility = View.VISIBLE
            val errorMsg = getString(R.string.version_failed, "$url/version", e.message)
            Utils.showSnackBar(this, errorMsg)
        }
    }

    private fun showHttpWarning() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.warning)
            .setCancelable(true)
            .setMessage(R.string.http_warning)
            .setPositiveButton(R.string.i_understand, null)
            .show()
    }

    private fun openLogs() {
        startActivity(Intent(this, LogsActivity::class.java))
    }

    private fun toggleShowAdvanced() {
        advancedDialog = AdvancedDialog(this, layoutInflater)
            .onDisableSSLChanged { _, disable ->
                invalidateUrl()
                disableSslValidation = disable
            }
            .onClickSelectCaCertificate {
                invalidateUrl()
                doSelectCertificate(caDialogResultLauncher, R.string.select_ca_file)
            }
            .onClickRemoveCaCertificate {
                invalidateUrl()
                caCertPath = null
                clientCertPassword = null
            }
            .onClickSelectClientCertificate {
                invalidateUrl()
                doSelectCertificate(clientCertDialogResultLauncher, R.string.select_client_file)
            }
            .onClickRemoveClientCertificate {
                invalidateUrl()
                clientCertPath = null
            }
            .onClose { newPassword ->
                clientCertPassword = newPassword
            }
            .show(
                disableSslValidation,
                caCertPath,
                caCertCN,
                clientCertPath,
                clientCertPassword
            )
    }

    private fun doSelectCertificate(
        resultLauncher: ActivityResultLauncher<Intent>,
        @StringRes descriptionId: Int
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        // we don't really care what kind of file it is as long as we can parse it
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            resultLauncher.launch(Intent.createChooser(intent, getString(descriptionId)))
        } catch (e: ActivityNotFoundException) {
            // case for user not having a file browser installed
            Utils.showSnackBar(this, getString(R.string.please_install_file_browser))
        }
    }

    private fun getNameOfCertContent(file: File): String? {
        val ca = FileInputStream(file).use { CertUtils.parseCertificate(it) }
        return (ca as X509Certificate).subjectX500Principal.name
    }

    private fun onValidUrl(url: String): SuccessCallback<VersionInfo> {
        return Callback.SuccessBody { version ->
            settings.url = url
            binding.checkurlProgress.visibility = View.GONE
            binding.checkurl.visibility = View.VISIBLE
            binding.checkurl.text = getString(R.string.found_gotify_version, version.version)
            binding.username.visibility = View.VISIBLE
            binding.username.requestFocus()
            binding.password.visibility = View.VISIBLE
            binding.login.visibility = View.VISIBLE
        }
    }

    private fun onInvalidUrl(url: String): Callback.ErrorCallback {
        return Callback.ErrorCallback { exception ->
            binding.checkurlProgress.visibility = View.GONE
            binding.checkurl.visibility = View.VISIBLE
            Utils.showSnackBar(this, versionError(url, exception))
        }
    }

    private fun doLogin() {
        val username = binding.usernameEditext.text.toString()
        val password = binding.passwordEditext.text.toString()

        binding.login.visibility = View.GONE
        binding.loginProgress.visibility = View.VISIBLE

        val client = ClientFactory.basicAuth(settings, tempSslSettings(), username, password)
        client.createService(UserApi::class.java)
            .currentUser()
            .enqueue(
                Callback.callInUI(
                    this,
                    onSuccess = { newClientDialog(client) },
                    onError = { onInvalidLogin() }
                )
            )
    }

    private fun onInvalidLogin() {
        binding.login.visibility = View.VISIBLE
        binding.loginProgress.visibility = View.GONE
        Utils.showSnackBar(this, getString(R.string.wronguserpw))
    }

    private fun newClientDialog(client: ApiClient) {
        val clientDialogBinding = ClientNameDialogBinding.inflate(layoutInflater)
        val clientDialogEditext = clientDialogBinding.clientNameEditext
        clientDialogEditext.setText(Build.MODEL)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_client_title)
            .setMessage(R.string.create_client_message)
            .setView(clientDialogBinding.root)
            .setPositiveButton(R.string.create, doCreateClient(client, clientDialogEditext))
            .setNegativeButton(R.string.cancel) { _, _ -> onCancelClientDialog() }
            .setCancelable(false)
            .show()
    }

    private fun doCreateClient(
        client: ApiClient,
        nameProvider: TextInputEditText
    ): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            val newClient = Client().name(nameProvider.text.toString())
            client.createService(ClientApi::class.java)
                .createClient(newClient)
                .enqueue(
                    Callback.callInUI(
                        this,
                        onSuccess = Callback.SuccessBody { client -> onCreatedClient(client) },
                        onError = { onFailedToCreateClient() }
                    )
                )
        }
    }

    private fun onCreatedClient(client: Client) {
        settings.token = client.token
        settings.validateSSL = !disableSslValidation
        settings.caCertPath = caCertPath
        settings.clientCertPath = clientCertPath
        settings.clientCertPassword = clientCertPassword

        Utils.showSnackBar(this, getString(R.string.created_client))
        startActivity(Intent(this, InitializationActivity::class.java))
        finish()
    }

    private fun onFailedToCreateClient() {
        Utils.showSnackBar(this, getString(R.string.create_client_failed))
        binding.loginProgress.visibility = View.GONE
        binding.login.visibility = View.VISIBLE
    }

    private fun onCancelClientDialog() {
        binding.loginProgress.visibility = View.GONE
        binding.login.visibility = View.VISIBLE
    }

    private fun versionError(url: String, exception: ApiException): String {
        return getString(R.string.version_failed_status_code, "$url/version", exception.code)
    }

    private fun tempSslSettings(): SSLSettings {
        return SSLSettings(
            !disableSslValidation,
            caCertPath,
            clientCertPath,
            clientCertPassword
        )
    }

    private fun copyStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use {
            inputStream.copyTo(it)
        }
    }
}
