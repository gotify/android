package com.github.gotify.messages.provider

import com.github.gotify.api.Api
import com.github.gotify.api.ApiException
import com.github.gotify.api.Callback
import com.github.gotify.client.api.MessageApi
import com.github.gotify.client.model.Message
import com.github.gotify.client.model.PagedMessages
import org.tinylog.kotlin.Logger

internal class MessageRequester(private val messageApi: MessageApi) {
    fun loadMore(state: MessageState): PagedMessages? {
        return try {
            Logger.info("Loading more messages for ${state.appId}")
            if (MessageState.ALL_MESSAGES == state.appId) {
                Api.execute(messageApi.getMessages(LIMIT, state.nextSince))
            } else {
                Api.execute(messageApi.getAppMessages(state.appId, LIMIT, state.nextSince))
            }
        } catch (apiException: ApiException) {
            Logger.error(apiException, "failed requesting messages")
            null
        }
    }

    fun asyncRemoveMessage(message: Message) {
        Logger.info("Removing message with id ${message.id}")
        messageApi.deleteMessage(message.id).enqueue(Callback.call())
    }

    fun deleteAll(appId: Long): Boolean {
        return try {
            Logger.info("Deleting all messages for $appId")
            if (MessageState.ALL_MESSAGES == appId) {
                Api.execute(messageApi.deleteMessages())
            } else {
                Api.execute(messageApi.deleteAppMessages(appId))
            }
            true
        } catch (e: ApiException) {
            Logger.error(e, "Could not delete messages")
            false
        }
    }

    companion object {
        private const val LIMIT = 100
    }
}
