package com.github.alertify.client.api;

import com.github.alertify.client.ApiClient;

import org.junit.Before;
import org.junit.Test;

/**
 * API tests for PluginApi
 */
public class PluginApiTest {

    private PluginApi api;

    @Before
    public void setup() {
        api = new ApiClient().createService(PluginApi.class);
    }

    /**
     * Disable a plugin.
     *
     * 
     */
    @Test
    public void disablePluginTest() {
        Long id = null;
        // Void response = api.disablePlugin(id);

        // TODO: test validations
    }
    /**
     * Enable a plugin.
     *
     * 
     */
    @Test
    public void enablePluginTest() {
        Long id = null;
        // Void response = api.enablePlugin(id);

        // TODO: test validations
    }
    /**
     * Get YAML configuration for Configurer plugin.
     *
     * 
     */
    @Test
    public void getPluginConfigTest() {
        Long id = null;
        // Object response = api.getPluginConfig(id);

        // TODO: test validations
    }
    /**
     * Get display info for a Displayer plugin.
     *
     * 
     */
    @Test
    public void getPluginDisplayTest() {
        Long id = null;
        // String response = api.getPluginDisplay(id);

        // TODO: test validations
    }
    /**
     * Return all plugins.
     *
     * 
     */
    @Test
    public void getPluginsTest() {
        // List<PluginConf> response = api.getPlugins();

        // TODO: test validations
    }
    /**
     * Update YAML configuration for Configurer plugin.
     *
     * 
     */
    @Test
    public void updatePluginConfigTest() {
        Long id = null;
        // Void response = api.updatePluginConfig(id);

        // TODO: test validations
    }
}
