package com.github.alertify.client.api;

import com.github.alertify.client.ApiClient;

import org.junit.Before;
import org.junit.Test;

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
