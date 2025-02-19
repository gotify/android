package com.github.gotify.client.api;

import com.github.gotify.client.ApiClient;
import com.github.gotify.client.model.Client;
import com.github.gotify.client.model.ClientParams;
import com.github.gotify.client.model.Error;
import org.junit.Before;
import org.junit.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * API tests for ClientApi
 */
public class ClientApiTest {

    private ClientApi api;

    @Before
    public void setup() {
        api = new ApiClient().createService(ClientApi.class);
    }


    /**
     * Create a client.
     *
     * 
     */
    @Test
    public void createClientTest() {
        ClientParams body = null;
        // Client response = api.createClient(body);

        // TODO: test validations
    }

    /**
     * Delete a client.
     *
     * 
     */
    @Test
    public void deleteClientTest() {
        Long id = null;
        // Void response = api.deleteClient(id);

        // TODO: test validations
    }

    /**
     * Return all clients.
     *
     * 
     */
    @Test
    public void getClientsTest() {
        // List<Client> response = api.getClients();

        // TODO: test validations
    }

    /**
     * Update a client.
     *
     * 
     */
    @Test
    public void updateClientTest() {
        ClientParams body = null;
        Long id = null;
        // Client response = api.updateClient(body, id);

        // TODO: test validations
    }
}
