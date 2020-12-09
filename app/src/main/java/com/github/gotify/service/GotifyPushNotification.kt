package com.github.gotify.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.core.os.bundleOf
import com.github.gotify.log.Log
import com.google.gson.Gson

/**
 * THIS FUNC IS USED TO PUSH NOTIFICATIONS TO OTHER APPS
 * It is called from the thread in WebSocketService
 */

/**
 * Function to notify client
 */
fun notifyClient(context: Context, clientPackage: String, message: com.github.gotify.client.model.Message){
    val db = MessagingDatabase(context)
    val service = db.getServiceName(clientPackage)
    if (service.isBlank()) {
        Log.w("No service found for $clientPackage")
        return
    }
    val gHandlerThread = HandlerThread(clientPackage)
    gHandlerThread.start()

    val gConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            val gService = Messenger(service)
            Log.i("Remote service connected")
            try {
                val msg = Message.obtain(
                        null,
                        TYPE_MESSAGE, 0, 0
                )
                msg.data = bundleOf("json" to Gson().toJson(message) )
                gService.send(msg)
                Log.i("Notification sent")
            } catch(e: RemoteException) {
                Log.e("Something went wrong", e)
            } finally {
                context.unbindService(this)
                gHandlerThread.quit()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            gHandlerThread.quit()
        }
    }

    val intent = Intent()
    intent.component = ComponentName(clientPackage, service)
    context.bindService(intent, gConnection, Context.BIND_AUTO_CREATE)
}