package com.github.gotify.service

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.gotify.NotificationSupport
import com.github.gotify.Settings
import com.github.gotify.Utils
import com.github.gotify.api.Callback
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.MessageApi
import org.tinylog.kotlin.Logger

internal class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, INVALID_MESSAGE_ID)
        if (messageId <= 0) {
            Logger.warn("Ignoring delete notification action with invalid message id")
            return
        }

        val settings = Settings(context)
        if (!settings.tokenExists()) {
            Logger.warn("Cannot delete notification because no client token is configured")
            return
        }

        val pendingResult = goAsync()
        ClientFactory.clientToken(settings)
            .createService(MessageApi::class.java)
            .deleteMessage(messageId)
            .enqueue(
                Callback.call(
                    onSuccess = {
                        try {
                            val notificationManager =
                                context.getSystemService(
                                    Context.NOTIFICATION_SERVICE
                                ) as NotificationManager
                            notificationManager.cancel(Utils.longToInt(messageId))
                            val hasMessageNotifications =
                                notificationManager.activeNotifications.any { statusBarNotification ->
                                    val notification = statusBarNotification.notification
                                    notification.group == NotificationSupport.Group.MESSAGES &&
                                        (notification.flags and
                                            Notification.FLAG_GROUP_SUMMARY) == 0
                                }
                            if (!hasMessageNotifications) {
                                notificationManager.cancel(
                                    NotificationSupport.ID.GROUP_SUMMARY
                                )
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    },
                    onError = {
                        pendingResult.finish()
                    }
                )
            )
    }

    companion object {
        const val EXTRA_MESSAGE_ID = "com.github.gotify.MESSAGE_ID"
        private const val INVALID_MESSAGE_ID = -1L
    }
}
