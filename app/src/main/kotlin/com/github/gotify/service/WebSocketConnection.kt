package com.github.gotify.service

import android.app.AlarmManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.github.gotify.SSLSettings
import com.github.gotify.Utils
import com.github.gotify.api.CertUtils
import com.github.gotify.client.model.Message
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.tinylog.kotlin.Logger

internal class WebSocketConnection(
    private val baseUrl: String,
    settings: SSLSettings,
    private val token: String?,
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
    private lateinit var onMessageCallback: (Message) -> Unit
    private lateinit var onClose: Runnable
    private lateinit var onOpen: Runnable
    private lateinit var onFailure: OnNetworkFailureRunnable
    private lateinit var onReconnected: Runnable
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
    fun onMessage(onMessage: (Message) -> Unit): WebSocketConnection {
        this.onMessageCallback = onMessage
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
    fun onFailure(onFailure: OnNetworkFailureRunnable): WebSocketConnection {
        this.onFailure = onFailure
        return this
    }

    @Synchronized
    fun onReconnected(onReconnected: Runnable): WebSocketConnection {
        this.onReconnected = onReconnected
        return this
    }

    private fun request(): Request {
        val url = baseUrl.toHttpUrlOrNull()!!
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
        Logger.info("WebSocket($nextId): starting...")

        webSocket = client.newWebSocket(request(), Listener(nextId))
        return this
    }

    @Synchronized
    fun close() {
        if (webSocket != null) {
            webSocket?.close(1000, "")
            closed()
            Logger.info("WebSocket(${ID.get()}): closing existing connection.")
        }
    }

    @Synchronized
    private fun closed() {
        webSocket = null
        state = State.Disconnected
    }

    @Synchronized
    fun scheduleReconnect(seconds: Long) {
        if (state == State.Connecting || state == State.Connected) {
            return
        }
        state = State.Scheduled

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Logger.info("WebSocket: scheduling a restart in $seconds second(s) (via alarm manager)")
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
            Logger.info("WebSocket: scheduling a restart in $seconds second(s)")
            reconnectHandler.removeCallbacks(reconnectCallback)
            reconnectHandler.postDelayed(reconnectCallback, TimeUnit.SECONDS.toMillis(seconds))
        }
    }

    private inner class Listener(private val id: Long) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            syncExec(id) {
                state = State.Connected
                Logger.info("WebSocket($id): opened")
                onOpen.run()

                if (errorCount > 0) {
                    onReconnected.run()
                    errorCount = 0
                }
            }
            super.onOpen(webSocket, response)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            syncExec(id) {
                Logger.info("WebSocket($id): received message $text")
                val message = Utils.JSON.fromJson(text, Message::class.java)
                onMessageCallback(message)
            }
            super.onMessage(webSocket, text)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            syncExec(id) {
                if (state == State.Connected) {
                    Logger.warn("WebSocket($id): closed")
                    onClose.run()
                }
                closed()
            }
            super.onClosed(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val code = if (response != null) "StatusCode: ${response.code}" else ""
            val message = response?.message ?: ""
            Logger.error(t) { "WebSocket($id): failure $code Message: $message" }
            syncExec(id) {
                closed()

                errorCount++
                val minutes = (errorCount * 2 - 1).coerceAtMost(20)

                onFailure.execute(response?.message ?: "unreachable", minutes)
                scheduleReconnect(TimeUnit.MINUTES.toSeconds(minutes.toLong()))
            }
            super.onFailure(webSocket, t, response)
        }
    }

    @Synchronized
    private fun syncExec(id: Long, runnable: () -> Unit) {
        if (ID.get() == id) {
            runnable()
        }
    }

    internal fun interface OnNetworkFailureRunnable {
        fun execute(status: String, minutes: Int)
    }

    internal enum class State {
        Scheduled,
        Connecting,
        Connected,
        Disconnected
    }
}
