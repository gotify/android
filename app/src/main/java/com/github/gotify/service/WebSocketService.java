package com.github.gotify.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import com.github.gotify.MissedMessageUtil;
import com.github.gotify.NotificationSupport;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.model.Message;
import com.github.gotify.log.Log;
import com.github.gotify.log.UncaughtExceptionHandler;
import com.github.gotify.messages.MessagesActivity;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketService extends Service {

    public static final String NEW_MESSAGE_BROADCAST =
            WebSocketService.class.getName() + ".NEW_MESSAGE";

    private static final int NOT_LOADED = -2;

    private Settings settings;
    private WebSocketConnection connection;

    private AtomicInteger lastReceivedMessage = new AtomicInteger(NOT_LOADED);
    private MissedMessageUtil missingMessageUtil;

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new Settings(this);
        missingMessageUtil =
                new MissedMessageUtil(ClientFactory.clientToken(settings.url(), settings.token()));
        Log.i("Create " + getClass().getSimpleName());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connection != null) {
            connection.close();
        }
        Log.w("Destroy " + getClass().getSimpleName());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.init(this);

        if (connection != null) {
            connection.close();
        }

        Log.i("Starting " + getClass().getSimpleName());
        super.onStartCommand(intent, flags, startId);
        new Thread(this::startPushService).run();

        return START_STICKY;
    }

    private void startPushService() {
        UncaughtExceptionHandler.registerCurrentThread();
        foreground(getString(R.string.websocket_init));

        if (lastReceivedMessage.get() == NOT_LOADED) {
            missingMessageUtil.lastReceivedMessage(lastReceivedMessage::set);
        }

        connection =
                new WebSocketConnection(settings.url(), settings.token())
                        .onOpen(this::onOpen)
                        .onClose(() -> foreground(getString(R.string.websocket_closed)))
                        .onBadRequest(this::onBadRequest)
                        .onFailure((min) -> foreground(getString(R.string.websocket_failed, min)))
                        .onMessage(this::onMessage)
                        .onReconnected(this::notifyMissedNotifications)
                        .start();
    }

    private void onBadRequest(String message) {
        foreground(getString(R.string.websocket_could_not_connect, message));
    }

    private void onOpen() {
        foreground(getString(R.string.websocket_listening, settings.url()));
    }

    private void notifyMissedNotifications() {
        int messageId = lastReceivedMessage.get();
        if (messageId == NOT_LOADED) {
            return;
        }

        List<Message> messages = missingMessageUtil.missingMessages(messageId);

        if (messages.size() > 5) {
            onGroupedMessages(messages);
        } else {
            for (Message message : messages) {
                onMessage(message);
            }
        }
    }

    private void onGroupedMessages(List<Message> messages) {
        for (Message message : messages) {
            if (lastReceivedMessage.get() < message.getId()) {
                lastReceivedMessage.set(message.getId());
            }
            broadcast(message);
        }
        int size = messages.size();
        showNotification(
                NotificationSupport.ID.GROUPED,
                getString(R.string.missed_messages),
                getString(R.string.grouped_message, size));
    }

    private void onMessage(Message message) {
        if (lastReceivedMessage.get() < message.getId()) {
            lastReceivedMessage.set(message.getId());
        }
        broadcast(message);
        showNotification(message.getId(), message.getTitle(), message.getMessage());
    }

    private void broadcast(Message message) {
        Intent intent = new Intent();
        intent.setAction(NEW_MESSAGE_BROADCAST);
        intent.putExtra("message", Utils.json().serialize(message));
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void foreground(String message) {
        Intent notificationIntent = new Intent(this, MessagesActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this, NotificationSupport.Channel.FOREGROUND)
                        .setSmallIcon(R.drawable.ic_gotify)
                        .setOngoing(true)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(message)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(NotificationSupport.ID.FOREGROUND, notification);
    }

    private void showNotification(int id, String title, String message) {
        Intent intent = new Intent(this, MessagesActivity.class);
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(this, NotificationSupport.Channel.MESSAGES);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            showNotificationGroup();
        }

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_gotify)
                .setTicker(getString(R.string.app_name) + " - " + title)
                .setGroup(NotificationSupport.Group.MESSAGES)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setContentIntent(contentIntent);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, b.build());
    }

    @RequiresApi(Build.VERSION_CODES.N)
    public void showNotificationGroup() {
        Intent intent = new Intent(this, MessagesActivity.class);
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(this, NotificationSupport.Channel.MESSAGES);

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_gotify)
                .setTicker(getString(R.string.app_name))
                .setGroup(NotificationSupport.Group.MESSAGES)
                .setContentTitle(getString(R.string.grouped_notification_text))
                .setGroupSummary(true)
                .setContentText(getString(R.string.grouped_notification_text))
                .setContentIntent(contentIntent);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(-5, b.build());
    }
}
