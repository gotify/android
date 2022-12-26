package com.github.gotify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.gotify.log.Log

internal object NotificationSupport {
    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannels(notificationManager: NotificationManager) {
        try {
            // Low importance so that persistent notification can be sorted towards bottom of
            // notification shade. Also prevents vibrations caused by persistent notification
            val foreground = NotificationChannel(
                Channel.FOREGROUND,
                "Gotify foreground notification",
                NotificationManager.IMPORTANCE_LOW
            )
            foreground.setShowBadge(false)

            val messagesImportanceMin = NotificationChannel(
                Channel.MESSAGES_IMPORTANCE_MIN,
                "Min priority messages (<1)",
                NotificationManager.IMPORTANCE_MIN
            )

            val messagesImportanceLow = NotificationChannel(
                Channel.MESSAGES_IMPORTANCE_LOW,
                "Low priority messages (1-3)",
                NotificationManager.IMPORTANCE_LOW
            )

            val messagesImportanceDefault = NotificationChannel(
                Channel.MESSAGES_IMPORTANCE_DEFAULT,
                "Normal priority messages (4-7)",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            messagesImportanceDefault.enableLights(true)
            messagesImportanceDefault.lightColor = Color.CYAN
            messagesImportanceDefault.enableVibration(true)

            val messagesImportanceHigh = NotificationChannel(
                Channel.MESSAGES_IMPORTANCE_HIGH,
                "High priority messages (>7)",
                NotificationManager.IMPORTANCE_HIGH
            )
            messagesImportanceHigh.enableLights(true)
            messagesImportanceHigh.lightColor = Color.CYAN
            messagesImportanceHigh.enableVibration(true)

            notificationManager.createNotificationChannel(foreground)
            notificationManager.createNotificationChannel(messagesImportanceMin)
            notificationManager.createNotificationChannel(messagesImportanceLow)
            notificationManager.createNotificationChannel(messagesImportanceDefault)
            notificationManager.createNotificationChannel(messagesImportanceHigh)
        } catch (e: Exception) {
            Log.e("Could not create channel", e)
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
    fun convertPriorityToChannel(priority: Long): String {
        return if (priority < 1) {
            Channel.MESSAGES_IMPORTANCE_MIN
        } else if (priority < 4) {
            Channel.MESSAGES_IMPORTANCE_LOW
        } else if (priority < 8) {
            Channel.MESSAGES_IMPORTANCE_DEFAULT
        } else {
            Channel.MESSAGES_IMPORTANCE_HIGH
        }
    }

    object Group {
        const val MESSAGES = "GOTIFY_GROUP_MESSAGES"
    }

    object Channel {
        const val FOREGROUND = "gotify_foreground"
        const val MESSAGES_IMPORTANCE_MIN = "gotify_messages_min_importance"
        const val MESSAGES_IMPORTANCE_LOW = "gotify_messages_low_importance"
        const val MESSAGES_IMPORTANCE_DEFAULT = "gotify_messages_default_importance"
        const val MESSAGES_IMPORTANCE_HIGH = "gotify_messages_high_importance"
    }

    object ID {
        const val FOREGROUND = -1
        const val GROUPED = -2
    }
}
