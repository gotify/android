package com.github.gotify.log;

import android.content.Context;
import com.hypertrack.hyperlog.LogFormat;
import java.util.Locale;

public class Format extends LogFormat {
    Format(Context context) {
        super(context);
    }

    @Override
    public String getFormattedLogMessage(
            String logLevelName,
            String tag,
            String message,
            String timeStamp,
            String senderName,
            String osVersion,
            String deviceUUID) {
        return String.format(Locale.ENGLISH, "%s %s: %s", timeStamp, logLevelName, message);
    }
}
