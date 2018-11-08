package com.github.gotify.service;

import android.os.Handler;
import com.github.gotify.Utils;
import com.github.gotify.api.Callback;
import com.github.gotify.api.CertUtils;
import com.github.gotify.client.JSON;
import com.github.gotify.client.model.Message;
import com.github.gotify.log.Log;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketConnection {
    private OkHttpClient client;
    private static final JSON gson = Utils.json();

    private final Handler handler = new Handler();
    private int errorCount = 0;

    private final String baseUrl;
    private final String token;
    private WebSocket webSocket;
    private Callback.SuccessCallback<Message> onMessage;
    private Runnable onClose;
    private Runnable onOpen;
    private BadRequestRunnable onBadRequest;
    private OnFailureCallback onFailure;
    private Runnable onReconnected;
    private boolean isClosed;

    WebSocketConnection(String baseUrl, CertUtils.SSLSettings settings, String token) {
        //        client = new ApiClient()
        //                .setVerifyingSsl(validateSSL)
        //                .setSslCaCert(Utils.stringToInputStream(cert))
        //                .getHttpClient();
        OkHttpClient.Builder builder =
                new OkHttpClient.Builder()
                        .readTimeout(0, TimeUnit.MILLISECONDS)
                        .pingInterval(1, TimeUnit.MINUTES)
                        .connectTimeout(10, TimeUnit.SECONDS);
        CertUtils.applySslSettings(builder, settings);

        client = builder.build();

        this.baseUrl = baseUrl;
        this.token = token;
    }

    synchronized WebSocketConnection onMessage(Callback.SuccessCallback<Message> onMessage) {
        this.onMessage = onMessage;
        return this;
    }

    synchronized WebSocketConnection onClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    synchronized WebSocketConnection onOpen(Runnable onOpen) {
        this.onOpen = onOpen;
        return this;
    }

    synchronized WebSocketConnection onBadRequest(BadRequestRunnable onBadRequest) {
        this.onBadRequest = onBadRequest;
        return this;
    }

    synchronized WebSocketConnection onFailure(OnFailureCallback onFailure) {
        this.onFailure = onFailure;
        return this;
    }

    synchronized WebSocketConnection onReconnected(Runnable onReconnected) {
        this.onReconnected = onReconnected;
        return this;
    }

    private Request request() {
        HttpUrl url =
                HttpUrl.parse(baseUrl)
                        .newBuilder()
                        .addPathSegment("stream")
                        .addQueryParameter("token", token)
                        .build();
        return new Request.Builder().url(url).get().build();
    }

    public synchronized WebSocketConnection start() {
        close();
        isClosed = false;
        Log.i("WebSocket: starting...");

        webSocket = client.newWebSocket(request(), new Listener());
        return this;
    }

    public synchronized void close() {
        if (webSocket != null) {
            Log.i("WebSocket: closing existing connection.");
            isClosed = true;
            webSocket.close(1000, "");
            webSocket = null;
        }
    }

    private class Listener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.i("WebSocket: opened");
            synchronized (this) {
                onOpen.run();

                if (errorCount > 0) {
                    onReconnected.run();
                    errorCount = 0;
                }
            }
            super.onOpen(webSocket, response);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.i("WebSocket: received message " + text);
            synchronized (this) {
                Message message = gson.deserialize(text, Message.class);
                onMessage.onSuccess(message);
            }
            super.onMessage(webSocket, text);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            synchronized (this) {
                if (!isClosed) {
                    Log.w("WebSocket: closed");
                    onClose.run();
                }
            }

            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            String code = response != null ? "StatusCode: " + response.code() : "";
            String message = response != null ? response.message() : "";
            Log.e("WebSocket: failure " + code + " Message: " + message, t);
            synchronized (this) {
                if (response != null && response.code() >= 400 && response.code() <= 499) {
                    onBadRequest.execute(message);
                    close();
                    return;
                }

                int minutes = errorCount * 5 + 1;

                Log.i("WebSocket: trying to restart in " + minutes + " minute(s)");

                errorCount++;
                handler.postDelayed(
                        WebSocketConnection.this::start, TimeUnit.MINUTES.toMillis(minutes));
                onFailure.execute(minutes);
            }

            super.onFailure(webSocket, t, response);
        }
    }

    interface BadRequestRunnable {
        void execute(String message);
    }

    interface OnFailureCallback {
        void execute(int minutesToTryAgain);
    }
}
