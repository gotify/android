package com.github.gotify.client.api;

import com.github.gotify.client.ApiClient;
import com.github.gotify.client.model.VersionInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
