package com.github.gotify.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.core.os.bundleOf
import com.github.gotify.log.Log

/**
 * THIS CLASS IS USED TO PUSH NOTIFICATIONS TO OTHER APPS
 * It is called from the thread in WebSocketService
 */


//TODO : delete the notification once delivered
//TODO: Continue to implement the URl change
// For the moment, re-registering the client is enough

/**
 * Function to notify client
 */
fun notifyClient(context: Context,app: String, msg: com.github.gotify.client.model.Message){
    val gotif = GotifyPushNotification(context,app,msg)
    gotif.start()
}

open class GotifyPushNotification(private val context: Context,
                                  private val clientPackage: String,
                                  private val message: com.github.gotify.client.model.Message){
    /** Messenger for communicating with service.  */
    private var gService: Messenger? = null
    /** To known if it if bound to the service */
    private var gIsBound = false
    /** Handler of incoming messages from service. */
    private val gHandlerThread = HandlerThread(clientPackage)
    private var gHandler: Handler? = null
    /** Target we publish for client apps to send messages to gHandler. */
    private var gMessenger: Messenger? = null

    fun start(){
        gHandlerThread.start()
        gHandler = Handler(gHandlerThread.looper)
        gMessenger = Messenger(gHandler)
        doBindService()
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private val gConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            gService = Messenger(service)
            gIsBound = true
            Log.i("Remote service connected")
            /** We're connected, now we send the message !**/
            doNotifyClient()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            gService = null
            doUnbindService()
            Log.i("Remote service disconnected")
        }
    }

    private fun doBindService() {
        val intent = Intent()
        val db = MessagingDatabase(context)
        val service = db.getServiceName(clientPackage)
        if (service.isBlank()){
            Log.w("No service found for $clientPackage")
            doUnbindService()
            return
        }
        intent.component = ComponentName(clientPackage , service)
        context.bindService( intent, gConnection, Context.BIND_AUTO_CREATE )
            // We don't do if(true){gIsBound = true} now because
            // we consider it is bound only when the service is connected
    }

    private fun doUnbindService() {
        Log.i("Unbinding")
        if (gIsBound) {
            // Detach our existing connection.
            context.unbindService(gConnection)
            gIsBound = false
        }
        gHandlerThread.quit()
        gHandler = null
        gMessenger = null
    }

    private fun doChangeURL(url: String){
        if(!gIsBound){
            Log.w("You need to bind fisrt")
            doUnbindService()
            return
        }
        try {
            val msg = Message.obtain(null,
                MSG_NEW_URL, 0, 0)
            msg.replyTo = gMessenger
            msg.data = bundleOf("url" to url)
            gService!!.send(msg)
        } catch (e: RemoteException) {
            // There is nothing special we need to do if the service
            // has crashed.
        }
        //done : unbinding
        doUnbindService()
    }
    private fun doNotifyClient() {
        if (!gIsBound) {
            Log.w("You need to bind first")
            doUnbindService()
            return
        }
        try {
            val msg = Message.obtain(
                null,
                MSG_NOTIFICATION, 0, 0
            )
            msg.replyTo = gMessenger
            msg.data = bundleOf("message" to message.message,
                                "title" to message.title,
                                "priority" to message.priority.toInt())
            gService!!.send(msg)
            Log.i("Notification sent")
        } catch (e: RemoteException) {
            // There is nothing special we need to do if the service
            // has crashed.
        }
        /** Once the notification is delivered, we can unbind **/
        doUnbindService()
    }
}