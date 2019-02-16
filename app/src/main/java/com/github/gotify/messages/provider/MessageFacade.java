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

    public void delete(Message message) {
        this.requester.asyncRemoveMessage(message);
        this.state.removeMessage(message);
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
