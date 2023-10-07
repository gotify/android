package com.github.gotify.log

import org.tinylog.kotlin.Logger

internal object UncaughtExceptionHandler {
    fun registerCurrentThread() {
        Thread.setDefaultUncaughtExceptionHandler { _, e: Throwable ->
            Logger.error(e, "uncaught exception")
        }
    }
}
