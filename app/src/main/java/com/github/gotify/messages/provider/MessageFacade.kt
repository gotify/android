package com.github.gotify.messages.provider

import com.github.gotify.client.api.MessageApi
import com.github.gotify.client.model.Message

internal class MessageFacade(api: MessageApi, private val applicationHolder: ApplicationHolder) {
    private val requester = MessageRequester(api)
    private val state = MessageStateHolder()

    @Synchronized
    operator fun get(appId: Long): List<MessageWithImage> {
        return MessageImageCombiner.combine(state.state(appId).messages, applicationHolder.get())
    }

    @Synchronized
    fun addMessages(messages: List<Message>) {
        messages.forEach {
            state.newMessage(it)
        }
    }

    @Synchronized
    fun loadMore(appId: Long): List<MessageWithImage> {
        val state = state.state(appId)
        if (state.hasNext || !state.loaded) {
            val pagedMessages = requester.loadMore(state)
            if (pagedMessages != null) {
                this.state.newMessages(appId, pagedMessages)
            }
        }
        return get(appId)
    }

    @Synchronized
    fun loadMoreIfNotPresent(appId: Long) {
        val state = state.state(appId)
        if (!state.loaded) {
            loadMore(appId)
        }
    }

    @Synchronized
    fun clear() {
        state.clear()
    }

    fun getLastReceivedMessage(): Long = state.lastReceivedMessage

    @Synchronized
    fun deleteLocal(message: Message) {
        // If there is already a deletion pending, that one should be executed before scheduling the
        // next deletion.
        if (state.deletionPending()) commitDelete()
        state.deleteMessage(message)
    }

    @Synchronized
    fun commitDelete() {
        if (state.deletionPending()) {
            val deletion = state.purgePendingDeletion()
            requester.asyncRemoveMessage(deletion!!.message)
        }
    }

    @Synchronized
    fun undoDeleteLocal(): MessageDeletion? = state.undoPendingDeletion()

    @Synchronized
    fun deleteAll(appId: Long): Boolean {
        val success = requester.deleteAll(appId)
        state.deleteAll(appId)
        return success
    }

    @Synchronized
    fun canLoadMore(appId: Long): Boolean = state.state(appId).hasNext
}
