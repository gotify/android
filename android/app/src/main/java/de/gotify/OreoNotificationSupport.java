package de.gotify;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * Creates a Notification channel for android oreo.
 */
public class OreoNotificationSupport {
    public static final String CHANNEL_ID = "gotify";

    @RequiresApi(Build.VERSION_CODES.O)
    public static void createChannel(NotificationManager notificationManager) {
        try {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Gotify", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        } catch (Exception e) {
            Log.e("Could not create channel", e);
        }
    }
}