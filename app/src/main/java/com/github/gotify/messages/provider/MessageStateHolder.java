package com.github.gotify.messages.provider;

import com.github.gotify.client.model.Message;
import com.github.gotify.client.model.PagedMessages;
import java.util.HashMap;
import java.util.Map;

class MessageStateHolder {
    private int lastReceivedMessage = -1;
    private Map<Integer, MessageState> states = new HashMap<>();

    private MessageDeletion pendingDeletion = null;

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

        // If there is a message with pending deletion, it should not reappear in the list in case
        // it is added again.
        if (deletionPending()) {
            deleteMessage(pendingDeletion.getMessage());
        }
    }

    synchronized void newMessage(Message message) {
        // If there is a message with pending deletion, its indices are going to change. To keep
        // them consistent the deletion is undone first and redone again after adding the new
        // message.
        MessageDeletion deletion = undoPendingDeletion();

        addMessage(message, 0, 0);
        lastReceivedMessage = message.getId();

        if (deletion != null) deleteMessage(deletion.getMessage());
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

    synchronized void deleteMessage(Message message) {
        MessageState allMessages = state(MessageState.ALL_MESSAGES);
        MessageState appMessages = state(message.getAppid());

        int pendingDeletedAllPosition = -1;
        int pendingDeletedAppPosition = -1;

        if (allMessages.loaded) {
            int allPosition = allMessages.messages.indexOf(message);
            allMessages.messages.remove(allPosition);
            pendingDeletedAllPosition = allPosition;
        }

        if (appMessages.loaded) {
            int appPosition = appMessages.messages.indexOf(message);
            appMessages.messages.remove(appPosition);
            pendingDeletedAppPosition = appPosition;
        }

        pendingDeletion =
                new MessageDeletion(message, pendingDeletedAllPosition, pendingDeletedAppPosition);
    }

    synchronized MessageDeletion undoPendingDeletion() {
        if (pendingDeletion != null)
            addMessage(
                    pendingDeletion.getMessage(),
                    pendingDeletion.getAllPosition(),
                    pendingDeletion.getAppPosition());
        return purgePendingDeletion();
    }

    synchronized MessageDeletion purgePendingDeletion() {
        MessageDeletion result = pendingDeletion;
        pendingDeletion = null;
        return result;
    }

    boolean deletionPending() {
        return pendingDeletion != null;
    }

    private void addMessage(Message message, int allPosition, int appPosition) {
        MessageState allMessages = state(MessageState.ALL_MESSAGES);
        MessageState appMessages = state(message.getAppid());

        if (allMessages.loaded && allPosition != -1) {
            allMessages.messages.add(allPosition, message);
        }

        if (appMessages.loaded && appPosition != -1) {
            appMessages.messages.add(appPosition, message);
        }
    }
}
