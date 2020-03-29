package com.github.gotify.log;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.github.gotify.R;
import com.github.gotify.Utils;

public class LogsActivity extends AppCompatActivity {

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        Log.i("Entering " + getClass().getSimpleName());
        updateLogs();
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }
    }

    private void updateLogs() {
        new RefreshLogs().execute();
        if (!isDestroyed()) {
            handler.postDelayed(this::updateLogs, 5000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logs_action, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        if (item.getItemId() == R.id.action_delete_logs) {
            Log.clear();
        }
        if (item.getItemId() == R.id.action_copy_logs) {
            TextView content = findViewById(R.id.log_content);
            ClipboardManager clipboardManager =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("GotifyLog", content.getText().toString());
            clipboardManager.setPrimaryClip(clipData);
            Utils.showSnackBar(this, getString(R.string.logs_copied));
        }
        return super.onOptionsItemSelected(item);
    }

    class RefreshLogs extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            return com.github.gotify.log.Log.get();
        }

        @Override
        protected void onPostExecute(String s) {
            TextView content = findViewById(R.id.log_content);
            if (content.getSelectionStart() == content.getSelectionEnd()) {
                content.setText(s);
            }
            super.onPostExecute(s);
        }
    }
}
