package com.github.gotify.sharing;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.api.Api;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.MessageApi;
import com.github.gotify.client.model.Message;
import com.github.gotify.log.Log;

import static com.github.gotify.Utils.first;

public class ShareActivity extends AppCompatActivity {
    private ApiClient client;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        Log.i("Entering " + getClass().getSimpleName());
        //updateLogs();
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }
        actionBar.setSubtitle("Test");

        settings = new Settings(this);
        handleShareIntent();


        actionBar.setSubtitle("Test");


        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText edtTxtTitle = (EditText)findViewById(R.id.title);
                String title = edtTxtTitle.getText().toString();

                EditText edtTxtContent = (EditText)findViewById(R.id.content);
                String content = edtTxtContent.getText().toString();

                Message message = new Message();
                message.setMessage(content);
                message.setTitle(title);
                message.setPriority((long) 5);
                new GetApps().execute(message);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleShareIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    Message message = new Message();
                    message.setMessage(sharedText);
                    message.setTitle("Shared content");
                    message.setPriority((long) 5);

                    //client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token());
                    client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), "APS0aY0WNaPsTJU");
                    new GetApps().execute(message);
                }
            }
        }
    }

    private class GetApps extends AsyncTask<Message, Void, Void> {
        @Override
        protected Void doInBackground(Message... messages) {
            try {
                //ApplicationApi applicationApi = client.createService(ApplicationApi.class);
                //List<Application> apps = Api.execute(applicationApi.getApps());
                //client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), apps.get(0).getToken());
                client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), "APS0aY0WNaPsTJU");
                new SendSharedContent().execute(first(messages));
            }
            //catch (ApiException apiException) {
            catch (Exception e) {
                //Log.e("Failed getting apps", apiException);
                Log.e("Failed getting apps", e);
            }

            return null;
        }
    }

    private class SendSharedContent extends AsyncTask<Message, String, String> {
        @Override
        protected String doInBackground(Message... messages) {
            try {
                MessageApi messageApi = client.createService(MessageApi.class);
                Api.execute(messageApi.createMessage(first(messages)));
                return "Pushed!";
            } catch (ApiException apiException) {
                Log.e("Failed sending message", apiException);
                return "Oops! Something went wrong...";
            }
        }

        @Override
        protected void onPostExecute(String message) {
            Utils.showLongToast(ShareActivity.this, message);
            ShareActivity.this.finish();
        }
    }
}

