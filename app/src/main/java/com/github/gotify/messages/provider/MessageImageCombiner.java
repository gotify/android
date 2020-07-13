package com.github.gotify.messages.provider;

import com.github.gotify.client.model.Application;
import com.github.gotify.client.model.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageImageCombiner {

    List<MessageWithImage> combine(List<Message> messages, List<Application> applications) {
        Map<Long, String> appIdToImage = appIdToImage(applications);

        List<MessageWithImage> result = new ArrayList<>();

        for (Message message : messages) {
            MessageWithImage messageWithImage = new MessageWithImage();

            messageWithImage.message = message;
            messageWithImage.image = appIdToImage.get(message.getAppid());

            result.add(messageWithImage);
        }

        return result;
    }

    public static Map<Long, String> appIdToImage(List<Application> applications) {
        Map<Long, String> map = new ConcurrentHashMap<>();
        for (Application app : applications) {
            map.put(app.getId(), app.getImage());
        }
        return map;
    }
}
