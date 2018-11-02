package com.github.gotify.log;

public class UncaughtExceptionHandler {
    public static void registerCurrentThread() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Log.e("uncaught exception", e));
    }
}
