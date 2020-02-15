package com.github.gotify.messages.provider;

import com.github.gotify.client.model.Message;

public final class MessageDeletion {
    private final Message message;
    private final int allPosition;
    private final int appPosition;

    public MessageDeletion(Message message, int allPosition, int appPosition) {
        this.message = message;
        this.allPosition = allPosition;
        this.appPosition = appPosition;
    }

    public int getAllPosition() {
        return allPosition;
    }

    public int getAppPosition() {
        return appPosition;
    }

    public Message getMessage() {
        return message;
    }
}
