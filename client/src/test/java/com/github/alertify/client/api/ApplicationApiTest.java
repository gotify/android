package com.github.alertify.client.api;

import com.github.alertify.client.ApiClient;
import com.github.alertify.client.model.Application;

import java.io.File;
import org.junit.Before;
import org.junit.Test;

/**
 * API tests for ApplicationApi
 */
public class ApplicationApiTest {

    private ApplicationApi api;

    @Before
    public void setup() {
        api = new ApiClient().createService(ApplicationApi.class);
    }

    /**
     * Create an application.
     *
     * 
     */
    @Test
    public void createAppTest() {
        Application body = null;
        // Application response = api.createApp(body);

        // TODO: test validations
    }
    /**
     * Delete an application.
     *
     * 
     */
    @Test
    public void deleteAppTest() {
        Long id = null;
        // Void response = api.deleteApp(id);

        // TODO: test validations
    }
    /**
     * Return all applications.
     *
     * 
     */
    @Test
    public void getAppsTest() {
        // List<Application> response = api.getApps();

        // TODO: test validations
    }
    /**
     * Update an application.
     *
     * 
     */
    @Test
    public void updateApplicationTest() {
        Application body = null;
        Long id = null;
        // Application response = api.updateApplication(body, id);

        // TODO: test validations
    }
    /**
     * Upload an image for an application.
     *
     * 
     */
    @Test
    public void uploadAppImageTest() {
        File file = null;
        Long id = null;
        // Application response = api.uploadAppImage(file, id);

        // TODO: test validations
    }
}
