package com.github.gotify.client.api;

import com.github.gotify.client.ApiClient;
import com.github.gotify.client.model.Health;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for HealthApi
 */
public class HealthApiTest {

    private HealthApi api;

    @Before
    public void setup() {
        api = new ApiClient().createService(HealthApi.class);
    }

    /**
     * Get health information.
     *
     * 
     */
    @Test
    public void getHealthTest() {
        // Health response = api.getHealth();

        // TODO: test validations
    }
}
