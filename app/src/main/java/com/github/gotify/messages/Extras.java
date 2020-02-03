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

    public static <T> T getNestedValue(Message message, String... keys) {
        return getNestedValue(message.getExtras(), keys);
    }

    public static <T> T getNestedValue(Map<String, Object> extras, String... keys) {
        Object value = extras;
        for (String key : keys) {
            if ((Map) value == null) return null;
            value = ((Map) value).get(key);
        }
        return (T) value;
    }
}
