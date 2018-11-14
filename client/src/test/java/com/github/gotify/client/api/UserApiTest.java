package com.github.gotify.client.api;

import com.github.gotify.client.ApiClient;
import com.github.gotify.client.model.Error;
import com.github.gotify.client.model.User;
import com.github.gotify.client.model.UserPass;
import com.github.gotify.client.model.UserWithPass;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for UserApi
 */
public class UserApiTest {

    private UserApi api;

    @Before
    public void setup() {
        api = new ApiClient().createService(UserApi.class);
    }

    /**
     * Create a user.
     *
     * 
     */
    @Test
    public void createUserTest() {
        UserWithPass body = null;
        // User response = api.createUser(body);

        // TODO: test validations
    }
    /**
     * Return the current user.
     *
     * 
     */
    @Test
    public void currentUserTest() {
        // User response = api.currentUser();

        // TODO: test validations
    }
    /**
     * Deletes a user.
     *
     * 
     */
    @Test
    public void deleteUserTest() {
        Integer id = null;
        // Void response = api.deleteUser(id);

        // TODO: test validations
    }
    /**
     * Get a user.
     *
     * 
     */
    @Test
    public void getUserTest() {
        Integer id = null;
        // User response = api.getUser(id);

        // TODO: test validations
    }
    /**
     * Return all users.
     *
     * 
     */
    @Test
    public void getUsersTest() {
        // List<User> response = api.getUsers();

        // TODO: test validations
    }
    /**
     * Update the password of the current user.
     *
     * 
     */
    @Test
    public void updateCurrentUserTest() {
        UserPass body = null;
        // Void response = api.updateCurrentUser(body);

        // TODO: test validations
    }
    /**
     * Update a user.
     *
     * 
     */
    @Test
    public void updateUserTest() {
        Integer id = null;
        UserWithPass body = null;
        // User response = api.updateUser(id, body);

        // TODO: test validations
    }
}
