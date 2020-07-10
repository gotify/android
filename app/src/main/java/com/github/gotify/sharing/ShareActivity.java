package com.github.gotify.sharing;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.github.gotify.R;
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
import com.github.gotify.messages.provider.ApplicationHolder;

import java.util.ArrayList;
import java.util.List;

import static com.github.gotify.Utils.first;

public class ShareActivity extends AppCompatActivity {
    private ApiClient client;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        Log.i("Entering " + getClass().getSimpleName());
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }
        settings = new Settings(this);

        // App spinner
        client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token());
        ApplicationHolder appsHolder = new ApplicationHolder(ShareActivity.this, client);
        List<Application> apps = appsHolder.get();
        appsHolder.onUpdate(() -> PopulateSpinner(appsHolder.get()));
        appsHolder.request();

        // Push button
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Message message = new Message();

                EditText edtTxtTitle = (EditText)findViewById(R.id.title);
                String title = edtTxtTitle.getText().toString();
                message.setTitle(title);

                EditText edtTxtContent = (EditText)findViewById(R.id.content);
                String content = edtTxtContent.getText().toString();
                message.setMessage(content);

                Spinner appSpinner = (Spinner) findViewById(R.id.appSpinner);
                String selectedApp = appSpinner.getSelectedItem().toString();

                Spinner prioritySpinner = (Spinner) findViewById(R.id.prioritySpinner);
                Long priority = Long.parseLong(prioritySpinner.getSelectedItem().toString());
                message.setPriority((long) priority);

                new GetApps(selectedApp).execute(message);
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

    private void PopulateSpinner (List<Application> apps) {
        List<String> appNameList = new ArrayList();
        for (Application app : apps) {
            appNameList.add(app.getName());
        }
        Spinner dynamicSpinner = (Spinner) findViewById(R.id.appSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ShareActivity.this,
                android.R.layout.simple_spinner_dropdown_item, appNameList);
        dynamicSpinner.setAdapter(adapter);
        dynamicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Log.v("item", (String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        // Auto select app
        Integer selectedApp = getIntent().getIntExtra("SELECTED_APP", 0);
        dynamicSpinner.setSelection(selectedApp);
    }

    private class GetApps extends AsyncTask<Message, Void, Void> {
        private String SelectedApp;

        public GetApps(String passedData) {
            SelectedApp = passedData;
        }

        @SuppressLint("WrongThread")
        @Override
        protected Void doInBackground(Message... messages) {
            try {
                ApplicationApi applicationApi = client.createService(ApplicationApi.class);
                List<Application> apps = Api.execute(applicationApi.getApps());
                for(Application app : apps) {
                    if (app.getName().equals(SelectedApp)) {
                        client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), app.getToken());
                        new SendSharedContent().execute(first(messages));
                        break;
                    }
                }

            }
            catch (ApiException apiException) {
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
            Utils.showLongToast(ShareActivity.this, message);
            ShareActivity.this.finish();
        }
    }
}

