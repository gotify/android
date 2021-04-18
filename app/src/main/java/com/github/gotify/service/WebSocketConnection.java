package com.github.gotify.service;

import android.app.AlarmManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import com.github.gotify.SSLSettings;
import com.github.gotify.Utils;
import com.github.gotify.api.Callback;
import com.github.gotify.api.CertUtils;
import com.github.gotify.client.model.Message;
import com.github.gotify.log.Log;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

class WebSocketConnection {
    private static final AtomicLong ID = new AtomicLong(0);
    private final ConnectivityManager connectivityManager;
    private final AlarmManager alarmManager;
    private OkHttpClient client;

    private final Handler reconnectHandler = new Handler();
    private Runnable reconnectCallback = this::start;
    private int errorCount = 0;

    private final String baseUrl;
    private final String token;
    private WebSocket webSocket;
    private Callback.SuccessCallback<Message> onMessage;
    private Runnable onClose;
    private Runnable onOpen;
    private BadRequestRunnable onBadRequest;
    private OnNetworkFailureRunnable onNetworkFailure;
    private Runnable onReconnected;
    private State state;

    WebSocketConnection(
            String baseUrl,
            SSLSettings settings,
            String token,
            ConnectivityManager connectivityManager,
            AlarmManager alarmManager) {
        this.connectivityManager = connectivityManager;
        this.alarmManager = alarmManager;
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

    synchronized WebSocketConnection onNetworkFailure(OnNetworkFailureRunnable onNetworkFailure) {
        this.onNetworkFailure = onNetworkFailure;
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
        if (state == State.Connecting || state == State.Connected) {
            return this;
        }
        close();
        state = State.Connecting;
        long nextId = ID.incrementAndGet();
        Log.i("WebSocket(" + nextId + "): starting...");

        webSocket = client.newWebSocket(request(), new Listener(nextId));
        return this;
    }

    public synchronized void close() {
        if (webSocket != null) {
            Log.i("WebSocket(" + ID.get() + "): closing existing connection.");
            state = State.Disconnected;
            webSocket.close(1000, "");
            webSocket = null;
        }
    }

    public synchronized void scheduleReconnect(long seconds) {
        if (state == State.Connecting || state == State.Connected) {
            return;
        }
        state = State.Scheduled;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.i(
                    "WebSocket: scheduling a restart in "
                            + seconds
                            + " second(s) (via alarm manager)");
            final Calendar future = Calendar.getInstance();
            future.add(Calendar.SECOND, (int) seconds);
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    future.getTimeInMillis(),
                    "reconnect-tag",
                    this::start,
                    null);
        } else {
            Log.i("WebSocket: scheduling a restart in " + seconds + " second(s)");
            reconnectHandler.removeCallbacks(reconnectCallback);
            reconnectHandler.postDelayed(reconnectCallback, TimeUnit.SECONDS.toMillis(seconds));
        }
    }

    private class Listener extends WebSocketListener {
        private final long id;

        public Listener(long id) {
            this.id = id;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            syncExec(
                    () -> {
                        state = State.Connected;
                        Log.i("WebSocket(" + id + "): opened");
                        onOpen.run();

                        if (errorCount > 0) {
                            onReconnected.run();
                            errorCount = 0;
                        }
                    });
            super.onOpen(webSocket, response);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            syncExec(
                    () -> {
                        Log.i("WebSocket(" + id + "): received message " + text);
                        Message message = Utils.JSON.fromJson(text, Message.class);
                        onMessage.onSuccess(message);
                    });
            super.onMessage(webSocket, text);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            syncExec(
                    () -> {
                        if (state == State.Connected) {
                            Log.w("WebSocket(" + id + "): closed");
                            onClose.run();
                        }
                        state = State.Disconnected;
                    });

            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            String code = response != null ? "StatusCode: " + response.code() : "";
            String message = response != null ? response.message() : "";
            Log.e("WebSocket(" + id + "): failure " + code + " Message: " + message, t);
            syncExec(
                    () -> {
                        state = State.Disconnected;
                        if (response != null && response.code() >= 400 && response.code() <= 499) {
                            onBadRequest.execute(message);
                            close();
                            return;
                        }

                        errorCount++;

                        NetworkInfo network = connectivityManager.getActiveNetworkInfo();
                        if (network == null || !network.isConnected()) {
                            Log.i("WebSocket(" + id + "): Network not connected");
                        }

                        int minutes = Math.min(errorCount * 2 - 1, 20);

                        onNetworkFailure.execute(minutes);
                        scheduleReconnect(TimeUnit.MINUTES.toSeconds(minutes));
                    });

            super.onFailure(webSocket, t, response);
        }

        private void syncExec(Runnable runnable) {
            synchronized (this) {
                if (ID.get() == id) {
                    runnable.run();
                }
            }
        }
    }

    interface BadRequestRunnable {
        void execute(String message);
    }

    interface OnNetworkFailureRunnable {
        void execute(long millis);
    }

    enum State {
        Scheduled,
        Connecting,
        Connected,
        Disconnected
    }
}
