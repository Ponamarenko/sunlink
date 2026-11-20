package com.sunlink.svitgrid.network

import com.sunlink.svitgrid.data.dto.LiveDataMessageDto
import com.sunlink.svitgrid.data.dto.WebSocketLiveParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Stage 29 — единственный WebSocket-клиент телеметрии.
 *
 * Не опрашивает оборудование и не создаёт второй poller: он только потребляет
 * кадры `live_data`, которые backend формирует из одного physical poller.
 * При потере связи переподключается с ограниченной экспоненциальной задержкой.
 */
class TelemetryWebSocketClient internal constructor(
    private val scope: CoroutineScope,
    private val urlProvider: () -> String,
    private val onMessage: (LiveDataMessageDto) -> Unit,
    private val onState: (WsTransportState) -> Unit,
    private val clientFactory: () -> OkHttpClient = { defaultClient() },
    private val registerUrlListener: Boolean = true
) {
    enum class WsTransportState { CONNECTING, OPEN, RECONNECTING, CLOSED }

    companion object {
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val NORMAL_CLOSE = 1000

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }

    private val client: OkHttpClient by lazy { clientFactory() }
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var stopped = true
    private var attempt = 0
    private var generation = 0

    private val urlListener: (String) -> Unit = { reconnectNow() }

    init {
        if (registerUrlListener) RetrofitClient.addBaseUrlListener(urlListener)
    }

    fun start() {
        stopped = false
        if (webSocket == null && reconnectJob?.isActive != true) connect()
    }

    fun stop() {
        stopped = true
        reconnectJob?.cancel()
        reconnectJob = null
        val socket = webSocket
        webSocket = null
        socket?.close(NORMAL_CLOSE, "client stop")
        onState(WsTransportState.CLOSED)
        if (registerUrlListener) RetrofitClient.removeBaseUrlListener(urlListener)
    }

    /** Явное переподключение после смены адреса backend. */
    fun reconnectNow() {
        if (stopped) return
        reconnectJob?.cancel()
        reconnectJob = null
        val socket = webSocket
        webSocket = null
        socket?.close(NORMAL_CLOSE, "endpoint changed")
        attempt = 0
        connect()
    }

    fun currentBackoffMs(): Long = backoffMs(attempt)

    private fun backoffMs(attemptNumber: Int): Long {
        if (attemptNumber <= 0) return INITIAL_BACKOFF_MS
        val shift = min(attemptNumber - 1, 10)
        return min(MAX_BACKOFF_MS, INITIAL_BACKOFF_MS shl shift)
    }

    private fun connect() {
        if (stopped) return
        val url = urlProvider()
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: Exception) {
            scheduleReconnect()
            return
        }
        val myGeneration = ++generation
        NetworkDiagnostics.record(RequestType.WEBSOCKET, url, "CONNECTING")
        onState(WsTransportState.CONNECTING)
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                if (!isCurrent(myGeneration)) return
                attempt = 0
                NetworkDiagnostics.record(RequestType.WEBSOCKET, url, "OPEN", success = true)
                onState(WsTransportState.OPEN)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!isCurrent(myGeneration) || stopped) return
                val message = WebSocketLiveParser.parse(text)
                if (!WebSocketLiveParser.isLiveData(message)) return
                attempt = 0
                NetworkDiagnostics.record(RequestType.WEBSOCKET, url, "LIVE_DATA", success = true)
                message?.let(onMessage)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSE, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrent(myGeneration)) return
                this@TelemetryWebSocketClient.webSocket = null
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                NetworkDiagnostics.record(
                    RequestType.WEBSOCKET, url, "FAILED",
                    httpCode = response?.code, errorType = t.javaClass.simpleName
                )
                if (!isCurrent(myGeneration)) return
                this@TelemetryWebSocketClient.webSocket = null
                scheduleReconnect()
            }
        })
    }

    private fun isCurrent(myGeneration: Int): Boolean = !stopped && myGeneration == generation

    private fun scheduleReconnect() {
        if (stopped || reconnectJob?.isActive == true) return
        val delayMs = backoffMs(attempt)
        attempt++
        onState(WsTransportState.RECONNECTING)
        reconnectJob = scope.launch {
            delay(delayMs)
            if (!stopped) connect()
        }
    }
}
