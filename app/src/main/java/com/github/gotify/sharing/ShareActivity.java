package com.github.gotify.sharing;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.api.Api;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.ApplicationApi;
import com.github.gotify.client.api.MessageApi;
import com.github.gotify.client.model.Application;
import com.github.gotify.client.model.Message;
import com.github.gotify.log.Log;
import java.util.List;

import static com.github.gotify.Utils.first;

public class ShareActivity extends Activity {
    private ApiClient client;
    private Settings settings;
    private ShareActivity shareActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shareActivity = this;
        settings = new Settings(this);
        handleShareIntent();
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

                    client =
                            ClientFactory.clientToken(
                                    settings.url(), settings.sslSettings(), settings.token());

                    new GetApps().execute(message);
                }
            }
        }
    }

    private class GetApps extends AsyncTask<Message, Void, Void> {
        @Override
        protected Void doInBackground(Message... messages) {
            try {
                ApplicationApi applicationApi = client.createService(ApplicationApi.class);
                List<Application> apps = Api.execute(applicationApi.getApps());
                client =
                        ClientFactory.clientToken(
                                settings.url(), settings.sslSettings(), apps.get(0).getToken());
                new SendSharedContent().execute(first(messages));
            } catch (ApiException apiException) {
                Log.e("Failed getting apps", apiException);
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
            Utils.showLongToast(shareActivity, message);
            shareActivity.finish();
        }
    }
}
