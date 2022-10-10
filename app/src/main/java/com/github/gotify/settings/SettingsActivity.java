package com.github.gotify.settings;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.github.gotify.R;

public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.setting_key_theme).equals(key)) {
            ThemeHelper.setTheme(
                    this, sharedPreferences.getString(key, getString(R.string.theme_default)));
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ListPreference message_layout =
                    findPreference(getString(R.string.setting_key_message_layout));
            message_layout.setOnPreferenceChangeListener(
                    (ignored, ignored2) -> {
                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.setting_message_layout_dialog_title)
                                .setMessage(R.string.setting_message_layout_dialog_message)
                                .setPositiveButton(
                                        getString(R.string.setting_message_layout_dialog_button1),
                                        (ignored3, ignored4) -> {
                                            restartApp();
                                        })
                                .setNegativeButton(
                                        getString(R.string.setting_message_layout_dialog_button2),
                                        (ignore3, ignored4) -> {})
                                .show();
                        return true;
                    });
        }

        private void restartApp() {
            PackageManager packageManager = getContext().getPackageManager();
            String packageName = getContext().getPackageName();
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        }
    }
}
