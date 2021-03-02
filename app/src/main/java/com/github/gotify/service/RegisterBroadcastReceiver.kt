package com.github.gotify.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.gotify.Settings
import com.github.gotify.api.Api
import com.github.gotify.api.ApiException
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.ApplicationApi
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.concurrent.thread

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */

class RegisterBroadcastReceiver: BroadcastReceiver() {

    private lateinit var settings: Settings

    private fun unregisterApp(db: MessagingDatabase, application: String, token: String) {
        // we only trust unregistered demands from the uid who registered the app
        if (db.strictIsRegistered(application, token)) {
            Log.i("RegisterService","Unregistering $application token: $token")
            deleteApp(db, application)
            db.unregisterApp(application, token)
        }
    }

    private fun registerApp(context: Context?, db: MessagingDatabase, application: String, connectorToken: String) {
        if (application.isBlank()) {
            Log.w("RegisterService","Trying to register an app without packageName")
            return
        }
        Log.i("RegisterService","registering $application token: $connectorToken")
        // The app is registered with the same token : we re-register it
        // the client may need its endpoint again
        if (db.strictIsRegistered(application, connectorToken)) {
            Log.i("RegisterService","$application already registered")
            return
        }

        val app = createApp(application)
        if(app == null){
            val message = "Cannot create a new app to register"
            Log.w("RegisterService", message)
            sendRegistrationFailed(context!!,application,connectorToken,message)
            return
        }
        db.registerApp(application, app.id, app.token, connectorToken)
    }

    private fun createApp(appName: String): com.github.gotify.client.model.Application? {
        val client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token())
        val app = com.github.gotify.client.model.Application()
        app.name = appName
        val date = SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Date())
        app.description = "(auto) $date"
        try {
            Log.i("RegisterService","Creating app")
            return Api.execute(client.createService(ApplicationApi::class.java).createApp(app))
        } catch (e: ApiException) {
            Log.e("RegisterService","Could not create app.", e)
        }
        return null
    }

    private fun deleteApp(db: MessagingDatabase, appName: String){
        val client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token())
        try {
            val appId = db.getAppId(appName)
            Log.i("RegisterService","Deleting app with appId=$appId")
            Api.execute(client.createService(ApplicationApi::class.java).deleteApp(appId))
        } catch (e: ApiException) {
            Log.e("RegisterService","Could not delete app.", e)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        settings = Settings(context)
        when (intent!!.action) {
            ACTION_REGISTER ->{
                val db = MessagingDatabase(context!!)
                Log.i("Register","REGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: ""
                thread(start = true) {
                    registerApp(context, db, application, connectorToken)
                    Log.i("RegisterService","Registration is finished")
                }.join()
                val token = db.getGotifyToken(connectorToken)
                db.close()
                val endpoint = settings.url() +
                        "/UP?token=$token"
                sendEndpoint(context, connectorToken, endpoint)
            }
            ACTION_UNREGISTER ->{
                Log.i("Register","UNREGISTER")
                val token = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: ""
                thread(start = true) {
                    val db = MessagingDatabase(context!!)
                    unregisterApp(db,application, token)
                    db.close()
                    Log.i("RegisterService","Unregistration is finished")
                }
                sendUnregistered(context!!, token)
            }
        }
    }
}