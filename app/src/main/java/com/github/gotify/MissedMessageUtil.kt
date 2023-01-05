package com.github.gotify

import com.github.gotify.api.Api
import com.github.gotify.api.ApiException
import com.github.gotify.api.Callback
import com.github.gotify.api.Callback.SuccessCallback
import com.github.gotify.client.api.MessageApi
import com.github.gotify.client.model.Message
import com.github.gotify.client.model.PagedMessages
import com.github.gotify.log.Log

internal class MissedMessageUtil(private val api: MessageApi) {
    fun lastReceivedMessage(successCallback: SuccessCallback<Long>) {
        api.getMessages(1, 0L).enqueue(
            Callback.call({ messages: PagedMessages ->
                if (messages.messages.size == 1) {
                    successCallback.onSuccess(messages.messages[0].id)
                } else {
                    successCallback.onSuccess(NO_MESSAGES)
                }
            }
            ) {}
        )
    }

    fun missingMessages(till: Long): List<Message?> {
        val result = mutableListOf<Message?>()
        try {
            var since: Long? = null
            while (true) {
                val pagedMessages = Api.execute(api.getMessages(10, since))
                val messages = pagedMessages.messages
                val filtered = filter(messages, till)
                result.addAll(filtered)
                if (messages.size != filtered.size ||
                    messages.size == 0 ||
                    pagedMessages.paging.next == null) {
                    break
                }
                since = pagedMessages.paging.since
            }
        } catch (e: ApiException) {
            Log.e("cannot retrieve missing messages", e)
        }
        return result.reversed()
    }

    private fun filter(messages: List<Message>, till: Long): List<Message?> {
        val result = mutableListOf<Message?>()
        for (message in messages) {
            if (message.id > till) {
                result.add(message)
            } else {
                break
            }
        }
        return result
    }

    companion object {
        const val NO_MESSAGES = 0L
    }
}
