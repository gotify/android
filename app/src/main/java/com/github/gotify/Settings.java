package com.github.gotify;

import android.content.Context;
import android.content.SharedPreferences;
import com.github.gotify.client.model.User;

public class Settings {
    private final SharedPreferences sharedPreferences;

    public Settings(Context context) {
        sharedPreferences = context.getSharedPreferences("gotify", Context.MODE_PRIVATE);
    }

    public void url(String url) {
        sharedPreferences.edit().putString("url", url).apply();
    }

    public String url() {
        return sharedPreferences.getString("url", null);
    }

    public boolean tokenExists() {
        return token() != null;
    }

    public String token() {
        return sharedPreferences.getString("token", null);
    }

    public void token(String token) {
        sharedPreferences.edit().putString("token", token).apply();
    }

    public void clear() {
        url(null);
        token(null);
        validateSSL(true);
        cert(null);
    }

    public void user(String name, boolean admin) {
        sharedPreferences.edit().putString("username", name).putBoolean("admin", admin).apply();
    }

    public User user() {
        String username = sharedPreferences.getString("username", null);
        boolean admin = sharedPreferences.getBoolean("admin", false);
        if (username != null) {
            return new User().name(username).admin(admin);
        } else {
            return new User().name("UNKNOWN").admin(false);
        }
    }

    public String serverVersion() {
        return sharedPreferences.getString("version", "UNKNOWN");
    }

    public void serverVersion(String version) {
        sharedPreferences.edit().putString("version", version).apply();
    }

    private boolean validateSSL() {
        return sharedPreferences.getBoolean("validateSSL", true);
    }

    public void validateSSL(boolean validateSSL) {
        sharedPreferences.edit().putBoolean("validateSSL", validateSSL).apply();
    }

    private String cert() {
        return sharedPreferences.getString("cert", null);
    }

    public void cert(String cert) {
        sharedPreferences.edit().putString("cert", cert).apply();
    }

    public SSLSettings sslSettings() {
        return new SSLSettings(validateSSL(), cert());
    }
}
