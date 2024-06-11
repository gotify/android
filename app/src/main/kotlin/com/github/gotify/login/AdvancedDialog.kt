package com.github.gotify.login

import android.content.Context
import android.view.LayoutInflater
import android.widget.CompoundButton
import androidx.core.widget.doOnTextChanged
import com.github.gotify.R
import com.github.gotify.databinding.AdvancedSettingsDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal class AdvancedDialog(
    private val context: Context,
    private val layoutInflater: LayoutInflater
) {
    private lateinit var binding: AdvancedSettingsDialogBinding
    private var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
    private lateinit var onClickSelectCaCertificate: Runnable
    private lateinit var onClickRemoveCaCertificate: Runnable
    private lateinit var onClickSelectClientCertificate: Runnable
    private lateinit var onClickRemoveClientCertificate: Runnable
    private lateinit var onClose: (password: String) -> Unit

    fun onDisableSSLChanged(
        onCheckedChangeListener: CompoundButton.OnCheckedChangeListener?
    ): AdvancedDialog {
        this.onCheckedChangeListener = onCheckedChangeListener
        return this
    }

    fun onClickSelectCaCertificate(onClickSelectCaCertificate: Runnable): AdvancedDialog {
        this.onClickSelectCaCertificate = onClickSelectCaCertificate
        return this
    }

    fun onClickRemoveCaCertificate(onClickRemoveCaCertificate: Runnable): AdvancedDialog {
        this.onClickRemoveCaCertificate = onClickRemoveCaCertificate
        return this
    }

    fun onClickSelectClientCertificate(onClickSelectClientCertificate: Runnable): AdvancedDialog {
        this.onClickSelectClientCertificate = onClickSelectClientCertificate
        return this
    }

    fun onClickRemoveClientCertificate(onClickRemoveClientCertificate: Runnable): AdvancedDialog {
        this.onClickRemoveClientCertificate = onClickRemoveClientCertificate
        return this
    }

    fun onClose(onClose: (password: String) -> Unit): AdvancedDialog {
        this.onClose = onClose
        return this
    }

    fun show(
        disableSSL: Boolean,
        caCertPath: String? = null,
        caCertCN: String?,
        clientCertPath: String? = null,
        clientCertPassword: String?
    ): AdvancedDialog {
        binding = AdvancedSettingsDialogBinding.inflate(layoutInflater)
        binding.disableSSL.isChecked = disableSSL
        binding.disableSSL.setOnCheckedChangeListener(onCheckedChangeListener)
        if (!clientCertPassword.isNullOrEmpty()) {
            binding.clientCertPasswordEdittext.setText(clientCertPassword)
        }
        binding.clientCertPasswordEdittext.doOnTextChanged { _, _, _, _ ->
            if (binding.selectedClientCert.text.toString() ==
                context.getString(R.string.certificate_found)
            ) {
                showPasswordMissing(binding.clientCertPasswordEdittext.text.toString().isEmpty())
            }
        }
        if (caCertPath == null) {
            showSelectCaCertificate()
        } else {
            showRemoveCaCertificate(caCertCN!!)
        }
        if (clientCertPath == null) {
            showSelectClientCertificate()
        } else {
            showRemoveClientCertificate()
        }
        MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setTitle(R.string.advanced_settings)
            .setPositiveButton(context.getString(R.string.done), null)
            .setOnDismissListener {
                onClose(binding.clientCertPasswordEdittext.text.toString())
            }
            .show()
        return this
    }

    private fun showSelectCaCertificate() {
        binding.toggleCaCert.setText(R.string.select_ca_certificate)
        binding.toggleCaCert.setOnClickListener { onClickSelectCaCertificate.run() }
        binding.selectedCaCert.setText(R.string.no_certificate_selected)
    }

    fun showRemoveCaCertificate(certificateCN: String) {
        binding.toggleCaCert.setText(R.string.remove_ca_certificate)
        binding.toggleCaCert.setOnClickListener {
            showSelectCaCertificate()
            onClickRemoveCaCertificate.run()
        }
        binding.selectedCaCert.text = certificateCN
    }

    private fun showSelectClientCertificate() {
        binding.toggleClientCert.setText(R.string.select_client_certificate)
        binding.toggleClientCert.setOnClickListener { onClickSelectClientCertificate.run() }
        binding.selectedClientCert.setText(R.string.no_certificate_selected)
        showPasswordMissing(false)
        binding.clientCertPasswordEdittext.text = null
    }

    fun showRemoveClientCertificate() {
        binding.toggleClientCert.setText(R.string.remove_client_certificate)
        binding.toggleClientCert.setOnClickListener {
            showSelectClientCertificate()
            onClickRemoveClientCertificate.run()
        }
        binding.selectedClientCert.setText(R.string.certificate_found)
        showPasswordMissing(binding.clientCertPasswordEdittext.text.toString().isEmpty())
    }

    private fun showPasswordMissing(toggled: Boolean) {
        val error = if (toggled) {
            context.getString(R.string.client_cert_password_missing)
        } else {
            null
        }
        binding.clientCertPassword.error = error
    }
}
