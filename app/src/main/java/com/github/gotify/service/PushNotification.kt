package com.github.gotify.service


import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * These functions are used to send messages to other apps
 */

fun sendMessage(context: Context, application: String, message: String){
    val token = getToken(context,application)!!
    val broadcastIntent = Intent()
    broadcastIntent.`package` = application
    broadcastIntent.action = MESSAGE
    broadcastIntent.putExtra("token", token)
    broadcastIntent.putExtra("message", message)
    context.sendBroadcast(broadcastIntent)
}

fun sendEndpoint(context: Context, application: String, endpoint: String) {
    val token = getToken(context,application)!!
    val broadcastIntent = Intent()
    broadcastIntent.`package` = application
    broadcastIntent.action = NEW_ENDPOINT
    broadcastIntent.putExtra("token", token)
    broadcastIntent.putExtra("endpoint", endpoint)
    context.sendBroadcast(broadcastIntent)
}

fun sendUnregistered(context: Context, application: String, _token: String?){
    val token = _token?: getToken(context,application)!!
    val broadcastIntent = Intent()
    broadcastIntent.`package` = application
    broadcastIntent.action = UNREGISTERED
    broadcastIntent.putExtra("token", token)
    context.sendBroadcast(broadcastIntent)
}

fun getToken(context: Context, application: String): String?{
    val db = MessagingDatabase(context)
    val token = db.getConnectorToken(application)
    db.close()
    if (token.isBlank()) {
        Log.w("notifyClient", "No token found for $application")
        return null
    }
    return token
}