package com.github.gotify.messages;

import com.github.gotify.client.model.Message;
import java.util.Map;

public final class Extras {
    private Extras() {}

    public static boolean useMarkdown(Message message) {
        return useMarkdown(message.getExtras());
    }

    public static boolean useMarkdown(Map<String, Object> extras) {
        if (extras == null) {
            return false;
        }

        Object display = extras.get("client::display");
        if (!(display instanceof Map)) {
            return false;
        }

        return "text/markdown".equals(((Map) display).get("contentType"));
    }

    public static <T> T getNestedValue(Class<T> clazz, Message message, String... keys) {
        return getNestedValue(clazz, message.getExtras(), keys);
    }

    public static <T> T getNestedValue(Class<T> clazz, Map<String, Object> extras, String... keys) {
        Object value = extras;

        for (String key : keys) {
            if (value == null) {
                return null;
            }

            if (!(value instanceof Map<?, ?>)) {
                return null;
            }

            value = ((Map<?, ?>) value).get(key);
        }

        if (!clazz.isInstance(value)) {
            return null;
        }

        return clazz.cast(value);
    }
}
