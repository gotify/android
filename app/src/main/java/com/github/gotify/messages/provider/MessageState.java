package com.github.gotify.messages.provider;

import com.github.gotify.client.model.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageState {
    public static final long ALL_MESSAGES = -1;

    long appId;
    boolean loaded;
    boolean hasNext;
    long nextSince = 0;
    List<Message> messages = new ArrayList<>();
}
