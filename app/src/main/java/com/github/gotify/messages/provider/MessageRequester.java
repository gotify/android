package com.github.gotify.messages.provider;

import com.github.gotify.api.Api;
import com.github.gotify.client.ApiException;
import com.github.gotify.client.api.MessageApi;
import com.github.gotify.client.model.Message;
import com.github.gotify.client.model.PagedMessages;
import com.github.gotify.log.Log;

class MessageRequester {
    private static final Integer LIMIT = 100;
    private MessageApi messageApi;

    MessageRequester(MessageApi messageApi) {
        this.messageApi = messageApi;
    }

    PagedMessages loadMore(MessageState state) {
        try {
            Log.i("Loading more messages for " + state.appId);
            if (MessageState.ALL_MESSAGES == state.appId) {
                return messageApi.getMessages(LIMIT, state.nextSince);
            } else {
                return messageApi.getAppMessages(state.appId, LIMIT, state.nextSince);
            }
        } catch (ApiException apiException) {
            Log.e("failed requesting messages", apiException);
            return null;
        }
    }

    void asyncRemoveMessage(Message message) {
        Log.i("Removing message with id " + message.getId());
        Api.<Void>withLogging((cb) -> messageApi.deleteMessageAsync(message.getId(), cb))
                .handle((a) -> {}, (e) -> {});
    }

    boolean deleteAll(Integer appId) {
        try {
            Log.i("Deleting all messages for " + appId);
            if (MessageState.ALL_MESSAGES == appId) {
                messageApi.deleteMessages();
            } else {
                messageApi.deleteAppMessages(appId);
            }
            return true;
        } catch (ApiException e) {
            Log.e("Could not delete messages", e);
            return false;
        }
    }
}
