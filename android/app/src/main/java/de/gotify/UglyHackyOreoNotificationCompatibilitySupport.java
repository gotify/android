package de.gotify;


import android.app.NotificationManager;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * I hate reflections and android.
 */
public class UglyHackyOreoNotificationCompatibilitySupport {
    public static final String CHANNEL_ID = "gotify";

    /**
     * @param notificationManager the notification manager
     * @see <a href="https://stackoverflow.com/a/48861133/4244993">Code by Elad Nava</a>
     */
    public static void uglyInitializeNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        // Channel importance (3 means default importance)
        int channelImportance = 3;

        try {
            // Get NotificationChannel class via reflection (only available on devices running Android O or newer)
            Class notificationChannelClass = Class.forName("android.app.NotificationChannel");

            // Get NotificationChannel constructor
            Constructor<?> notificationChannelConstructor = notificationChannelClass.getDeclaredConstructor(String.class, CharSequence.class, int.class);

            // Instantiate new notification channel
            Object notificationChannel = notificationChannelConstructor.newInstance(CHANNEL_ID, "Gotify", channelImportance);

            // Get notification channel creation method via reflection
            Method createNotificationChannelMethod = notificationManager.getClass().getDeclaredMethod("createNotificationChannel", notificationChannelClass);

            // Invoke method on NotificationManager, passing in the channel object
            createNotificationChannelMethod.invoke(notificationManager, notificationChannel);

        } catch (Exception e) {
            Log.e("Creating notification channel failed.", e);
        }
    }
}