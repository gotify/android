package de.gotify;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Log {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
    private static final List<String> LOG = Collections.synchronizedList(new ArrayList<String>());
    private static final String TAG = "gotify";

    public static void i(String message) {
        i(message, null);
    }

    public static List<String> get() {
        return LOG;
    }

    public static void i(String message, Throwable throwable) {
        log("INFO", message, throwable);
        android.util.Log.i(TAG, message, throwable);
    }

    public static void e(String message) {
        e(message, null);
    }

    public static void e(String message, Throwable throwable) {
        log("ERROR", message, throwable);
        android.util.Log.e("gotify", message, throwable);
    }

    private static void log(String type, String message, Throwable exception) {
        if (exception == null) {
            LOG.add(String.format("%s: %s - %s", type, FORMAT.format(new Date()), message));
        } else {
            LOG.add(String.format("%s: %s - %s%s%s", type, FORMAT.format(new Date()), message, "\n", android.util.Log.getStackTraceString(exception)));
        }
    }
}
