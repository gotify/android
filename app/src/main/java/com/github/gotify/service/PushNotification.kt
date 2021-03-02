package com.github.gotify.service


import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * These functions are used to send messages to other apps
 */

fun sendMessage(context: Context, token: String, message: String){
    val application = getApp(context, token)!!
    val broadcastIntent = Intent()
    broadcastIntent.`package` = application
    broadcastIntent.action = ACTION_MESSAGE
    broadcastIntent.putExtra(EXTRA_TOKEN, token)
    broadcastIntent.putExtra(EXTRA_MESSAGE, message)
    context.sendBroadcast(broadcastIntent)
}

fun sendEndpoint(context: Context, token: String, endpoint: String) {
    val application = getApp(context, token)!!
    val broadcastIntent = Intent()
    broadcastIntent.`package` = application
    broadcastIntent.action = ACTION_NEW_ENDPOINT
    broadcastIntent.putExtra(EXTRA_TOKEN, token)
    broadcastIntent.putExtra(EXTRA_ENDPOINT, endpoint)
    context.sendBroadcast(broadcastIntent)
}

fun sendUnregistered(context: Context, token: String){
    val application = getApp(context, token)
    val broadcastIntent = Intent()
    broadcastIntent.`package` = application
    broadcastIntent.action = ACTION_UNREGISTERED
    broadcastIntent.putExtra(EXTRA_TOKEN, token)
    context.sendBroadcast(broadcastIntent)
}

fun sendRegistrationFailed(context: Context, application: String, token: String, message: String){
    val broadcastIntent = Intent()
    broadcastIntent.`package` = application
    broadcastIntent.action = ACTION_REGISTRATION_FAILED
    broadcastIntent.putExtra(EXTRA_TOKEN, token)
    broadcastIntent.putExtra(EXTRA_MESSAGE, message)
    context.sendBroadcast(broadcastIntent)
}

fun sendRegistrationRefused(context: Context, application: String, token: String, message: String) {
    val broadcastIntent = Intent()
    broadcastIntent.`package` = application
    broadcastIntent.action = ACTION_REGISTRATION_REFUSED
    broadcastIntent.putExtra(EXTRA_TOKEN, token)
    broadcastIntent.putExtra(EXTRA_MESSAGE, message)
    context.sendBroadcast(broadcastIntent)
}

fun getApp(context: Context, token: String): String?{
    val db = MessagingDatabase(context)
    val application = db.getAppFromToken(token)
    db.close()
    if (application.isBlank()) {
        Log.w("getApp", "No app found for $token")
        return null
    }
    return application
}