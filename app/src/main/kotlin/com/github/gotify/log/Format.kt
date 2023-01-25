package com.github.gotify.log

import android.content.Context
import com.hypertrack.hyperlog.LogFormat

internal class Format(context: Context) : LogFormat(context) {
    override fun getFormattedLogMessage(
        logLevelName: String,
        tag: String,
        message: String,
        timeStamp: String,
        senderName: String,
        osVersion: String,
        deviceUuid: String
    ) = "$timeStamp $logLevelName: $message"
}
