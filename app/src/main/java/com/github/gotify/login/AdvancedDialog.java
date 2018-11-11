package com.github.gotify.login;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.github.gotify.R;

class AdvancedDialog {

    private Context context;
    private ViewHolder holder;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    private Runnable onClickSelectCaCertificate;
    private Runnable onClickRemoveCaCertificate;

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

    AdvancedDialog show(boolean disableSSL, @Nullable String selectedCertificate) {

        View dialogView =
                LayoutInflater.from(context).inflate(R.layout.advanced_settings_dialog, null);
        holder = new ViewHolder(dialogView);
        holder.disableSSL.setChecked(disableSSL);
        holder.disableSSL.setOnCheckedChangeListener(onCheckedChangeListener);

        if (selectedCertificate == null) {
            showSelectCACertificate();
        } else {
            showRemoveCACertificate(selectedCertificate);
        }

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle(R.string.advanced_settings)
                .setPositiveButton(context.getString(R.string.done), (ignored, ignored2) -> {})
                .show();
        return this;
    }

    private void showSelectCACertificate() {
        holder.toggleCaCert.setText(R.string.select_ca_certificate);
        holder.toggleCaCert.setOnClickListener((a) -> onClickSelectCaCertificate.run());
        holder.selectedCaCertificate.setText(R.string.no_certificate_selected);
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

    class ViewHolder {
        @BindView(R.id.disableSSL)
        CheckBox disableSSL;

        @BindView(R.id.toggle_ca_cert)
        Button toggleCaCert;

        @BindView(R.id.seleceted_ca_cert)
        TextView selectedCaCertificate;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
