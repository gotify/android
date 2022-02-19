package com.github.gotify;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
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
    public static void createForegroundChannel(Context context, NotificationManager notificationManager) {
        // Low importance so that persistent notification can be sorted towards bottom of
        // notification shade. Also prevents vibrations caused by persistent notification
        NotificationChannel foreground =
                new NotificationChannel(
                        Channel.FOREGROUND,
                        context.getString(R.string.notification_channel_title_foreground),
                        NotificationManager.IMPORTANCE_LOW);
        foreground.setShowBadge(false);
        notificationManager.createNotificationChannel(foreground);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createChannel(Context context, NotificationManager notificationManager, String groupid, String groupname) {
        try {
            notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(groupid, groupname));

            NotificationChannel messagesImportanceMin =
                    new NotificationChannel(
                            getChannelID(Channel.MESSAGES_IMPORTANCE_MIN, groupid),
                            context.getString(R.string.notification_channel_title_min),
                            NotificationManager.IMPORTANCE_MIN);
            messagesImportanceMin.setGroup(groupid);

            NotificationChannel messagesImportanceLow =
                    new NotificationChannel(
                            getChannelID(Channel.MESSAGES_IMPORTANCE_LOW, groupid),
                            context.getString(R.string.notification_channel_title_low),
                            NotificationManager.IMPORTANCE_LOW);
            messagesImportanceLow.setGroup(groupid);

            NotificationChannel messagesImportanceDefault =
                    new NotificationChannel(
                            getChannelID(Channel.MESSAGES_IMPORTANCE_DEFAULT, groupid),
                            context.getString(R.string.notification_channel_title_normal),
                            NotificationManager.IMPORTANCE_DEFAULT);
            messagesImportanceDefault.enableLights(true);
            messagesImportanceDefault.setLightColor(Color.CYAN);
            messagesImportanceDefault.enableVibration(true);
            messagesImportanceDefault.setGroup(groupid);

            NotificationChannel messagesImportanceHigh =
                    new NotificationChannel(
                            getChannelID(Channel.MESSAGES_IMPORTANCE_HIGH, groupid),
                            context.getString(R.string.notification_channel_title_high),
                            NotificationManager.IMPORTANCE_HIGH);
            messagesImportanceHigh.enableLights(true);
            messagesImportanceHigh.setLightColor(Color.CYAN);
            messagesImportanceHigh.enableVibration(true);
            messagesImportanceHigh.setGroup(groupid);

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

    public static String getChannelID(String importance, String groupid){
        return importance+"::"+groupid;
    }

    public static String getChannelID(long priority, String groupid){
        return getChannelID(convertPriorityToChannel(priority), groupid);
    }
}
