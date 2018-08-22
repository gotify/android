package com.github.gotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import com.github.gotify.model.Message;
import com.github.gotify.model.PagedMessages;
import com.github.gotify.model.Paging;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class PushService extends Service {
    private static final String TOKEN = "@global:token";
    private static final String URL = "@global:url";
    private static final List<String> UPDATE_ON_KEYS = Arrays.asList(TOKEN, URL);
    private static final int NO_MESSAGE = -1;

    private final Object socketLock = new Object();
    private final OkHttpClient client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(1, TimeUnit.MINUTES).connectTimeout(10, TimeUnit.SECONDS).build();
    private final AtomicLong lastError = new AtomicLong(0);
    private final AtomicInteger lastReceivedMessage = new AtomicInteger(NO_MESSAGE);
    private Handler handler = null;
    private WebSocket socket = null;
    private Gson gson = null;

    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (!UPDATE_ON_KEYS.contains(key)) {
                return;
            }
            synchronized (socketLock) {
                if (socket != null) {
                    Log.i("Closing WebSocket (preference change)");
                    socket.close(1000, "client logout");
                    socket = null;
                }
            }
            new Thread(pushService).start();
        }
    };

    private final Runnable pushService = new Runnable() {
        @Override
        public void run() {
            try {
                start(true);
            } catch (Exception e) {
                Log.e("Could not start service", e);
            }
        }
    };

    private final Runnable pushServiceAfterError = new Runnable() {
        @Override
        public void run() {
            start(false);
        }
    };

    private final Runnable pushServiceAfterErrorInNewThread = new Runnable() {
        @Override
        public void run() {
            new Thread(pushServiceAfterError).start();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        Log.i("Creating WebSocket-Service");

        gson = new Gson();
        handler = new Handler();
        new Thread(pushService).start();
        appPreferences().registerOnSharedPreferenceChangeListener(listener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OreoNotificationSupport.createChannel((NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE));
        }
    }

    private void foregroundNotification(String message) {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "GOTIFY_CHANNEL")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Gotify")
                .setChannelId(OreoNotificationSupport.CHANNEL_ID)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
    }

    private void ensureAllMessagesArePublished(boolean firstStart, String url, String token) {
        PagedMessages message = getMessages(url, token, 1, null);
        List<Message> messages = message.getMessages();

        if (firstStart) {
            if (messages.isEmpty()) {
                lastReceivedMessage.set(NO_MESSAGE);
                Log.i("Last available message id: no stored messages on server");
            } else {
                lastReceivedMessage.set(messages.get(0).getId());
                Log.i("Last available message id: " + lastReceivedMessage.get());
            }
        } else {
            if (!messages.isEmpty() && message.getMessages().get(0).getId() > lastReceivedMessage.get()) {
                Log.i("Missed messages while being disconnected from the WebSocket, publishing them now.");
                if (lastReceivedMessage.get() == NO_MESSAGE) {
                    notifyTill(url, token, 0);
                } else {
                    notifyTill(url, token, lastReceivedMessage.get());
                }
            } else {
                Log.i("Missed no messages while being disconnected from the WebSocket.");
            }
        }
    }

    private void notifyTill(String url, String token, int till) {
        Integer since = null;
        while (true) {
            PagedMessages messages = getMessages(url, token, 10, since);
            for (Message message : messages.getMessages()) {
                if (message.getId() > till) {
                    notify(message);
                } else {
                    break;
                }
            }
            since = messages.getPaging().getSince();
            if (since <= 0) {
                // no messages left
                break;
            }
        }
    }

    private PagedMessages getMessages(String url, String token, int limit, @Nullable Integer since) {
        HttpUrl.Builder builder = HttpUrl.parse(url).newBuilder()
                .addPathSegment("message")
                .addQueryParameter("token", token)
                .addQueryParameter("limit", String.valueOf(limit));
        if (since != null) {
            builder.addQueryParameter("since", String.valueOf(since));
        }
        HttpUrl httpUrl = builder.build();
        final Request request = new Request.Builder().url(httpUrl).get().build();
        try {
            Response execute = client.newCall(request).execute();
            if (execute.isSuccessful()) {
                return gson.fromJson(execute.body().string(), PagedMessages.class);
            }
        } catch (IOException e) {
            Log.e("Could not request messages", e);
        }
        PagedMessages pagedMessages = new PagedMessages();
        pagedMessages.setMessages(new ArrayList<Message>());
        Paging paging = new Paging();
        paging.setSince(0);
        pagedMessages.setPaging(paging);
        return pagedMessages;
    }

    private void start(boolean firstStart) {
        String url = appPreferences().getString(URL, null);
        String token = appPreferences().getString(TOKEN, null);

        if (url == null || token == null) {
            Log.i("url or token not configured; login required");
            foregroundNotification("login required");
            return;
        }

        ensureAllMessagesArePublished(firstStart, url, token);

        HttpUrl httpUrl = HttpUrl.parse(url).newBuilder().addPathSegment("stream").addQueryParameter("token", token).build();

        final Request request = new Request.Builder().url(httpUrl).get().build();

        foregroundNotification("Initializing WebSocket");
        Log.i("Initializing WebSocket");

        final WebSocket newSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.i("Initialized WebSocket");
                foregroundNotification("Listening to " + request.url().host());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Message message = gson.fromJson(text, Message.class);
                PushService.this.notify(message);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.e("WebSocket closed " + reason);
                foregroundNotification("WebSocket closed, re-login required");
                showNotification(-4, "WebSocket closed", "The WebSocket connection closed, this normally means the token(login) was invalidated. A re-login is required");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                foregroundNotification("Error: " + t.getMessage());
                Log.e("WebSocket failure", t);
                if (response != null && response.code() >= 400 && response.code() <= 499) {
                    showNotification(-2, "WebSocket Bad-Request", "Could not connect: " + response.message());
                    appPreferences().edit().remove(TOKEN).apply();
                    return;
                }

                boolean recentErrored = recentErrored();
                lastError.set(System.currentTimeMillis());

                if (recentErrored) {
                    Log.i("Waiting one minute to reconnect to the WebSocket (because WebSocket failed recently)");
                    foregroundNotification("WebSocket connected failed, trying to reconnect in one minute.");
                    handler.postDelayed(pushServiceAfterErrorInNewThread, TimeUnit.MINUTES.toMillis(1));
                } else {
                    Log.i("Trying to reconnect to WebSocket");
                    start(false);
                }
            }
        });

        synchronized (socketLock) {
            socket = newSocket;
        }
    }

    private boolean recentErrored() {
        return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1) < lastError.get();
    }

    private void showNotification(int id, String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, "GOTIFY_CHANNEL");

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setTicker("Gotify - " + title)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setChannelId(OreoNotificationSupport.CHANNEL_ID)
                .setContentIntent(contentIntent);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, b.build());
    }

    private SharedPreferences appPreferences() {
        // https://github.com/sriraman/react-native-shared-preferences/issues/12 for why wit_player_shared_preferences
        return this.getSharedPreferences("wit_player_shared_preferences", Context.MODE_PRIVATE);
    }

    private void notify(Message message) {
        if (lastReceivedMessage.get() < message.getId()) {
            lastReceivedMessage.set(message.getId());
        }

        showNotification(message.getId(), message.getTitle(), message.getMessage());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("Destroying WebSocket-Service");

        sendBroadcast(new Intent(RestartPushService.class.getName()));

        synchronized (socketLock) {
            if (socket != null) {
                socket.close(1000, "service stopped");
            }
        }

    }
}
