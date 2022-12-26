package com.github.gotify.log

import android.content.Context
import com.hypertrack.hyperlog.LogFormat
import java.util.*

internal class Format(context: Context) : LogFormat(context) {
    override fun getFormattedLogMessage(
        logLevelName: String,
        tag: String,
        message: String,
        timeStamp: String,
        senderName: String,
        osVersion: String,
        deviceUuid: String
    ) = String.format(Locale.ENGLISH, "%s %s: %s", timeStamp, logLevelName, message)
}
