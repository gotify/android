package com.github.gotify;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class LogManager extends ReactContextBaseJavaModule {
    LogManager(final ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "LogManager";
    }

    @ReactMethod
    public void clear(Callback callback) {
        Log.get().clear();
        callback.invoke();
    }

    @ReactMethod
    public void getLog(Callback callback) {
        StringBuilder log = new StringBuilder();
        for (String line : Log.get()) {
            log.append(line).append("\n");
        }
        callback.invoke(log.toString());
    }
}
