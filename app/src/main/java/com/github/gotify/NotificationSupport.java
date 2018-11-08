package com.github.gotify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.RequiresApi;
import com.github.gotify.log.Log;

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
            // Low importance so that persistent notification can be sorted towards bottom of
            // notification shade. Also prevents vibrations caused by persistent notification
            NotificationChannel foreground =
                    new NotificationChannel(
                            Channel.FOREGROUND,
                            "Gotify foreground notification",
                            NotificationManager.IMPORTANCE_LOW);
            // High importance for message notifications so that they are shown as heads-up
            // notifications and sorted towards the top of the notification shade
            NotificationChannel messages =
                    new NotificationChannel(
                            Channel.MESSAGES,
                            "Gotify messages",
                            NotificationManager.IMPORTANCE_HIGH);
            messages.enableLights(true);
            messages.setLightColor(Color.CYAN);
            notificationManager.createNotificationChannel(foreground);
            notificationManager.createNotificationChannel(messages);
        } catch (Exception e) {
            Log.e("Could not create channel", e);
        }
    }
}
