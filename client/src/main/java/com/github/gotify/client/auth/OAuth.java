package com.github.gotify.client.auth;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

import java.io.IOException;
import java.util.Map;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.AuthenticationRequestBuilder;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.TokenRequestBuilder;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.token.BasicOAuthToken;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

public class OAuth implements Interceptor {

    public interface AccessTokenListener {
        public void notify(BasicOAuthToken token);
    }

    private volatile String accessToken;
    private OAuthClient oauthClient;

    private TokenRequestBuilder tokenRequestBuilder;
    private AuthenticationRequestBuilder authenticationRequestBuilder;

    private AccessTokenListener accessTokenListener;

    public OAuth( OkHttpClient client, TokenRequestBuilder requestBuilder ) {
        this.oauthClient = new OAuthClient(new OAuthOkHttpClient(client));
        this.tokenRequestBuilder = requestBuilder;
    }

    public OAuth(TokenRequestBuilder requestBuilder ) {
        this(new OkHttpClient(), requestBuilder);
    }

    public OAuth(OAuthFlow flow, String authorizationUrl, String tokenUrl, String scopes) {
        this(OAuthClientRequest.tokenLocation(tokenUrl).setScope(scopes));
        setFlow(flow);
        authenticationRequestBuilder = OAuthClientRequest.authorizationLocation(authorizationUrl);
    }

    public void setFlow(OAuthFlow flow) {
        switch(flow) {
        case accessCode:
        case implicit:
            tokenRequestBuilder.setGrantType(GrantType.AUTHORIZATION_CODE);
            break;
        case password:
            tokenRequestBuilder.setGrantType(GrantType.PASSWORD);
            break;
        case application:
            tokenRequestBuilder.setGrantType(GrantType.CLIENT_CREDENTIALS);
            break;
        default:
            break;
        }            
    }

    @Override
    public Response intercept(Chain chain)
            throws IOException {

        return retryingIntercept(chain, true);
    }

    private Response retryingIntercept(Chain chain, boolean updateTokenAndRetryOnAuthorizationFailure) throws IOException {
        Request request = chain.request();

        // If the request already have an authorization (eg. Basic auth), do nothing
        if (request.header("Authorization") != null) {
            return chain.proceed(request);
        }

        // If first time, get the token
        OAuthClientRequest oAuthRequest;
        if (getAccessToken() == null) {
            updateAccessToken(null);
        }

        if (getAccessToken() != null) {
            // Build the request
            Builder rb = request.newBuilder();

            String requestAccessToken = new String(getAccessToken());
            try {
                oAuthRequest = new OAuthBearerClientRequest(request.url().toString())
                        .setAccessToken(requestAccessToken)
                        .buildHeaderMessage();
            } catch (OAuthSystemException e) {
                throw new IOException(e);
            }

            for ( Map.Entry<String, String> header : oAuthRequest.getHeaders().entrySet() ) {
                rb.addHeader(header.getKey(), header.getValue());
            }
            rb.url( oAuthRequest.getLocationUri());

            //Execute the request
            Response response = chain.proceed(rb.build());

            // 401/403 most likely indicates that access token has expired. Unless it happens two times in a row.
            if ( response != null && (response.code() == HTTP_UNAUTHORIZED || response.code() == HTTP_FORBIDDEN) && updateTokenAndRetryOnAuthorizationFailure ) {
                if (response != null && (response.code() == HTTP_UNAUTHORIZED || response.code() == HTTP_FORBIDDEN) && updateTokenAndRetryOnAuthorizationFailure) {
                    try {
                        if (updateAccessToken(requestAccessToken)) {
                            response.body().close();
                            return retryingIntercept(chain, false);
                        }
                    } catch (Exception e) {
                        response.body().close();
                        throw e;
                    }
                }
            }
            return response;
        } else {
            return chain.proceed(chain.request());
        }
    }

    /*
     * Returns true if the access token has been updated
     */
    public synchronized boolean updateAccessToken(String requestAccessToken) throws IOException {
        if (getAccessToken() == null || getAccessToken().equals(requestAccessToken)) {    
            try {
                OAuthJSONAccessTokenResponse accessTokenResponse = oauthClient.accessToken(this.tokenRequestBuilder.buildBodyMessage());
                if (accessTokenResponse != null && accessTokenResponse.getAccessToken() != null) {
                    setAccessToken(accessTokenResponse.getAccessToken());
                    if (accessTokenListener != null) {
                        accessTokenListener.notify((BasicOAuthToken) accessTokenResponse.getOAuthToken());
                    }
                    return !getAccessToken().equals(requestAccessToken);
                } else {
                    return false;
                }
            } catch (OAuthSystemException e) {
                throw new IOException(e);
            } catch (OAuthProblemException e) {
                throw new IOException(e);
            }
        }
        return true;
    }

    public void registerAccessTokenListener(AccessTokenListener accessTokenListener) {
        this.accessTokenListener = accessTokenListener;
    }

    public synchronized String getAccessToken() {
        return accessToken;
    }

    public synchronized void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public TokenRequestBuilder getTokenRequestBuilder() {
        return tokenRequestBuilder;
    }

    public void setTokenRequestBuilder(TokenRequestBuilder tokenRequestBuilder) {
        this.tokenRequestBuilder = tokenRequestBuilder;
    }

    public AuthenticationRequestBuilder getAuthenticationRequestBuilder() {
        return authenticationRequestBuilder;
    }

    public void setAuthenticationRequestBuilder(AuthenticationRequestBuilder authenticationRequestBuilder) {
        this.authenticationRequestBuilder = authenticationRequestBuilder;
    }

}
