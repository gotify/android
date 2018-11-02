package com.github.gotify.messages.provider;

import com.github.gotify.client.model.Message;
import com.github.gotify.client.model.PagedMessages;
import java.util.HashMap;
import java.util.Map;

class MessageStateHolder {
    private int lastReceivedMessage = -1;
    private Map<Integer, MessageState> states = new HashMap<>();

    synchronized void clear() {
        states = new HashMap<>();
    }

    synchronized void newMessages(Integer appId, PagedMessages pagedMessages) {
        MessageState state = state(appId);

        if (!state.loaded && pagedMessages.getMessages().size() > 0) {
            lastReceivedMessage =
                    Math.max(pagedMessages.getMessages().get(0).getId(), lastReceivedMessage);
        }

        state.loaded = true;
        state.messages.addAll(pagedMessages.getMessages());
        state.hasNext = pagedMessages.getPaging().getNext() != null;
        state.nextSince = pagedMessages.getPaging().getSince();
        state.appId = appId;
        states.put(appId, state);
    }

    synchronized void newMessage(Message message) {
        MessageState allMessages = state(MessageState.ALL_MESSAGES);
        MessageState appMessages = state(message.getAppid());

        if (allMessages.loaded) {
            allMessages.messages.add(0, message);
        }

        if (appMessages.loaded) {
            appMessages.messages.add(0, message);
        }

        lastReceivedMessage = message.getId();
    }

    synchronized MessageState state(Integer appId) {
        MessageState state = states.get(appId);
        if (state == null) {
            return emptyState(appId);
        }
        return state;
    }

    void deleteAll(Integer appId) {
        clear();
        MessageState state = state(appId);
        state.loaded = true;
        states.put(appId, state);
    }

    private MessageState emptyState(Integer appId) {
        MessageState emptyState = new MessageState();
        emptyState.loaded = false;
        emptyState.hasNext = false;
        emptyState.nextSince = 0;
        emptyState.appId = appId;
        return emptyState;
    }

    synchronized int getLastReceivedMessage() {
        return lastReceivedMessage;
    }

    void removeMessage(Message message) {
        MessageState allMessages = state(MessageState.ALL_MESSAGES);
        MessageState appMessages = state(message.getAppid());

        if (allMessages.loaded) {
            allMessages.messages.remove(message);
        }

        if (appMessages.loaded) {
            appMessages.messages.remove(message);
        }
    }
}
