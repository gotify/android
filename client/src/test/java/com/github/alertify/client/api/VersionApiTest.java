package com.github.alertify.client.api;

import com.github.alertify.client.ApiClient;

import org.junit.Before;
import org.junit.Test;

/**
 * API tests for VersionApi
 */
public class VersionApiTest {

    private VersionApi api;

    @Before
    public void setup() {
        api = new ApiClient().createService(VersionApi.class);
    }

    /**
     * Get version information.
     *
     * 
     */
    @Test
    public void getVersionTest() {
        // VersionInfo response = api.getVersion();

        // TODO: test validations
    }
}
