package com.github.gotify.log

import com.github.gotify.log.Log.e

internal object UncaughtExceptionHandler {
    fun registerCurrentThread() {
        Thread.setDefaultUncaughtExceptionHandler { _, e: Throwable? ->
            e("uncaught exception", e!!)
        }
    }
}
