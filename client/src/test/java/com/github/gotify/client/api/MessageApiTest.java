package com.github.gotify.client.api;

import com.github.gotify.client.ApiClient;
import com.github.gotify.client.model.Error;
import com.github.gotify.client.model.Message;
import com.github.gotify.client.model.PagedMessages;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for MessageApi
 */
public class MessageApiTest {

    private MessageApi api;

    @Before
    public void setup() {
        api = new ApiClient().createService(MessageApi.class);
    }

    /**
     * Create a message.
     *
     * __NOTE__: This API ONLY accepts an application token as authentication.
     */
    @Test
    public void createMessageTest() {
        Message body = null;
        // Message response = api.createMessage(body);

        // TODO: test validations
    }
    /**
     * Delete all messages from a specific application.
     *
     * 
     */
    @Test
    public void deleteAppMessagesTest() {
        Integer id = null;
        // Void response = api.deleteAppMessages(id);

        // TODO: test validations
    }
    /**
     * Deletes a message with an id.
     *
     * 
     */
    @Test
    public void deleteMessageTest() {
        Integer id = null;
        // Void response = api.deleteMessage(id);

        // TODO: test validations
    }
    /**
     * Delete all messages.
     *
     * 
     */
    @Test
    public void deleteMessagesTest() {
        // Void response = api.deleteMessages();

        // TODO: test validations
    }
    /**
     * Return all messages from a specific application.
     *
     * 
     */
    @Test
    public void getAppMessagesTest() {
        Integer id = null;
        Integer limit = null;
        Integer since = null;
        // PagedMessages response = api.getAppMessages(id, limit, since);

        // TODO: test validations
    }
    /**
     * Return all messages.
     *
     * 
     */
    @Test
    public void getMessagesTest() {
        Integer limit = null;
        Integer since = null;
        // PagedMessages response = api.getMessages(limit, since);

        // TODO: test validations
    }
    /**
     * Websocket, return newly created messages.
     *
     * 
     */
    @Test
    public void streamMessagesTest() {
        // Message response = api.streamMessages();

        // TODO: test validations
    }
}
