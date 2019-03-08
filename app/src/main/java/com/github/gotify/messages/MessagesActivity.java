package com.github.gotify.messages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.github.gotify.BuildConfig;
import com.github.gotify.MissedMessageUtil;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.api.Api;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.CertUtils;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.ClientApi;
import com.github.gotify.client.api.MessageApi;
import com.github.gotify.client.model.Application;
import com.github.gotify.client.model.Client;
import com.github.gotify.client.model.Message;
import com.github.gotify.init.InitializationActivity;
import com.github.gotify.log.Log;
import com.github.gotify.log.LogsActivity;
import com.github.gotify.login.LoginActivity;
import com.github.gotify.messages.provider.ApplicationHolder;
import com.github.gotify.messages.provider.MessageFacade;
import com.github.gotify.messages.provider.MessageState;
import com.github.gotify.messages.provider.MessageWithImage;
import com.github.gotify.service.WebSocketService;
import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.OkHttpClient;

import static java.util.Collections.emptyList;

public class MessagesActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AbsListView.OnScrollListener {

    private BroadcastReceiver receiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String messageJson = intent.getStringExtra("message");
                    Message message = Utils.JSON.fromJson(messageJson, Message.class);
                    new NewSingleMessage().execute(message);
                }
            };

    private int APPLICATION_ORDER = 1;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.messages_view)
    ListView messagesView;

    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;

    private MessageFacade messages;

    private ApiClient client;
    private Settings settings;
    protected ApplicationHolder appsHolder;

    private int appId = MessageState.ALL_MESSAGES;

    private boolean isLoadMore = false;
    private Integer selectAppIdOnDrawerClose = null;

    private Picasso picasso;

    // we need to keep the target references otherwise they get gc'ed before they can be called.
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Target> targetReferences = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        ButterKnife.bind(this);
        Log.i("Entering " + getClass().getSimpleName());
        settings = new Settings(this);

        picasso = makePicasso();

        client =
                ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token());
        appsHolder = new ApplicationHolder(this, client);
        appsHolder.onUpdate(() -> onUpdateApps(appsHolder.get()));
        appsHolder.request();
        initDrawer();

        messages = new MessageFacade(client.createService(MessageApi.class), appsHolder);

        messagesView.setOnScrollListener(this);
        messagesView.setAdapter(
                new ListMessageAdapter(this, settings, picasso, emptyList(), this::delete));

        swipeRefreshLayout.setOnRefreshListener(this::onRefresh);
        drawer.addDrawerListener(
                new DrawerLayout.SimpleDrawerListener() {
                    @Override
                    public void onDrawerClosed(View drawerView) {
                        if (selectAppIdOnDrawerClose != null) {
                            appId = selectAppIdOnDrawerClose;
                            new SelectApplicationAndUpdateMessages(true)
                                    .execute(selectAppIdOnDrawerClose);
                            selectAppIdOnDrawerClose = null;
                        }
                    }
                });
        new SelectApplicationAndUpdateMessages(true).execute(appId);
    }

    public void onRefreshAll(View view) {
        startActivity(new Intent(this, InitializationActivity.class));
        finish();
    }

    private void onRefresh() {
        messages.clear();
        new LoadMore().execute(appId);
    }

    public void delete(Message message) {
        new DeleteMessage().execute(message);
    }

    protected void onUpdateApps(List<Application> applications) {
        Menu menu = navigationView.getMenu();
        menu.removeGroup(R.id.apps);
        targetReferences.clear();
        updateMessagesAndStopLoading(messages.get(appId));
        for (Application app : applications) {
            MenuItem item = menu.add(R.id.apps, app.getId(), APPLICATION_ORDER, app.getName());
            item.setCheckable(true);
            Target t = Utils.toDrawable(getResources(), item::setIcon);
            targetReferences.add(t);
            picasso.load(Utils.resolveAbsoluteUrl(settings.url() + "/", app.getImage()))
                    .error(R.drawable.ic_alarm)
                    .placeholder(R.drawable.ic_placeholder)
                    .resize(100, 100)
                    .into(t);
        }
    }

    private Picasso makePicasso() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        CertUtils.applySslSettings(builder, settings.sslSettings());

        OkHttp3Downloader downloader = new OkHttp3Downloader(builder.build());

        return new Picasso.Builder(this).downloader(downloader).build();
    }

    private void initDrawer() {
        setSupportActionBar(toolbar);
        navigationView.setItemIconTintList(null);
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(
                        this,
                        drawer,
                        toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);

        TextView user = headerView.findViewById(R.id.header_user);
        user.setText(settings.user().getName());

        TextView connection = headerView.findViewById(R.id.header_connection);
        connection.setText(
                getString(R.string.connection, settings.user().getName(), settings.url()));

        TextView version = headerView.findViewById(R.id.header_version);
        version.setText(
                getString(R.string.versions, BuildConfig.VERSION_NAME, settings.serverVersion()));

        ImageButton refreshAll = headerView.findViewById(R.id.refresh_all);
        refreshAll.setOnClickListener(this::onRefreshAll);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (item.getGroupId() == R.id.apps) {
            selectAppIdOnDrawerClose = id;
            startLoading();
            toolbar.setSubtitle(item.getTitle());
        } else if (id == R.id.nav_all_messages) {
            selectAppIdOnDrawerClose = MessageState.ALL_MESSAGES;
            startLoading();
            toolbar.setSubtitle("");
        } else if (id == R.id.logout) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.logout)
                    .setMessage(getString(R.string.logout_confirm))
                    .setPositiveButton(R.string.yes, this::doLogout)
                    .setNegativeButton(R.string.cancel, (a, b) -> {})
                    .show();
        } else if (id == R.id.nav_logs) {
            startActivity(new Intent(this, LogsActivity.class));
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void doLogout(DialogInterface dialog, int which) {
        setContentView(R.layout.splash);
        new DeleteClientAndNavigateToLogin().execute();
    }

    private void startLoading() {
        swipeRefreshLayout.setRefreshing(true);
        messagesView.setVisibility(View.GONE);
    }

    private void stopLoading() {
        swipeRefreshLayout.setRefreshing(false);
        messagesView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WebSocketService.NEW_MESSAGE_BROADCAST);
        registerReceiver(receiver, filter);
        new UpdateMissedMessages().execute(messages.getLastReceivedMessage());
        navigationView
                .getMenu()
                .findItem(appId == MessageState.ALL_MESSAGES ? R.id.nav_all_messages : appId)
                .setChecked(true);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        picasso.shutdown();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(
            AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem + visibleItemCount > totalItemCount - 15
                && totalItemCount != 0
                && messages.canLoadMore(appId)) {
            if (!isLoadMore) {
                isLoadMore = true;
                new LoadMore().execute(appId);
            }
        }
    }

    private class UpdateMissedMessages extends AsyncTask<Integer, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Integer... ids) {
            Integer id = first(ids);
            if (id == -1) {
                return false;
            }

            List<Message> newMessages =
                    new MissedMessageUtil(client.createService(MessageApi.class))
                            .missingMessages(id);
            messages.addMessages(newMessages);
            return !newMessages.isEmpty();
        }

        @Override
        protected void onPostExecute(Boolean update) {
            if (update) {
                new SelectApplicationAndUpdateMessages(true).execute(appId);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.messages_action, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            new DeleteMessages().execute(appId);
        }
        return super.onContextItemSelected(item);
    }

    private class LoadMore extends AsyncTask<Integer, Void, List<MessageWithImage>> {

        @Override
        protected List<MessageWithImage> doInBackground(Integer... appId) {
            return messages.loadMore(first(appId));
        }

        @Override
        protected void onPostExecute(List<MessageWithImage> messageWithImages) {
            updateMessagesAndStopLoading(messageWithImages);
        }
    }

    private class SelectApplicationAndUpdateMessages extends AsyncTask<Integer, Void, Integer> {

        private SelectApplicationAndUpdateMessages(boolean withLoadingSpinner) {
            if (withLoadingSpinner) {
                startLoading();
            }
        }

        @Override
        protected Integer doInBackground(Integer... appIds) {
            Integer appId = first(appIds);
            messages.loadMoreIfNotPresent(appId);
            return appId;
        }

        @Override
        protected void onPostExecute(Integer appId) {
            updateMessagesAndStopLoading(messages.get(appId));
        }
    }

    private class NewSingleMessage extends AsyncTask<Message, Void, Void> {

        @Override
        protected Void doInBackground(Message... newMessages) {
            messages.addMessages(Arrays.asList(newMessages));
            return null;
        }

        @Override
        protected void onPostExecute(Void data) {
            new SelectApplicationAndUpdateMessages(false).execute(appId);
        }
    }

    private class DeleteMessage extends AsyncTask<Message, Void, Void> {

        @Override
        protected Void doInBackground(Message... messages) {
            MessagesActivity.this.messages.delete(first(messages));
            return null;
        }

        @Override
        protected void onPostExecute(Void data) {
            new SelectApplicationAndUpdateMessages(false).execute(appId);
        }
    }

    private class DeleteMessages extends AsyncTask<Integer, Void, Boolean> {

        DeleteMessages() {
            startLoading();
        }

        @Override
        protected Boolean doInBackground(Integer... appId) {
            return messages.deleteAll(first(appId));
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Utils.showSnackBar(MessagesActivity.this, "Delete failed :(");
            }
            new SelectApplicationAndUpdateMessages(false).execute(appId);
        }
    }

    private class DeleteClientAndNavigateToLogin extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... ignore) {
            ClientApi api =
                    ClientFactory.clientToken(
                                    settings.url(), settings.sslSettings(), settings.token())
                            .createService(ClientApi.class);
            stopService(new Intent(MessagesActivity.this, WebSocketService.class));
            try {
                List<Client> clients = Api.execute(api.getClients());

                Client currentClient = null;
                for (Client client : clients) {
                    if (client.getToken().equals(settings.token())) {
                        currentClient = client;
                        break;
                    }
                }

                if (currentClient != null) {
                    Log.i("Delete client with id " + currentClient.getId());
                    api.deleteClient(currentClient.getId());
                } else {
                    Log.e("Could not delete client, client does not exist.");
                }

            } catch (ApiException e) {
                Log.e("Could not delete client", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            settings.clear();
            startActivity(new Intent(MessagesActivity.this, LoginActivity.class));
            finish();
            super.onPostExecute(aVoid);
        }
    }

    private void updateMessagesAndStopLoading(List<MessageWithImage> messageWithImages) {
        isLoadMore = false;
        stopLoading();
        ListMessageAdapter adapter = (ListMessageAdapter) messagesView.getAdapter();
        adapter.items(messageWithImages);
        adapter.notifyDataSetChanged();
    }

    private <T> T first(T[] data) {
        if (data.length != 1) {
            throw new IllegalArgumentException("must be one element");
        }

        return data[0];
    }
}
