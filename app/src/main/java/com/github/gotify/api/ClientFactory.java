package com.github.gotify.api;

import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.UserApi;
import com.github.gotify.client.api.VersionApi;
import com.github.gotify.client.auth.ApiKeyAuth;
import com.github.gotify.client.auth.HttpBasicAuth;

public class ClientFactory {
    public static ApiClient unauthorized(String baseUrl, boolean validateSSL, String cert) {
        ApiClient client = new ApiClient();
        client.setVerifyingSsl(validateSSL);
        client.setSslCaCert(Utils.stringToInputStream(cert));
        client.setBasePath(baseUrl);
        return client;
    }

    public static ApiClient basicAuth(String baseUrl, boolean validateSSL, String cert, String username, String password) {
        ApiClient client = unauthorized(baseUrl, validateSSL, cert);
        HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("basicAuth");
        auth.setUsername(username);
        auth.setPassword(password);

        return client;
    }

    public static ApiClient clientToken(String baseUrl, boolean validateSSL, String cert, String token) {
        ApiClient client = unauthorized(baseUrl, validateSSL, cert);
        ApiKeyAuth tokenAuth = (ApiKeyAuth) client.getAuthentication("clientTokenHeader");
        tokenAuth.setApiKey(token);
        return client;
    }

    public static VersionApi versionApi(String baseUrl, boolean validateSSL, String cert) {
        return new VersionApi(unauthorized(baseUrl, validateSSL, cert));
    }

    public static UserApi userApiWithToken(Settings settings) {
        return new UserApi(clientToken(settings.url(), settings.validateSSL(), settings.cert(), settings.token()));
    }
}
