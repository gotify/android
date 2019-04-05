package com.github.gotify.messages;

import com.github.gotify.client.model.Message;
import java.util.Map;

public final class Extras {
    private Extras() {}

    public static boolean useMarkdown(Message message) {
        if (message.getExtras() == null) {
            return false;
        }

        Object display = message.getExtras().get("client::display");
        if (!(display instanceof Map)) {
            return false;
        }

        return "text/markdown".equals(((Map) display).get("contentType"));
    }
}
