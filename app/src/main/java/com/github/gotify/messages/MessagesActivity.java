package com.github.gotify.messages;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewFlipper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.gotify.BuildConfig;
import com.github.gotify.MissedMessageUtil;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.api.Api;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.Callback;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.ApplicationApi;
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
import com.github.gotify.messages.provider.MessageDeletion;
import com.github.gotify.messages.provider.MessageFacade;
import com.github.gotify.messages.provider.MessageState;
import com.github.gotify.messages.provider.MessageWithImage;
import com.github.gotify.service.WebSocketService;
import com.github.gotify.settings.SettingsActivity;
import com.github.gotify.sharing.ShareActivity;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Target;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.github.gotify.Utils.first;
import static java.util.Collections.emptyList;

public class MessagesActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final BroadcastReceiver receiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String messageJson = intent.getStringExtra("message");
                    Message message = Utils.JSON.fromJson(messageJson, Message.class);
                    new NewSingleMessage().execute(message);
                }
            };

    private static final int APPLICATION_ORDER = 1;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.messages_view)
    RecyclerView messagesView;

    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.flipper)
    ViewFlipper flipper;

    private MessagesModel viewModel;

    private boolean isLoadMore = false;
    private Long updateAppOnDrawerClose = null;

    private ListMessageAdapter listMessageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        ButterKnife.bind(this);
        viewModel =
                new ViewModelProvider(this, new MessagesModelFactory(this))
                        .get(MessagesModel.class);
        Log.i("Entering " + getClass().getSimpleName());

        initDrawer();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(
                        messagesView.getContext(), layoutManager.getOrientation());
        listMessageAdapter =
                new ListMessageAdapter(
                        this,
                        viewModel.getSettings(),
                        viewModel.getPicassoHandler().get(),
                        emptyList(),
                        this::scheduleDeletion);

        messagesView.addItemDecoration(dividerItemDecoration);
        messagesView.setHasFixedSize(true);
        messagesView.setLayoutManager(layoutManager);
        messagesView.addOnScrollListener(new MessageListOnScrollListener());
        messagesView.setAdapter(listMessageAdapter);

        ApplicationHolder appsHolder = viewModel.getAppsHolder();
        appsHolder.onUpdate(() -> onUpdateApps(appsHolder.get()));
        if (appsHolder.wasRequested()) onUpdateApps(appsHolder.get());
        else appsHolder.request();

        ItemTouchHelper itemTouchHelper =
                new ItemTouchHelper(new SwipeToDeleteCallback(listMessageAdapter));
        itemTouchHelper.attachToRecyclerView(messagesView);

        swipeRefreshLayout.setOnRefreshListener(this::onRefresh);
        drawer.addDrawerListener(
                new DrawerLayout.SimpleDrawerListener() {
                    @Override
                    public void onDrawerClosed(View drawerView) {
                        if (updateAppOnDrawerClose != null) {
                            viewModel.setAppId(updateAppOnDrawerClose);
                            new UpdateMessagesForApplication(true).execute(updateAppOnDrawerClose);
                            updateAppOnDrawerClose = null;
                            invalidateOptionsMenu();
                        }
                    }
                });

        swipeRefreshLayout.setEnabled(false);
        messagesView
                .getViewTreeObserver()
                .addOnScrollChangedListener(
                        () -> {
                            View topChild = messagesView.getChildAt(0);
                            if (topChild != null) {
                                swipeRefreshLayout.setEnabled(topChild.getTop() == 0);
                            } else {
                                swipeRefreshLayout.setEnabled(true);
                            }
                        });

        new UpdateMessagesForApplication(true).execute(viewModel.getAppId());
    }

    public void onRefreshAll(View view) {
        refreshAll();
    }

    public void refreshAll() {
        try {
            viewModel.getPicassoHandler().evict();
        } catch (IOException e) {
            Log.e("Problem evicting Picasso cache", e);
        }
        startActivity(new Intent(this, InitializationActivity.class));
        finish();
    }

    private void onRefresh() {
        viewModel.getMessages().clear();
        new LoadMore().execute(viewModel.getAppId());
    }

    @OnClick(R.id.learn_gotify)
    public void openDocumentation() {
        Intent browserIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://gotify.net/docs/pushmsg"));
        startActivity(browserIntent);
    }

    public void commitDelete() {
        new CommitDeleteMessage().execute();
    }

    protected void onUpdateApps(List<Application> applications) {
        Menu menu = navigationView.getMenu();
        menu.removeGroup(R.id.apps);
        viewModel.getTargetReferences().clear();
        updateMessagesAndStopLoading(viewModel.getMessages().get(viewModel.getAppId()));

        MenuItem selectedItem = menu.findItem(R.id.nav_all_messages);
        for (int i = 0; i < applications.size(); i++) {
            Application app = applications.get(i);
            MenuItem item = menu.add(R.id.apps, i, APPLICATION_ORDER, app.getName());
            item.setCheckable(true);
            if (app.getId() == viewModel.getAppId()) selectedItem = item;
            Target t = Utils.toDrawable(getResources(), item::setIcon);
            viewModel.getTargetReferences().add(t);
            viewModel
                    .getPicassoHandler()
                    .get()
                    .load(
                            Utils.resolveAbsoluteUrl(
                                    viewModel.getSettings().url() + "/", app.getImage()))
                    .error(R.drawable.ic_alarm)
                    .placeholder(R.drawable.ic_placeholder)
                    .resize(100, 100)
                    .into(t);
        }
        selectAppInMenu(selectedItem);
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

        Settings settings = viewModel.getSettings();

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

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (item.getGroupId() == R.id.apps) {
            Application app = viewModel.getAppsHolder().get().get(id);
            updateAppOnDrawerClose = app != null ? app.getId() : MessageState.ALL_MESSAGES;
            startLoading();
            toolbar.setSubtitle(item.getTitle());
        } else if (id == R.id.nav_all_messages) {
            updateAppOnDrawerClose = MessageState.ALL_MESSAGES;
            startLoading();
            toolbar.setSubtitle("");
        } else if (id == R.id.logout) {
            new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_Dialog))
                    .setTitle(R.string.logout)
                    .setMessage(getString(R.string.logout_confirm))
                    .setPositiveButton(R.string.yes, this::doLogout)
                    .setNegativeButton(R.string.cancel, (a, b) -> {})
                    .show();
        } else if (id == R.id.nav_logs) {
            startActivity(new Intent(this, LogsActivity.class));
        } else if (id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.push_message) {
            Intent intent = new Intent(MessagesActivity.this, ShareActivity.class);
            startActivity(intent);
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

        Context context = getApplicationContext();
        NotificationManager nManager =
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        nManager.cancelAll();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WebSocketService.NEW_MESSAGE_BROADCAST);
        registerReceiver(receiver, filter);
        new UpdateMissedMessages().execute(viewModel.getMessages().getLastReceivedMessage());

        int selectedIndex = R.id.nav_all_messages;
        long appId = viewModel.getAppId();
        if (appId != MessageState.ALL_MESSAGES) {
            List<Application> apps = viewModel.getAppsHolder().get();
            for (int i = 0; i < apps.size(); i++) {
                if (apps.get(i).getId() == appId) {
                    selectedIndex = i;
                }
            }
        }

        listMessageAdapter.notifyDataSetChanged();
        selectAppInMenu(navigationView.getMenu().findItem(selectedIndex));
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    private void selectAppInMenu(MenuItem appItem) {
        if (appItem != null) {
            appItem.setChecked(true);
            if (appItem.getItemId() != R.id.nav_all_messages)
                toolbar.setSubtitle(appItem.getTitle());
        }
    }

    private void scheduleDeletion(int position, Message message, boolean listAnimation) {
        ListMessageAdapter adapter = (ListMessageAdapter) messagesView.getAdapter();

        MessageFacade messages = viewModel.getMessages();
        messages.deleteLocal(message);
        adapter.setItems(messages.get(viewModel.getAppId()));

        if (listAnimation) adapter.notifyItemRemoved(position);
        else adapter.notifyDataSetChanged();

        showDeletionSnackbar();
    }

    private void undoDelete() {
        MessageFacade messages = viewModel.getMessages();
        MessageDeletion deletion = messages.undoDeleteLocal();
        if (deletion != null) {
            ListMessageAdapter adapter = (ListMessageAdapter) messagesView.getAdapter();
            long appId = viewModel.getAppId();
            adapter.setItems(messages.get(appId));
            int insertPosition =
                    appId == MessageState.ALL_MESSAGES
                            ? deletion.getAllPosition()
                            : deletion.getAppPosition();
            adapter.notifyItemInserted(insertPosition);
        }
    }

    private void showDeletionSnackbar() {
        View view = swipeRefreshLayout;
        Snackbar snackbar = Snackbar.make(view, R.string.snackbar_deleted, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.snackbar_undo, v -> undoDelete());
        snackbar.addCallback(new SnackbarCallback());
        snackbar.show();
    }

    private class SnackbarCallback extends BaseTransientBottomBar.BaseCallback<Snackbar> {
        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            super.onDismissed(transientBottomBar, event);
            if (event != DISMISS_EVENT_ACTION && event != DISMISS_EVENT_CONSECUTIVE) {
                // Execute deletion when the snackbar disappeared without pressing the undo button
                // DISMISS_EVENT_CONSECUTIVE should be excluded as well, because it would cause the
                // deletion to be sent to the server twice, since the deletion is sent to the server
                // in MessageFacade if a message is deleted while another message was already
                // waiting for deletion.
                MessagesActivity.this.commitDelete();
            }
        }
    }

    private class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private final ListMessageAdapter adapter;
        private Drawable icon;
        private final ColorDrawable background;

        public SwipeToDeleteCallback(ListMessageAdapter adapter) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            this.adapter = adapter;

            int backgroundColorId =
                    ContextCompat.getColor(MessagesActivity.this, R.color.swipeBackground);
            int iconColorId = ContextCompat.getColor(MessagesActivity.this, R.color.swipeIcon);

            Drawable drawable =
                    ContextCompat.getDrawable(MessagesActivity.this, R.drawable.ic_delete);
            icon = null;
            if (drawable != null) {
                icon = DrawableCompat.wrap(drawable.mutate());
                DrawableCompat.setTint(icon, iconColorId);
            }

            background = new ColorDrawable(backgroundColorId);
        }

        @Override
        public boolean onMove(
                @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            MessageWithImage message = adapter.getItems().get(position);
            scheduleDeletion(position, message.message, true);
        }

        @Override
        public void onChildDraw(
                @NonNull Canvas c,
                @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                float dX,
                float dY,
                int actionState,
                boolean isCurrentlyActive) {
            if (icon != null) {
                View itemView = viewHolder.itemView;

                int iconHeight = itemView.getHeight() / 3;
                double scale = iconHeight / (double) icon.getIntrinsicHeight();
                int iconWidth = (int) (icon.getIntrinsicWidth() * scale);

                int iconMarginLeftRight = 50;
                int iconMarginTopBottom = (itemView.getHeight() - iconHeight) / 2;
                int iconTop = itemView.getTop() + iconMarginTopBottom;
                int iconBottom = itemView.getBottom() - iconMarginTopBottom;

                if (dX > 0) {
                    // Swiping to the right
                    int iconLeft = itemView.getLeft() + iconMarginLeftRight;
                    int iconRight = itemView.getLeft() + iconMarginLeftRight + iconWidth;
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                    background.setBounds(
                            itemView.getLeft(),
                            itemView.getTop(),
                            itemView.getLeft() + ((int) dX),
                            itemView.getBottom());
                } else if (dX < 0) {
                    // Swiping to the left
                    int iconLeft = itemView.getRight() - iconMarginLeftRight - iconWidth;
                    int iconRight = itemView.getRight() - iconMarginLeftRight;
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                    background.setBounds(
                            itemView.getRight() + ((int) dX),
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom());
                } else {
                    // View is unswiped
                    icon.setBounds(0, 0, 0, 0);
                    background.setBounds(0, 0, 0, 0);
                }

                background.draw(c);
                icon.draw(c);
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    private class MessageListOnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView view, int scrollState) {}

        @Override
        public void onScrolled(RecyclerView view, int dx, int dy) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) view.getLayoutManager();
            if (linearLayoutManager != null) {
                int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                int totalItemCount = view.getAdapter().getItemCount();

                if (lastVisibleItem > totalItemCount - 15
                        && totalItemCount != 0
                        && viewModel.getMessages().canLoadMore(viewModel.getAppId())) {
                    if (!isLoadMore) {
                        isLoadMore = true;
                        new LoadMore().execute(viewModel.getAppId());
                    }
                }
            }
        }
    }

    private class UpdateMissedMessages extends AsyncTask<Long, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Long... ids) {
            Long id = first(ids);
            if (id == -1) {
                return false;
            }

            List<Message> newMessages =
                    new MissedMessageUtil(viewModel.getClient().createService(MessageApi.class))
                            .missingMessages(id);
            viewModel.getMessages().addMessages(newMessages);
            return !newMessages.isEmpty();
        }

        @Override
        protected void onPostExecute(Boolean update) {
            if (update) {
                new UpdateMessagesForApplication(true).execute(viewModel.getAppId());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.messages_action, menu);
        menu.findItem(R.id.action_delete_app)
                .setVisible(viewModel.getAppId() != MessageState.ALL_MESSAGES);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            new DeleteMessages().execute(viewModel.getAppId());
        }
        if (item.getItemId() == R.id.action_delete_app) {
            android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
            alert.setTitle(R.string.delete_app);
            alert.setMessage(R.string.ack);
            alert.setPositiveButton(
                    R.string.yes, (dialog, which) -> deleteApp(viewModel.getAppId()));
            alert.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
            alert.show();
        }
        return super.onContextItemSelected(item);
    }

    private void deleteApp(Long appId) {
        Settings settings = viewModel.getSettings();
        ApiClient client =
                ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token());

        client.createService(ApplicationApi.class)
                .deleteApp(appId)
                .enqueue(
                        Callback.callInUI(
                                this,
                                (ignored) -> refreshAll(),
                                (e) ->
                                        Utils.showSnackBar(
                                                this, getString(R.string.error_delete_app))));
    }

    private class LoadMore extends AsyncTask<Long, Void, List<MessageWithImage>> {

        @Override
        protected List<MessageWithImage> doInBackground(Long... appId) {
            return viewModel.getMessages().loadMore(first(appId));
        }

        @Override
        protected void onPostExecute(List<MessageWithImage> messageWithImages) {
            updateMessagesAndStopLoading(messageWithImages);
        }
    }

    private class UpdateMessagesForApplication extends AsyncTask<Long, Void, Long> {

        private UpdateMessagesForApplication(boolean withLoadingSpinner) {
            if (withLoadingSpinner) {
                startLoading();
            }
        }

        @Override
        protected Long doInBackground(Long... appIds) {
            Long appId = first(appIds);
            viewModel.getMessages().loadMoreIfNotPresent(appId);
            return appId;
        }

        @Override
        protected void onPostExecute(Long appId) {
            updateMessagesAndStopLoading(viewModel.getMessages().get(appId));
        }
    }

    private class NewSingleMessage extends AsyncTask<Message, Void, Void> {

        @Override
        protected Void doInBackground(Message... newMessages) {
            viewModel.getMessages().addMessages(Arrays.asList(newMessages));
            return null;
        }

        @Override
        protected void onPostExecute(Void data) {
            new UpdateMessagesForApplication(false).execute(viewModel.getAppId());
        }
    }

    private class CommitDeleteMessage extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... messages) {
            viewModel.getMessages().commitDelete();
            return null;
        }

        @Override
        protected void onPostExecute(Void data) {
            new UpdateMessagesForApplication(false).execute(viewModel.getAppId());
        }
    }

    private class DeleteMessages extends AsyncTask<Long, Void, Boolean> {

        DeleteMessages() {
            startLoading();
        }

        @Override
        protected Boolean doInBackground(Long... appId) {
            return viewModel.getMessages().deleteAll(first(appId));
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Utils.showSnackBar(MessagesActivity.this, "Delete failed :(");
            }
            new UpdateMessagesForApplication(false).execute(viewModel.getAppId());
        }
    }

    private class DeleteClientAndNavigateToLogin extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... ignore) {
            Settings settings = viewModel.getSettings();
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
                    Api.execute(api.deleteClient(currentClient.getId()));
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
            viewModel.getSettings().clear();
            startActivity(new Intent(MessagesActivity.this, LoginActivity.class));
            finish();
            super.onPostExecute(aVoid);
        }
    }

    private void updateMessagesAndStopLoading(List<MessageWithImage> messageWithImages) {
        isLoadMore = false;
        stopLoading();

        if (messageWithImages.isEmpty()) {
            flipper.setDisplayedChild(1);
        } else {
            flipper.setDisplayedChild(0);
        }

        ListMessageAdapter adapter = (ListMessageAdapter) messagesView.getAdapter();
        adapter.setItems(messageWithImages);
        adapter.notifyDataSetChanged();
    }
}
