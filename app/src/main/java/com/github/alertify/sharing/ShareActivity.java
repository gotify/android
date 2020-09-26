package com.github.alertify.sharing;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.alertify.R;
import com.github.alertify.Settings;
import com.github.alertify.api.Api;
import com.github.alertify.api.ApiException;
import com.github.alertify.api.ClientFactory;
import com.github.alertify.client.ApiClient;
import com.github.alertify.client.api.MessageApi;
import com.github.alertify.client.model.Application;
import com.github.alertify.client.model.Message;
import com.github.alertify.log.Log;
import com.github.alertify.init.messages.provider.ApplicationHolder;
import java.util.ArrayList;
import java.util.List;

import static com.github.alertify.Utils.first;

public class ShareActivity extends AppCompatActivity {
    private Settings settings;
    private ApplicationHolder appsHolder;

    @BindView(R.id.title)
    EditText edtTxtTitle;

    @BindView(R.id.content)
    EditText edtTxtContent;

    @BindView(R.id.edtTxtPriority)
    EditText edtTxtPriority;

    @BindView(R.id.appSpinner)
    Spinner appSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        ButterKnife.bind(this);

        Log.i("Entering " + getClass().getSimpleName());
        setSupportActionBar(findViewById(R.id.toolbar));
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
                edtTxtContent.setText(sharedText);
            }
        }

        ApiClient client =
                ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token());
        appsHolder = new ApplicationHolder(this, client);
        appsHolder.onUpdate(() -> populateSpinner(appsHolder.get()));
        appsHolder.request();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.push_button)
    public void pushMessage(View view) {
        String titleText = edtTxtTitle.getText().toString();
        String contentText = edtTxtContent.getText().toString();
        String priority = edtTxtPriority.getText().toString();
        int appIndex = appSpinner.getSelectedItemPosition();

        if (contentText.isEmpty()) {
            Toast.makeText(this, "Content should not be empty.", Toast.LENGTH_LONG).show();
            return;
        } else if (priority.isEmpty()) {
            Toast.makeText(this, "Priority should be number.", Toast.LENGTH_LONG).show();
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
        appSpinner.setAdapter(adapter);
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
