package com.github.gotify.service

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import com.github.gotify.Settings
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.ApplicationApi
import com.github.gotify.log.Log
import java.io.IOException

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */
// TODO : in the app, implement forceUnregisterApp

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class GotifyMessengerService : Service() {
    /** Keeps track of all current registered clients.  */
    private val db = MessagingDatabase(this)
    private lateinit var settings: Settings //= Settings(this)

    /**
     * Handler of incoming messages from clients.
     */
    internal inner class gHandler : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_START -> simpleAnswer(msg, MSG_START,0)
                MSG_REGISTER_CLIENT -> {
                    val uid = msg.sendingUid
                    val msgData = msg.data
                    val thread = object : Thread() {
                        override fun run() {
                            registerApp(msgData, uid)
                        }
                    }
                    thread.start()
                    thread.join()
                    simpleAnswer(msg, MSG_REGISTER_CLIENT,0)
                }
                MSG_UNREGISTER_CLIENT -> {
                    unregisterApp(msg.data,msg.sendingUid,true)
                    simpleAnswer(msg, MSG_UNREGISTER_CLIENT,0)
                }
                MSG_GET_INFO -> sendInfo(msg)
                MSG_IS_REGISTERED -> {
                    var rep =0
                    if(db.strictIsRegistered(msg.data.getString("package").toString(),msg.sendingUid)){
                        rep = 1
                    }
                    simpleAnswer(msg, MSG_IS_REGISTERED,rep)
                }
                else -> super.handleMessage(msg)
            }
        }

        private fun simpleAnswer(msg: Message, what: Int, arg1: Int) {
            try {
                msg.replyTo?.send(Message.obtain(null, what, arg1, 0))
            } catch (e: RemoteException) {
            }
        }

        private fun unregisterApp(msg: Bundle, clientUid: Int, deleteApp: Boolean) {
            val clientPackageName = msg.getString("package").toString()
            // we only trust unregistered demands from the uid who registered the app
            if (db.strictIsRegistered(clientPackageName, clientUid)) {
                Log.i("Unregistering $clientPackageName uid: $clientUid")
                db.unregisterApp(clientPackageName, clientUid)
                if(deleteApp){
                    // TODO : delete app on the server,
                    //  it can be done manually for the moment
                }
            }
        }

        private fun registerApp(msg: Bundle,clientUid: Int) {
            val clientPackageName = msg.getString("package").toString()
            if (clientPackageName.isBlank()) {
                Log.w("Trying to register an app without packageName")
                return
            }
            Log.i("registering $clientPackageName uid: $clientUid")
            // The app is registered with the same uid : we re-register it
            // the client may need to create a new app in the server
            if (db.strictIsRegistered(clientPackageName, clientUid)) {
                Log.i("$clientPackageName already registered : unregistering to register again")
                unregisterApp(msg,clientUid,false)
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
            var app = getApp(clientPackageName)
            if (app == null){
                Log.i("$clientPackageName isn't registered to the server, creating a new one")
                app = createApp(clientPackageName)
            }
            if(app == null){
                Log.w("Cannot find the AppId neither create a new app to register")
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
                val token = db.getToken(clientPackageName)
                try {
                    val answer = Message.obtain(null, MSG_GET_INFO, 0, 0)
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

    private fun getApp(appName: String): com.github.gotify.client.model.Application? {
        val client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token())
        var applications: List<com.github.gotify.client.model.Application>? = null
        try{
            applications = client.createService(ApplicationApi::class.java).apps.execute().body()
        }catch(e: IOException){
            e.printStackTrace()
        }
        if(applications.isNullOrEmpty()) {
            Log.i("There isn't any app registered to the server")
            return null
        }
        for (app in applications) {
            if (app.name == appName) return app
        }
        return null
    }

    private fun createApp(appName: String): com.github.gotify.client.model.Application? {
        val client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token())
        val app = com.github.gotify.client.model.Application()
        app.name = appName
        app.description = "automatically created"
        try{
            return client.createService(ApplicationApi::class.java).createApp(app).execute().body()
        }catch(e: IOException){
            e.printStackTrace()
        }
        return null
    }
}