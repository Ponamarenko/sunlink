package com.sunlink.svitgrid.integration

import com.sunlink.svitgrid.data.BackendDefaults
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterStatus
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.NetworkDiagnostics
import com.sunlink.svitgrid.network.RetrofitClient
import com.sunlink.svitgrid.network.RequestType
import com.sunlink.svitgrid.ui.designsystem.DashboardState
import com.sunlink.svitgrid.ui.designsystem.DashboardStateMapper
import com.sunlink.svitgrid.update.UpdateManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class HotfixEndpointTest {

    @Before
    fun setUp() {
        RetrofitClient.setBaseUrl("https://sunlink.pp.ua:9443/")
    }

    @Test
    fun `release public endpoint is exactly agreed URL`() {
        assertEquals("https://sunlink.pp.ua:9443/", BackendDefaults.BASE_URL)
        assertEquals("https://sunlink.pp.ua:9443/", RetrofitClient.getBaseUrl())
        assertEquals(9443, BackendDefaults.PORT)
        assertTrue(BackendDefaults.USE_HTTPS)
    }

    @Test
    fun `public http with port 8800 is rejected by runtime client`() {
        assertFalse(RetrofitClient.setBaseUrl("http://sunlink.pp.ua:8800/"))
        assertEquals("https://sunlink.pp.ua:9443/", RetrofitClient.getBaseUrl())
        assertFalse(RetrofitClient.setBaseUrl("http://91.226.253.235:8800/"))
    }

    @Test
    fun `websocket url follows rest scheme and port`() {
        assertEquals("wss://sunlink.pp.ua:9443/ws", RetrofitClient.getWebSocketUrl())
        assertTrue(RetrofitClient.setBaseUrl("http://192.168.25.20:18800/"))
        assertEquals("ws://192.168.25.20:18800/ws", RetrofitClient.getWebSocketUrl())
        RetrofitClient.setBaseUrl("https://sunlink.pp.ua:9443/")
    }

    @Test
    fun `update checker uses GitHub endpoint not sunlink backend`() {
        assertEquals(
            "https://api.github.com/repos/Ponamarenko/sunlink/releases/latest",
            UpdateManager.UPDATE_ENDPOINT
        )
        assertFalse(UpdateManager.UPDATE_ENDPOINT.contains("sunlink.pp.ua"))
    }

    @Test
    fun `no unexpected 8800 URL in production kotlin sources`() {
        val root = File("src/main/java/com/sunlink/svitgrid")
        val bad = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("sunlink.pp.ua:8800") || it.readText().contains("91.226.253.235:8800") }
            .map { it.name }.toList()
        assertTrue("unexpected 8800 endpoint: $bad", bad.isEmpty())
    }

    @Test
    fun `diagnostics sanitize secrets and record request type`() {
        assertEquals(
            "wss://sunlink.pp.ua:9443/ws",
            NetworkDiagnostics.sanitize("wss://user:token@sunlink.pp.ua:9443/ws?key=secret#frag")
        )
        NetworkDiagnostics.record(RequestType.UPDATE, "wss://token@host/ws?x=1", "FAILED", errorType = "IOException")
        val event = NetworkDiagnostics.events.value.first()
        assertEquals(RequestType.UPDATE, event.type)
        assertEquals("FAILED", event.stage)
        assertEquals("IOException", event.errorType)
        assertFalse(event.endpoint.contains("token"))
        assertFalse(event.endpoint.contains("secret"))
    }

    @Test
    fun `update failure does not block dashboard telemetry state`() {
        val status = InverterStatus(online = true, lastUpdate = "2026-11-20T10:00:00Z")
        val data = InverterData(batterySoc = 90f, timestamp = status.lastUpdate, source = "deye_modbus")
        val state = DashboardStateMapper.map(
            InverterUiState.Success(data, status, false),
            false,
            com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState.CLOSED
        )
        // UPDATE failing is a separate flow and must not downgrade telemetry state.
        assertEquals(DashboardState.ONLINE, state)
        assertTrue(data.hasMeasurements)
    }
}
