package com.github.gotify

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.github.gotify.client.model.Application
import org.tinylog.kotlin.Logger

internal object NotificationSupport {
    @RequiresApi(Build.VERSION_CODES.O)
    fun createForegroundChannel(context: Context, notificationManager: NotificationManager) {
        // Low importance so that persistent notification can be sorted towards bottom of
        // notification shade. Also prevents vibrations caused by persistent notification
        val foreground = NotificationChannel(
            Channel.FOREGROUND,
            context.getString(R.string.notification_channel_title_foreground),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(foreground)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannels(
        context: Context,
        notificationManager: NotificationManager,
        applications: List<Application>
    ) {
        if (areAppChannelsRequested(context)) {
            notificationManager.notificationChannels.forEach { channel ->
                if (channel.id != Channel.FOREGROUND) {
                    notificationManager.deleteNotificationChannel(channel.id)
                }
            }
            applications.forEach { app ->
                createAppChannels(context, notificationManager, app.id.toString(), app.name)
            }
        } else {
            notificationManager.notificationChannelGroups.forEach { group ->
                notificationManager.deleteNotificationChannelGroup(group.id)
            }
            createGeneralChannels(context, notificationManager)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createGeneralChannels(context: Context, notificationManager: NotificationManager) {
        try {
            val messagesImportanceMin = NotificationChannel(
                Channel.MESSAGES_IMPORTANCE_MIN,
                context.getString(R.string.notification_channel_title_min),
                NotificationManager.IMPORTANCE_MIN
            )

            val messagesImportanceLow = NotificationChannel(
                Channel.MESSAGES_IMPORTANCE_LOW,
                context.getString(R.string.notification_channel_title_low),
                NotificationManager.IMPORTANCE_LOW
            )

            val messagesImportanceDefault = NotificationChannel(
                Channel.MESSAGES_IMPORTANCE_DEFAULT,
                context.getString(R.string.notification_channel_title_normal),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
            }

            val messagesImportanceHigh = NotificationChannel(
                Channel.MESSAGES_IMPORTANCE_HIGH,
                context.getString(R.string.notification_channel_title_high),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(messagesImportanceMin)
            notificationManager.createNotificationChannel(messagesImportanceLow)
            notificationManager.createNotificationChannel(messagesImportanceDefault)
            notificationManager.createNotificationChannel(messagesImportanceHigh)
        } catch (e: Exception) {
            Logger.error(e, "Could not create channel")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createChannelIfNonexistent(context: Context, groupId: String, channelId: String) {
        if (!doesNotificationChannelExist(context, channelId)) {
            val notificationManager = (context as Service)
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createAppChannels(context, notificationManager, groupId, groupId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAppChannels(
        context: Context,
        notificationManager: NotificationManager,
        groupId: String,
        groupName: String
    ) {
        try {
            notificationManager.createNotificationChannelGroup(
                NotificationChannelGroup(
                    groupId,
                    groupName
                )
            )

            val messagesImportanceMin = NotificationChannel(
                getChannelID(Channel.MESSAGES_IMPORTANCE_MIN, groupId),
                context.getString(R.string.notification_channel_title_min),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                group = groupId
            }

            val messagesImportanceLow = NotificationChannel(
                getChannelID(Channel.MESSAGES_IMPORTANCE_LOW, groupId),
                context.getString(R.string.notification_channel_title_low),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = groupId
            }

            val messagesImportanceDefault = NotificationChannel(
                getChannelID(Channel.MESSAGES_IMPORTANCE_DEFAULT, groupId),
                context.getString(R.string.notification_channel_title_normal),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                group = groupId
            }

            val messagesImportanceHigh = NotificationChannel(
                getChannelID(Channel.MESSAGES_IMPORTANCE_HIGH, groupId),
                context.getString(R.string.notification_channel_title_high),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                group = groupId
            }

            notificationManager.createNotificationChannel(messagesImportanceMin)
            notificationManager.createNotificationChannel(messagesImportanceLow)
            notificationManager.createNotificationChannel(messagesImportanceDefault)
            notificationManager.createNotificationChannel(messagesImportanceHigh)
        } catch (e: Exception) {
            Logger.error(e, "Could not create channel")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun doesNotificationChannelExist(context: Context, channelId: String): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(channelId)
        return channel != null
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

    private fun getChannelID(importance: String, groupId: String): String {
        return "$groupId::$importance"
    }

    fun getChannelID(priority: Long, groupId: String): String {
        return getChannelID(convertPriorityToChannel(priority), groupId)
    }

    fun areAppChannelsRequested(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            context.getString(R.string.setting_key_notification_channels),
            context.resources.getBoolean(R.bool.notification_channels)
        )
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
