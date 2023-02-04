package com.github.gotify.login

import android.content.Context
import android.view.LayoutInflater
import android.widget.CompoundButton
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

    fun show(disableSSL: Boolean, selectedCertificate: String?): AdvancedDialog {
        binding = AdvancedSettingsDialogBinding.inflate(layoutInflater)
        binding.disableSSL.isChecked = disableSSL
        binding.disableSSL.setOnCheckedChangeListener(onCheckedChangeListener)
        if (selectedCertificate == null) {
            showSelectCACertificate()
        } else {
            showRemoveCACertificate(selectedCertificate)
        }
        MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setTitle(R.string.advanced_settings)
            .setPositiveButton(context.getString(R.string.done), null)
            .show()
        return this
    }

    private fun showSelectCACertificate() {
        binding.toggleCaCert.setText(R.string.select_ca_certificate)
        binding.toggleCaCert.setOnClickListener { onClickSelectCaCertificate.run() }
        binding.selecetedCaCert.setText(R.string.no_certificate_selected)
    }

    fun showRemoveCACertificate(certificate: String) {
        binding.toggleCaCert.setText(R.string.remove_ca_certificate)
        binding.toggleCaCert.setOnClickListener {
            showSelectCACertificate()
            onClickRemoveCaCertificate.run()
        }
        binding.selecetedCaCert.text = certificate
    }
}
