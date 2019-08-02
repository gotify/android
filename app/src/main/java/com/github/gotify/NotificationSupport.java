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
        public static final String MESSAGES_IMPORTANCE_HIGH = "gotify_messages_high_importance";
    }

    public static final class ID {
        public static final int FOREGROUND = -1;
        public static final int GROUPED = -2;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static void createChannel(NotificationManager notificationManager, String name, String id) {
        NotificationChannel channel =
                new NotificationChannel(
                        id,
                        name,
                        NotificationManager.IMPORTANCE_HIGH);
        channel.enableLights(true);
        channel.setLightColor(Color.CYAN);
        channel.enableVibration(true);
        notificationManager.createNotificationChannel(channel);
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
            foreground.setShowBadge(false);

            NotificationChannel messagesImportanceHigh =
                    new NotificationChannel(
                            Channel.MESSAGES_IMPORTANCE_HIGH,
                            "Gotify notifications",
                            NotificationManager.IMPORTANCE_HIGH);
            messagesImportanceHigh.enableLights(true);
            messagesImportanceHigh.setLightColor(Color.CYAN);
            messagesImportanceHigh.enableVibration(true);

            notificationManager.createNotificationChannel(foreground);
            notificationManager.createNotificationChannel(messagesImportanceHigh);
        } catch (Exception e) {
            Log.e("Could not create channel", e);
        }
    }
}
