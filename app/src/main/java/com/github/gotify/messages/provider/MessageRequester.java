package com.github.gotify.messages.provider;

import com.github.gotify.api.Api;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.Callback;
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
                return Api.execute(messageApi.getMessages(LIMIT, state.nextSince));
            } else {
                return Api.execute(messageApi.getAppMessages(state.appId, LIMIT, state.nextSince));
            }
        } catch (ApiException apiException) {
            Log.e("failed requesting messages", apiException);
            return null;
        }
    }

    void asyncRemoveMessage(Message message) {
        Log.i("Removing message with id " + message.getId());
        messageApi.deleteMessage(message.getId()).enqueue(Callback.call());
    }

    boolean deleteAll(Integer appId) {
        try {
            Log.i("Deleting all messages for " + appId);
            if (MessageState.ALL_MESSAGES == appId) {
                Api.execute(messageApi.deleteMessages());
            } else {
                Api.execute(messageApi.deleteAppMessages(appId));
            }
            return true;
        } catch (ApiException e) {
            Log.e("Could not delete messages", e);
            return false;
        }
    }
}
