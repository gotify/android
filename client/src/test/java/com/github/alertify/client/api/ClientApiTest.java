package com.github.alertify.client.api;

import com.github.alertify.client.ApiClient;
import com.github.alertify.client.model.Client;

import org.junit.Before;
import org.junit.Test;

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
        Client body = null;
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
        Client body = null;
        Long id = null;
        // Client response = api.updateClient(body, id);

        // TODO: test validations
    }
}
