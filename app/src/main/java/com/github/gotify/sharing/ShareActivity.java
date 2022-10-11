package com.github.gotify.sharing;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.api.Api;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.MessageApi;
import com.github.gotify.client.model.Application;
import com.github.gotify.client.model.Message;
import com.github.gotify.databinding.ActivityShareBinding;
import com.github.gotify.log.Log;
import com.github.gotify.messages.provider.ApplicationHolder;
import java.util.ArrayList;
import java.util.List;

import static com.github.gotify.Utils.first;

public class ShareActivity extends AppCompatActivity {
    private ActivityShareBinding binding;
    private Settings settings;
    private ApplicationHolder appsHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.i("Entering " + getClass().getSimpleName());
        setSupportActionBar(binding.appBarDrawer.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }
        settings = new Settings(this);

        Intent intent = getIntent();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                binding.content.setText(sharedText);
            }
        }

        if (!settings.tokenExists()) {
            Toast.makeText(getApplicationContext(), R.string.not_loggedin_share, Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        ApiClient client =
                ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token());
        appsHolder = new ApplicationHolder(this, client);
        appsHolder.onUpdate(
                () -> {
                    List<Application> apps = appsHolder.get();
                    populateSpinner(apps);

                    boolean appsAvailable = !apps.isEmpty();
                    binding.pushButton.setEnabled(appsAvailable);
                    binding.missingAppsContainer.setVisibility(
                            appsAvailable ? View.GONE : View.VISIBLE);
                });
        appsHolder.onUpdateFailed(() -> binding.pushButton.setEnabled(false));
        appsHolder.request();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        binding.pushButton.setOnClickListener(ignored -> pushMessage());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void pushMessage() {
        String titleText = binding.title.getText().toString();
        String contentText = binding.content.getText().toString();
        String priority = binding.edtTxtPriority.getText().toString();
        int appIndex = binding.appSpinner.getSelectedItemPosition();

        if (contentText.isEmpty()) {
            Toast.makeText(this, "Content should not be empty.", Toast.LENGTH_LONG).show();
            return;
        } else if (priority.isEmpty()) {
            Toast.makeText(this, "Priority should be number.", Toast.LENGTH_LONG).show();
            return;
        } else if (appIndex == Spinner.INVALID_POSITION) {
            // For safety, e.g. loading the apps needs too much time (maybe a timeout) and
            // the user tries to push without an app selected.
            Toast.makeText(this, "An app must be selected.", Toast.LENGTH_LONG).show();
            return;
        }

        Message message = new Message();
        if (!titleText.isEmpty()) {
            message.setTitle(titleText);
        }
        message.setMessage(contentText);
        message.setPriority(Long.parseLong(priority));
        new PushMessage(appsHolder.get().get(appIndex).getToken()).execute(message);
    }

    private void populateSpinner(List<Application> apps) {
        List<String> appNameList = new ArrayList<>();
        for (Application app : apps) {
            appNameList.add(app.getName());
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_dropdown_item, appNameList);
        binding.appSpinner.setAdapter(adapter);
    }

    private class PushMessage extends AsyncTask<Message, String, String> {
        private String token;

        public PushMessage(String token) {
            this.token = token;
        }

        @Override
        protected String doInBackground(Message... messages) {
            List<Application> apps = appsHolder.get();
            ApiClient pushClient =
                    ClientFactory.clientToken(settings.url(), settings.sslSettings(), token);

            try {
                MessageApi messageApi = pushClient.createService(MessageApi.class);
                Api.execute(messageApi.createMessage(first(messages)));
                return "Pushed!";
            } catch (ApiException apiException) {
                Log.e("Failed sending message", apiException);
                return "Oops! Something went wrong...";
            }
        }

        @Override
        protected void onPostExecute(String message) {
            Toast.makeText(ShareActivity.this, message, Toast.LENGTH_LONG).show();
            ShareActivity.this.finish();
        }
    }
}
