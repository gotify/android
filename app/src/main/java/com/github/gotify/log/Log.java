package com.github.gotify.log;

import android.content.Context;
import android.text.TextUtils;
import com.hypertrack.hyperlog.HyperLog;
import java.util.Collections;
import java.util.List;

public class Log {
    private static String TAG = "gotify";

    public static void init(Context content) {
        HyperLog.initialize(content, new Format(content));
        HyperLog.setLogLevel(android.util.Log.INFO); // TODO configurable
    }

    public static String get() {
        List<String> logs = HyperLog.getDeviceLogsAsStringList(false);
        Collections.reverse(logs);
        return TextUtils.join("\n", logs.subList(0, Math.min(200, logs.size())));
    }

    public static void e(String message) {
        HyperLog.e(TAG, message);
    }

    public static void e(String message, Throwable e) {
        HyperLog.e(TAG, message + '\n' + android.util.Log.getStackTraceString(e));
    }

    public static void i(String message) {
        HyperLog.i(TAG, message);
    }

    public static void i(String message, Throwable e) {
        HyperLog.i(TAG, message + '\n' + android.util.Log.getStackTraceString(e));
    }

    public static void w(String message) {
        HyperLog.w(TAG, message);
    }

    public static void w(String message, Throwable e) {
        HyperLog.w(TAG, message + '\n' + android.util.Log.getStackTraceString(e));
    }

    public static void clear() {
        HyperLog.deleteLogs();
    }
}
