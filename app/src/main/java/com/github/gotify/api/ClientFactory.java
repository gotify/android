package com.github.gotify.api;

import com.github.gotify.Settings;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.UserApi;
import com.github.gotify.client.api.VersionApi;
import com.github.gotify.client.auth.ApiKeyAuth;
import com.github.gotify.client.auth.HttpBasicAuth;

public class ClientFactory {
    public static ApiClient unauthorized(String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        return client;
    }

    public static ApiClient basicAuth(String baseUrl, String username, String password) {
        ApiClient client = unauthorized(baseUrl);
        HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("basicAuth");
        auth.setUsername(username);
        auth.setPassword(password);

        return client;
    }

    public static ApiClient clientToken(String baseUrl, String token) {
        ApiClient client = unauthorized(baseUrl);
        ApiKeyAuth tokenAuth = (ApiKeyAuth) client.getAuthentication("clientTokenHeader");
        tokenAuth.setApiKey(token);
        return client;
    }

    public static VersionApi versionApi(String baseUrl) {
        return new VersionApi(unauthorized(baseUrl));
    }

    public static UserApi userApiWithToken(Settings settings) {
        return new UserApi(clientToken(settings.url(), settings.token()));
    }
}
