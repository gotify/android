package com.github.gotify.login;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import androidx.annotation.Nullable;
import com.github.gotify.R;
import com.github.gotify.databinding.AdvancedSettingsDialogBinding;

class AdvancedDialog {

    private Context context;
    private LayoutInflater layoutInflater;
    private AdvancedSettingsDialogBinding binding;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    private Runnable onClickSelectCaCertificate;
    private Runnable onClickRemoveCaCertificate;

    AdvancedDialog(Context context, LayoutInflater layoutInflater) {
        this.context = context;
        this.layoutInflater = layoutInflater;
    }

    AdvancedDialog onDisableSSLChanged(
            CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener;
        return this;
    }

    AdvancedDialog onClickSelectCaCertificate(Runnable onClickSelectCaCertificate) {
        this.onClickSelectCaCertificate = onClickSelectCaCertificate;
        return this;
    }

    AdvancedDialog onClickRemoveCaCertificate(Runnable onClickRemoveCaCertificate) {
        this.onClickRemoveCaCertificate = onClickRemoveCaCertificate;
        return this;
    }

    AdvancedDialog show(boolean disableSSL, @Nullable String selectedCertificate) {
        binding = AdvancedSettingsDialogBinding.inflate(layoutInflater);
        binding.disableSSL.setChecked(disableSSL);
        binding.disableSSL.setOnCheckedChangeListener(onCheckedChangeListener);

        if (selectedCertificate == null) {
            showSelectCACertificate();
        } else {
            showRemoveCACertificate(selectedCertificate);
        }

        new AlertDialog.Builder(context)
                .setView(binding.getRoot())
                .setTitle(R.string.advanced_settings)
                .setPositiveButton(context.getString(R.string.done), (ignored, ignored2) -> {})
                .show();
        return this;
    }

    private void showSelectCACertificate() {
        binding.toggleCaCert.setText(R.string.select_ca_certificate);
        binding.toggleCaCert.setOnClickListener((a) -> onClickSelectCaCertificate.run());
        binding.selecetedCaCert.setText(R.string.no_certificate_selected);
    }

    void showRemoveCACertificate(String certificate) {
        binding.toggleCaCert.setText(R.string.remove_ca_certificate);
        binding.toggleCaCert.setOnClickListener(
                (a) -> {
                    showSelectCACertificate();
                    onClickRemoveCaCertificate.run();
                });
        binding.selecetedCaCert.setText(certificate);
    }
}
