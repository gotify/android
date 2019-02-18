package com.github.gotify.client.api;

import com.github.gotify.client.ApiClient;
import com.github.gotify.client.model.Error;
import com.github.gotify.client.model.PluginConf;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Integer id = null;
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
        Integer id = null;
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
        Integer id = null;
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
        Integer id = null;
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
        Integer id = null;
        // Void response = api.updatePluginConfig(id);

        // TODO: test validations
    }
}
