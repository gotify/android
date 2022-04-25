package com.github.gotify.login;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.github.gotify.R;
import com.github.gotify.Settings;

class AdvancedDialog {

    private Context context;
    private ViewHolder holder;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    private Runnable onClickSelectCaCertificate;
    private Runnable onClickRemoveCaCertificate;
    private Runnable onClickSelectClientCertificate;
    private Runnable onClickRemoveClientCertificate;

    AdvancedDialog(Context context) {
        this.context = context;
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

    AdvancedDialog onClickSelectClientCertificate(Runnable onClickSelectClientCertificate) {
        this.onClickSelectClientCertificate = onClickSelectClientCertificate;
        return this;
    }

    AdvancedDialog onClickRemoveClientCertificate(Runnable onClickRemoveClientCertificate) {
        this.onClickRemoveClientCertificate = onClickRemoveClientCertificate;
        return this;
    }

    AdvancedDialog show(boolean disableSSL, @Nullable String selectedCaCertificate, @Nullable String selectedClientCertificate, String password, Settings settings) {
        View dialogView =
                LayoutInflater.from(context).inflate(R.layout.advanced_settings_dialog, null);
        holder = new ViewHolder(dialogView);
        holder.disableSSL.setChecked(disableSSL);
        holder.disableSSL.setOnCheckedChangeListener(onCheckedChangeListener);
        holder.editClientCertPass.setText(password);

        if (selectedCaCertificate == null) {
            showSelectCACertificate();
        } else {
            showRemoveCACertificate(selectedCaCertificate);
        }

        if (selectedClientCertificate == null) {
            showSelectClientCertificate();
        } else {
            showRemoveClientCertificate(selectedClientCertificate);
        }

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle(R.string.advanced_settings)
                .setPositiveButton(context.getString(R.string.done), (ignored, ignored2) -> {
                    settings.clientCertPass(holder.editClientCertPass.getText().toString());
                })
                .show();
        return this;
    }

    private void showSelectCACertificate() {
        holder.toggleCaCert.setText(R.string.select_ca_certificate);
        holder.toggleCaCert.setOnClickListener((a) -> onClickSelectCaCertificate.run());
        holder.selectedCaCertificate.setText(R.string.no_ca_certificate_selected);
    }

    void showRemoveCACertificate(String certificate) {
        holder.toggleCaCert.setText(R.string.remove_ca_certificate);
        holder.toggleCaCert.setOnClickListener(
                (a) -> {
                    showSelectCACertificate();
                    onClickRemoveCaCertificate.run();
                });
        holder.selectedCaCertificate.setText(certificate);
    }

    private void showSelectClientCertificate() {
        holder.toggleClientCert.setText(R.string.select_client_certificate);
        holder.toggleClientCert.setOnClickListener((a) -> onClickSelectClientCertificate.run());
        holder.selectedClientCertificate.setText(R.string.no_client_certificate_selected);
    }

    void showRemoveClientCertificate(String certificate) {
        holder.toggleClientCert.setText(R.string.remove_client_certificate);
        holder.toggleClientCert.setOnClickListener(
                (a) -> {
                    showSelectClientCertificate();
                    onClickRemoveClientCertificate.run();
                });
        holder.selectedClientCertificate.setText(certificate);
    }

    class ViewHolder {
        @BindView(R.id.disableSSL)
        CheckBox disableSSL;

        @BindView(R.id.toggle_ca_cert)
        Button toggleCaCert;

        @BindView(R.id.selected_ca_cert)
        TextView selectedCaCertificate;

        @BindView(R.id.toggle_client_cert)
        Button toggleClientCert;

        @BindView(R.id.selected_client_cert)
        TextView selectedClientCertificate;

        @BindView(R.id.edit_client_cert_pass)
        EditText editClientCertPass;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
