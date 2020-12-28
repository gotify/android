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

    private fun registerApp(db: MessagingDatabase, application: String, connector_token: String) {
        if (application.isBlank()) {
            Log.w("RegisterService","Trying to register an app without packageName")
            return
        }
        Log.i("RegisterService","registering $application token: $connector_token")
        // The app is registered with the same token : we re-register it
        // the client may need its endpoint again
        if (db.strictIsRegistered(application, connector_token)) {
            Log.i("RegisterService","$application already registered : unregistering to register again")
            unregisterApp(db,application,connector_token)
        }
        // The app is registered with a new token.
        // User should unregister this app manually
        // to avoid an app to impersonate another one
        if (db.isRegistered(application)) {
            Log.w("RegisterService","$application already registered with a different token")
            return
        }
        val app = createApp(application)
        if(app == null){
            Log.w("RegisterService","Cannot create a new app to register")
            return
        }
        db.registerApp(application, app.id, app.token, connector_token)
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
            REGISTER ->{
                val db = MessagingDatabase(context!!)
                Log.i("Register","REGISTER")
                val internalToken = intent.getStringExtra("token")?: ""
                val application = intent.getStringExtra("application")?: ""
                thread(start = true) {
                    registerApp(db, application, internalToken)
                    Log.i("RegisterService","Registration is finished")
                }.join()
                val token = db.getGotifyToken(application, false)
                val endpoint = settings.url() +
                        "/UP?token=$token"
                sendEndpoint(context,application,endpoint)
                db.close()
            }
            UNREGISTER ->{
                Log.i("Register","UNREGISTER")
                val token = intent.getStringExtra("token")?: ""
                val application = intent.getStringExtra("application")?: ""
                thread(start = true) {
                    val db = MessagingDatabase(context!!)
                    unregisterApp(db,application, token)
                    db.close()
                    Log.i("RegisterService","Unregistration is finished")
                }
                sendUnregistered(context!!,application,false)
            }
        }
    }
}