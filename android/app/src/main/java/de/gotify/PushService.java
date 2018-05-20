package de.gotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class PushService extends Service {
    private final OkHttpClient client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build();
    public Handler handler = null;
    private WebSocket socket = null;
    private Gson gson = null;

    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (socket != null) {
                Log.i("Closing WebSocket (preference change)");
                socket.close(1000, "client logout");
                socket = null;
            }
            start();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        Log.i("Creating WebSocket-Service");

        handler = new Handler();
        gson = new Gson();
        start();
        // https://github.com/sriraman/react-native-shared-preferences/issues/12 for why wit_player_shared_preferences
        final SharedPreferences preferences = this.getSharedPreferences("wit_player_shared_preferences", Context.MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private void foregroundNotification(String message) {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "GOTIFY_CHANNEL")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Gotify")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentText(message)
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
    }

    private void start() {
        final SharedPreferences preferences = this.getSharedPreferences("wit_player_shared_preferences", Context.MODE_PRIVATE);
        String url = preferences.getString("@global:url", null);
        String token = preferences.getString("@global:token", null);


        if (url == null || token == null) {
            foregroundNotification("login required");
            return;
        }

        HttpUrl httpUrl = HttpUrl.parse(url).newBuilder().addPathSegment("stream").addQueryParameter("token", token).build();

        final Request request = new Request.Builder().url(httpUrl).get().build();

        foregroundNotification("Initializing WebSocket");
        Log.i("Initializing WebSocket");
        socket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.i("Initialized WebSocket");
                foregroundNotification("Listening to " + request.url().host());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Map<String, String> hashMap = gson.fromJson(text, new TypeToken<Map<String, String>>() {
                }.getType());
                showNotification(Integer.parseInt(hashMap.get("id")), hashMap.get("title"), hashMap.get("message"));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.e("WebSocket closed " + reason);
                foregroundNotification("WebSocket closed, re-login required");
                showNotification(-4, "WebSocket closed", "The WebSocket connection closed, this normally means the token(login) was invalidated. A re-login is required");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                foregroundNotification("Error: " + t.getMessage());
                Log.e("WebSocket failure", t);
                if (response != null && response.code() >= 400 && response.code() <= 499) {
                    preferences.edit().remove("@global:token").apply();
                    showNotification(-2, "WebSocket Bad-Request", "Could not connect: " + response.message());
                    return;
                }

                showNotification(-3, "WS Conn Failed", "The websocket connection failed, trying again in a minute: " + t.getMessage());

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        start();
                    }
                }, TimeUnit.MINUTES.toMillis(1));
            }
        });
    }

    private void showNotification(int id, String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, "GOTIFY_CHANNEL");

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setTicker("Gotify - " + title)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setContentIntent(contentIntent);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, b.build());
    }

    @Override
    public void onDestroy() {
        Log.i("Destroying WebSocket-Service");
        super.onDestroy();
    }
}
