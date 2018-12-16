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
        public static final String MESSAGES_IMPORTANCE_MIN = "gotify_messages_min_importance";
        public static final String MESSAGES_IMPORTANCE_LOW = "gotify_messages_low_importance";
        public static final String MESSAGES_IMPORTANCE_DEFAULT =
                "gotify_messages_default_importance";
        public static final String MESSAGES_IMPORTANCE_HIGH = "gotify_messages_high_importance";
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
            foreground.setShowBadge(false);

            NotificationChannel messagesImportanceMin =
                    new NotificationChannel(
                            Channel.MESSAGES_IMPORTANCE_MIN,
                            "Min priority messages (<1)",
                            NotificationManager.IMPORTANCE_MIN);

            NotificationChannel messagesImportanceLow =
                    new NotificationChannel(
                            Channel.MESSAGES_IMPORTANCE_LOW,
                            "Low priority messages (1-3)",
                            NotificationManager.IMPORTANCE_LOW);

            NotificationChannel messagesImportanceDefault =
                    new NotificationChannel(
                            Channel.MESSAGES_IMPORTANCE_DEFAULT,
                            "Normal priority messages (4-7)",
                            NotificationManager.IMPORTANCE_DEFAULT);
            messagesImportanceDefault.enableLights(true);
            messagesImportanceDefault.setLightColor(Color.CYAN);
            messagesImportanceDefault.enableVibration(true);

            NotificationChannel messagesImportanceHigh =
                    new NotificationChannel(
                            Channel.MESSAGES_IMPORTANCE_HIGH,
                            "High priority messages (>7)",
                            NotificationManager.IMPORTANCE_HIGH);
            messagesImportanceHigh.enableLights(true);
            messagesImportanceHigh.setLightColor(Color.CYAN);
            messagesImportanceHigh.enableVibration(true);

            notificationManager.createNotificationChannel(foreground);
            notificationManager.createNotificationChannel(messagesImportanceMin);
            notificationManager.createNotificationChannel(messagesImportanceLow);
            notificationManager.createNotificationChannel(messagesImportanceDefault);
            notificationManager.createNotificationChannel(messagesImportanceHigh);
        } catch (Exception e) {
            Log.e("Could not create channel", e);
        }
    }

    /**
     * Map {@link com.github.gotify.client.model.Message#getPriority() Gotify message priorities to
     * Android channels.
     *
     * <pre>
     * Gotify Priority  | Android Importance
     * <= 0             | min
     * 1-3              | low
     * 4-7              | default
     * >= 8             | high
     * </pre>
     *
     * @param priority the Gotify priority to convert to a notification channel as a long.
     * @return the identifier of the notification channel as a String.
     */
    public static String convertPriorityToChannel(long priority) {
        if (priority < 1) {
            return Channel.MESSAGES_IMPORTANCE_MIN;
        } else if (priority < 4) {
            return Channel.MESSAGES_IMPORTANCE_LOW;
        } else if (priority < 8) {
            return Channel.MESSAGES_IMPORTANCE_DEFAULT;
        } else {
            return Channel.MESSAGES_IMPORTANCE_HIGH;
        }
    }
}
