package com.github.alertify.client.api;

import com.github.alertify.client.ApiClient;
import com.github.alertify.client.model.Message;

import org.junit.Before;
import org.junit.Test;

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
        Long id = null;
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
        Long id = null;
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
        Long id = null;
        Integer limit = null;
        Long since = null;
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
        Long since = null;
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
