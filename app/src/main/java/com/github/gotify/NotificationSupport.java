package com.github.gotify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.github.gotify.log.Log;

import androidx.annotation.RequiresApi;

public class NotificationSupport {
    public static final class Group {
        public static final String MESSAGES = "GOTIFY_GROUP_MESSAGES";
    }

    public static final class Channel {
        public static final String FOREGROUND = "gotify_foreground";
        public static final String MESSAGES = "gotify_messages";
    }

    public static final class ID {
        public static final int FOREGROUND = -1;
        public static final int GROUPED = -2;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static void createChannels(NotificationManager notificationManager) {
        try {
            NotificationChannel foreground =
                    new NotificationChannel(
                            Channel.FOREGROUND,
                            "Gotify foreground notification",
                            NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel messages =
                    new NotificationChannel(
                            Channel.MESSAGES,
                            "Gotify messages",
                            NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(foreground);
            notificationManager.createNotificationChannel(messages);
        } catch (Exception e) {
            Log.e("Could not create channel", e);
        }
    }
}
