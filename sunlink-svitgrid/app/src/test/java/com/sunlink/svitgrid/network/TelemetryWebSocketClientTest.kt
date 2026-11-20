package com.sunlink.svitgrid.network

import com.sunlink.svitgrid.data.dto.LiveDataMessageDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Stage 29 — клиент WebSocket проверяется на локальном MockWebServer.
 * Это НЕ симуляция данных приложения: тестовый сервер только отдаёт кадр
 * в формате реального backend `/ws`.
 */
class TelemetryWebSocketClientTest {

    private val server = MockWebServer()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeClient: TelemetryWebSocketClient? = null
    private val frame = """
        {"type":"live_data","data":{"battery_soc":{"raw_value":94,"normalized_value":94.0,
        "unit":"%","confidence":"corroborated","quality":"valid","address":184,
        "timestamp":"2026-11-20T08:09:59+00:00"}},
        "status":{"connection_status":true,"last_update":"2026-11-20T08:09:59+00:00",
        "inverter_id":"deye_sg02lp1_am3","available_registers":9,"source":"deye_modbus"},
        "timestamp":"2026-11-20T08:09:59+00:00"}
    """.trimIndent()

    @After
    fun tearDown() {
        activeClient?.stop()
        server.shutdown()
    }

    private fun wsUpgrade(send: String? = frame): MockResponse =
        MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (send != null) webSocket.send(send)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
        })

    private fun client(
        onMessage: (LiveDataMessageDto) -> Unit,
        onState: (TelemetryWebSocketClient.WsTransportState) -> Unit
    ): TelemetryWebSocketClient = TelemetryWebSocketClient(
        scope = scope,
        urlProvider = { "ws://${server.hostName}:${server.port}/ws" },
        onMessage = onMessage,
        onState = onState,
        clientFactory = { OkHttpClient.Builder().build() },
        registerUrlListener = false
    ).also { activeClient = it }

    @Test
    fun `delivers real formatted frame to viewmodel callback`() {
        server.enqueue(wsUpgrade())
        val received = CountDownLatch(1)
        var soc: Double? = null
        val states = mutableListOf<TelemetryWebSocketClient.WsTransportState>()

        val client = client(
            onMessage = { soc = it.data?.get("battery_soc")?.normalized_value; received.countDown() },
            onState = { synchronized(states) { states.add(it) } }
        )
        client.start()

        assertTrue("frame not delivered", received.await(5, TimeUnit.SECONDS))
        assertEquals(94.0, soc)
        assertTrue(states.contains(TelemetryWebSocketClient.WsTransportState.OPEN))
    }

    @Test
    fun `non live_data frames are ignored`() {
        server.enqueue(wsUpgrade("""{"type":"ping"}"""))
        val received = CountDownLatch(1)
        var called = false
        val client = client(onMessage = { called = true; received.countDown() }, onState = {})
        client.start()
        received.await(500, TimeUnit.MILLISECONDS)
        assertFalse(called)
    }

    @Test
    fun `reconnects with bounded backoff after connection failure`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(wsUpgrade())
        val received = CountDownLatch(1)
        val reconnecting = CountDownLatch(1)

        val client = client(
            onMessage = { received.countDown() },
            onState = {
                if (it == TelemetryWebSocketClient.WsTransportState.RECONNECTING) {
                    reconnecting.countDown()
                }
            }
        )
        client.start()

        assertTrue("no reconnect scheduled", reconnecting.await(5, TimeUnit.SECONDS))
        assertTrue("no frame after reconnect", received.await(10, TimeUnit.SECONDS))
    }
}
