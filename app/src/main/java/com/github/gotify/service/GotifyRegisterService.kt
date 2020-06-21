package com.github.gotify.service

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import com.github.gotify.Settings
import com.github.gotify.api.Api
import com.github.gotify.api.ApiException
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.ApplicationApi
import com.github.gotify.log.Log
import kotlin.concurrent.thread

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */
// TODO : in the app, implement forceUnregisterApp

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class GotifyRegisterService : Service() {
    /** Keeps track of all current registered clients.  */
    private val db = MessagingDatabase(this)
    private lateinit var settings: Settings

    /**
     * Handler of incoming messages from clients.
     */
    internal inner class gHandler : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                TYPE_CLIENT_STARTED -> simpleAnswer(msg, TYPE_CLIENT_STARTED)
                TYPE_REGISTER_CLIENT -> {
                    val uid = msg.sendingUid
                    val msgData = msg.data
                    thread(start = true) {
                        registerApp(msgData, uid)
                    }.join()
                    sendInfo(msg)
                }
                TYPE_UNREGISTER_CLIENT -> {
                    val uid = msg.sendingUid
                    val msgData = msg.data
                    thread(start = true) {
                        unregisterApp(msgData, uid)
                    }
                    simpleAnswer(msg, TYPE_UNREGISTERED_CLIENT)
                }
                else -> super.handleMessage(msg)
            }
        }

        private fun simpleAnswer(msg: Message, what: Int) {
            try {
                msg.replyTo?.send(Message.obtain(null, what, 0, 0))
            } catch (e: RemoteException) {
            }
        }

        private fun unregisterApp(msg: Bundle, clientUid: Int) {
            val clientPackageName = msg.getString("package").toString()
            // we only trust unregistered demands from the uid who registered the app
            if (db.strictIsRegistered(clientPackageName, clientUid)) {
                Log.i("Unregistering $clientPackageName uid: $clientUid")
                deleteApp(clientPackageName)
                db.unregisterApp(clientPackageName, clientUid)
            }
        }

        private fun registerApp(msg: Bundle,clientUid: Int) {
            val clientPackageName = msg.getString("package").toString()
            if (clientPackageName.isBlank()) {
                Log.w("Trying to register an app without packageName")
                return
            }
            //TODO Send a notification to let the user acknowledge the registering
            Log.i("registering $clientPackageName uid: $clientUid")
            // The app is registered with the same uid : we re-register it
            // the client may need to create a new app in the server
            if (db.strictIsRegistered(clientPackageName, clientUid)) {
                Log.i("$clientPackageName already registered : unregistering to register again")
                unregisterApp(msg,clientUid)
            }
            // The app is registered with a new uid.
            // User should unregister this app manually
            // to avoid an app to impersonate another one
            if (db.isRegistered(clientPackageName)) {
                Log.w("$clientPackageName already registered with a different uid")
                return
            }
            //
            val clientService = msg.getString("service").toString()
            if (clientService.isBlank()) {
                Log.w("Cannot find the service for $clientPackageName")
                return
            }
            val app = createApp(clientPackageName)
            if(app == null){
                Log.w("Cannot create a new app to register")
                return
            }
            Log.i("registering : $clientPackageName $clientUid $clientService ${app.id} ${app.token}")
            db.registerApp(clientPackageName, clientUid, clientService, app.id, app.token)
        }

        private fun sendInfo(msg: Message){
            val clientPackageName = msg.data?.getString("package").toString()
            Log.i("$clientPackageName is asking for its token and url")
            // we only trust unregistered demands from the uid who registered the app
            if (db.strictIsRegistered(clientPackageName, msg.sendingUid)) {
                // db.getToken also remove the token in the db
                val token = db.getToken(clientPackageName)
                try {
                    val answer = Message.obtain(null, TYPE_REGISTERED_CLIENT, 0, 0)
                    answer.data = bundleOf("url" to settings.url(),
                                            "token" to token)
                    msg.replyTo?.send(answer)
                } catch (e: RemoteException) {
                }
            }else{
                Log.w("Client isn't registered or has a different uid")
            }
        }

    }

    /**
     * Target we publish for clients to send messages to gHandler.
     */
    private val gMessenger = Messenger(gHandler())

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    override fun onBind(intent: Intent): IBinder? {
        settings = Settings(this)
        return gMessenger.binder
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun createApp(appName: String): com.github.gotify.client.model.Application? {
        val client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token())
        val app = com.github.gotify.client.model.Application()
        app.name = appName
        app.description = "automatically created"
        try {
            return Api.execute(client.createService(ApplicationApi::class.java).createApp(app));
        } catch (e: ApiException) {
            Log.e("Could not create app.", e);
        }
        return null
    }

    private fun deleteApp(appName: String){
        //TODO Send a notification to let the user choosing if the app will be deleted or not
        val client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token())
        try {
            val appId = db.getAppId(appName)
            Log.i("appId: $appId")
            Api.execute(client.createService(ApplicationApi::class.java).deleteApp(appId));
        } catch (e: ApiException) {
            Log.e("Could not delete app.", e);
        }
    }
}