package com.github.gotify.service

import android.app.AlarmManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.github.gotify.SSLSettings
import com.github.gotify.Utils
import com.github.gotify.api.Callback.SuccessCallback
import com.github.gotify.api.CertUtils
import com.github.gotify.client.model.Message
import com.github.gotify.log.Log
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import okhttp3.*

internal class WebSocketConnection(
    private val baseUrl: String,
    settings: SSLSettings,
    private val token: String,
    private val connectivityManager: ConnectivityManager,
    private val alarmManager: AlarmManager
) {
    companion object {
        private val ID = AtomicLong(0)
    }

    private val client: OkHttpClient
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private val reconnectCallback = Runnable { start() }
    private var errorCount = 0

    private var webSocket: WebSocket? = null
    private var onMessage: SuccessCallback<Message>? = null
    private var onClose: Runnable? = null
    private var onOpen: Runnable? = null
    private var onBadRequest: BadRequestRunnable? = null
    private var onNetworkFailure: OnNetworkFailureRunnable? = null
    private var onReconnected: Runnable? = null
    private var state: State? = null

    init {
        val builder = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(1, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.SECONDS)
        CertUtils.applySslSettings(builder, settings)
        client = builder.build()
    }

    @Synchronized
    fun onMessage(onMessage: SuccessCallback<Message>): WebSocketConnection {
        this.onMessage = onMessage
        return this
    }

    @Synchronized
    fun onClose(onClose: Runnable): WebSocketConnection {
        this.onClose = onClose
        return this
    }

    @Synchronized
    fun onOpen(onOpen: Runnable): WebSocketConnection {
        this.onOpen = onOpen
        return this
    }

    @Synchronized
    fun onBadRequest(onBadRequest: BadRequestRunnable): WebSocketConnection {
        this.onBadRequest = onBadRequest
        return this
    }

    @Synchronized
    fun onNetworkFailure(onNetworkFailure: OnNetworkFailureRunnable): WebSocketConnection {
        this.onNetworkFailure = onNetworkFailure
        return this
    }

    @Synchronized
    fun onReconnected(onReconnected: Runnable): WebSocketConnection {
        this.onReconnected = onReconnected
        return this
    }

    private fun request(): Request {
        val url = HttpUrl.parse(baseUrl)!!
            .newBuilder()
            .addPathSegment("stream")
            .addQueryParameter("token", token)
            .build()
        return Request.Builder().url(url).get().build()
    }

    @Synchronized
    fun start(): WebSocketConnection {
        if (state == State.Connecting || state == State.Connected) {
            return this
        }
        close()
        state = State.Connecting
        val nextId = ID.incrementAndGet()
        Log.i("WebSocket($nextId): starting...")

        webSocket = client.newWebSocket(request(), Listener(nextId))
        return this
    }

    @Synchronized
    fun close() {
        if (webSocket != null) {
            Log.i("WebSocket(${ID.get()}): closing existing connection.")
            state = State.Disconnected
            webSocket!!.close(1000, "")
            webSocket = null
        }
    }

    @Synchronized
    fun scheduleReconnect(seconds: Long) {
        if (state == State.Connecting || state == State.Connected) {
            return
        }
        state = State.Scheduled

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.i("WebSocket: scheduling a restart in $seconds second(s) (via alarm manager)")
            val future = Calendar.getInstance()
            future.add(Calendar.SECOND, seconds.toInt())
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                future.timeInMillis,
                "reconnect-tag",
                { start() },
                null
            )
        } else {
            Log.i("WebSocket: scheduling a restart in $seconds second(s)")
            reconnectHandler.removeCallbacks(reconnectCallback)
            reconnectHandler.postDelayed(reconnectCallback, TimeUnit.SECONDS.toMillis(seconds))
        }
    }

    private inner class Listener(private val id: Long) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            syncExec {
                state = State.Connected
                Log.i("WebSocket($id): opened")
                onOpen!!.run()

                if (errorCount > 0) {
                    onReconnected!!.run()
                    errorCount = 0
                }
            }
            super.onOpen(webSocket, response)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            syncExec {
                Log.i("WebSocket($id): received message $text")
                val message = Utils.JSON.fromJson(text, Message::class.java)
                onMessage!!.onSuccess(message)
            }
            super.onMessage(webSocket, text)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            syncExec {
                if (state == State.Connected) {
                    Log.w("WebSocket($id): closed")
                    onClose!!.run()
                }
                state = State.Disconnected
            }
            super.onClosed(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val code = if (response != null) "StatusCode: ${response.code()}" else ""
            val message = if (response != null) response.message() else ""
            Log.e("WebSocket($id): failure $code Message: $message", t)
            syncExec {
                state = State.Disconnected
                if (response != null && response.code() >= 400 && response.code() <= 499) {
                    onBadRequest!!.execute(message)
                    close()
                    return@syncExec
                }

                errorCount++

                val network = connectivityManager.activeNetworkInfo
                if (network == null || !network.isConnected) {
                    Log.i("WebSocket($id): Network not connected")
                }

                val minutes = (errorCount * 2 - 1).coerceAtMost(20)

                onNetworkFailure!!.execute(minutes)
                scheduleReconnect(TimeUnit.MINUTES.toSeconds(minutes.toLong()))
            }
            super.onFailure(webSocket, t, response)
        }

        private fun syncExec(runnable: Runnable) {
            synchronized(this) {
                if (ID.get() == id) {
                    runnable.run()
                }
            }
        }
    }

    internal interface BadRequestRunnable {
        fun execute(message: String)
    }

    internal interface OnNetworkFailureRunnable {
        fun execute(minutes: Int)
    }

    internal enum class State {
        Scheduled,
        Connecting,
        Connected,
        Disconnected
    }
}
