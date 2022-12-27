package com.github.gotify.log

import android.content.Context
import android.util.Log
import com.hypertrack.hyperlog.HyperLog

internal object Log {
    private const val TAG = "gotify"

    fun init(content: Context) {
        HyperLog.initialize(content, Format(content))
        HyperLog.setLogLevel(Log.INFO) // TODO configurable
    }

    fun get(): String {
        val logs = HyperLog.getDeviceLogsAsStringList(false)
        return logs.takeLast(200).joinToString("\n")
    }

    fun e(message: String) {
        HyperLog.e(TAG, message)
    }

    fun e(message: String, e: Throwable) {
        HyperLog.e(TAG, "$message\n${Log.getStackTraceString(e)}")
    }

    fun i(message: String) {
        HyperLog.i(TAG, message)
    }

    fun i(message: String, e: Throwable) {
        HyperLog.i(TAG, "$message\n${Log.getStackTraceString(e)}")
    }

    fun w(message: String) {
        HyperLog.w(TAG, message)
    }

    fun w(message: String, e: Throwable) {
        HyperLog.w(TAG, "$message\n${Log.getStackTraceString(e)}")
    }

    fun clear() {
        HyperLog.deleteLogs()
    }
}
