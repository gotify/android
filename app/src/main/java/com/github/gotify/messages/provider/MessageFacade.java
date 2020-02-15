package com.github.gotify.messages.provider;

import com.github.gotify.client.api.MessageApi;
import com.github.gotify.client.model.Message;
import com.github.gotify.client.model.PagedMessages;
import java.util.List;

public class MessageFacade {
    private final ApplicationHolder applicationHolder;
    private final MessageRequester requester;
    private final MessageStateHolder state;
    private final MessageImageCombiner combiner;
    private Message messagePendingDeletion;

    public MessageFacade(MessageApi api, ApplicationHolder applicationHolder) {
        this.applicationHolder = applicationHolder;
        this.requester = new MessageRequester(api);
        this.combiner = new MessageImageCombiner();
        this.state = new MessageStateHolder();
    }

    public List<MessageWithImage> get(Integer appId) {
        return combiner.combine(state.state(appId).messages, applicationHolder.get());
    }

    public void addMessages(List<Message> messages) {
        for (Message message : messages) {
            state.newMessage(message);
        }
    }

    public List<MessageWithImage> loadMore(Integer appId) {
        MessageState state = this.state.state(appId);
        if (state.hasNext || !state.loaded) {
            PagedMessages pagedMessages = requester.loadMore(state);
            this.state.newMessages(appId, pagedMessages);

            // If there is a message with pending removal, it should not reappear in the list when
            // reloading. Thus, it needs to be removed from the local list again after loading new
            // messages.
            if (messagePendingDeletion != null) {
                this.state.removeMessage(messagePendingDeletion);
            }
        }
        return get(appId);
    }

    public void loadMoreIfNotPresent(Integer appId) {
        MessageState state = this.state.state(appId);
        if (!state.loaded) {
            loadMore(appId);
        }
    }

    public void clear() {
        this.state.clear();
    }

    public int getLastReceivedMessage() {
        return state.getLastReceivedMessage();
    }

    public synchronized void deleteLocal(Message message) {
        this.state.removeMessage(message);
        // If there is already a deletion pending, that one should be executed before scheduling the
        // next deletion.
        if (messagePendingDeletion != null) {
            commitDelete();
        }
        messagePendingDeletion = message;
    }

    public synchronized void commitDelete() {
        this.requester.asyncRemoveMessage(messagePendingDeletion);
        messagePendingDeletion = null;
    }

    public synchronized PositionPair undoDeleteLocal() {
        messagePendingDeletion = null;
        return this.state.undoLastRemoveMessage();
    }

    public boolean deleteAll(Integer appId) {
        boolean success = this.requester.deleteAll(appId);
        this.state.deleteAll(appId);
        return success;
    }

    public boolean canLoadMore(Integer appId) {
        return state.state(appId).hasNext;
    }
}
