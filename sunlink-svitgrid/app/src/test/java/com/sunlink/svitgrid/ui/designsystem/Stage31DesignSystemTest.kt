package com.sunlink.svitgrid.ui.designsystem

import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterStatus
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.ui.theme.ThemeMode
import com.sunlink.svitgrid.ui.theme.ThemeController
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Stage 31 — тести Design System і чесності відображення телеметрії.
 * Без X random/mock у production-шляху.
 */
class Stage31DesignSystemTest {

    private val onlineStatus = InverterStatus(online = true, lastUpdate = "2026-11-20T10:00:00Z")
    private val offlineStatus = InverterStatus(online = false, lastUpdate = "2026-11-20T10:00:00Z")

    private fun data(soc: Float? = 90f, grid: Float? = null) = InverterData(
        batterySoc = soc,
        pGrid = grid,
        timestamp = "2026-11-20T10:00:00Z",
        connectionStatus = true,
        source = "deye_modbus",
        quality = "valid"
    )

    @Test
    fun `dark and light palettes define all semantic roles`() {
        val dark = sunLinkDarkColors()
        val light = sunLinkLightColors()
        assertNotNull(dark.background); assertNotNull(dark.surface); assertNotNull(dark.textPrimary)
        assertNotNull(light.background); assertNotNull(light.surface); assertNotNull(light.textPrimary)
        assertNotEquals(dark.background, light.background)
        assertNotEquals(dark.surface, light.surface)
        assertNotEquals(dark.textPrimary, light.textPrimary)
        assertNotEquals(dark.solar, light.solar)
        assertNotEquals(dark.battery, light.battery)
        assertNotEquals(dark.grid, light.grid)
        assertNotEquals(dark.load, light.load)
        assertNotEquals(dark.error, light.error)
    }

    @Test
    fun `numeric styles use tabular figures`() {
        assertEquals("tnum", SunLinkTypography.metric.fontFeatureSettings)
        assertEquals("tnum", SunLinkTypography.display.fontFeatureSettings)
        assertEquals("tnum", SunLinkTypography.caption.fontFeatureSettings)
    }

    @Test
    fun `state mapper distinguishes loading online offline stale error connecting`() {
        
        assertEquals(
            DashboardState.LOADING,
            DashboardStateMapper.map(InverterUiState.Loading, true, com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState.CLOSED)
        )
        assertEquals(
            DashboardState.ONLINE,
            DashboardStateMapper.map(InverterUiState.Success(data(), onlineStatus, false), false, com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState.OPEN)
        )
        assertEquals(
            DashboardState.STALE,
            DashboardStateMapper.map(InverterUiState.Success(data(), onlineStatus, true), false, com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState.OPEN)
        )
        assertEquals(
            DashboardState.ERROR,
            DashboardStateMapper.map(InverterUiState.Error("fail"), false, com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState.CLOSED)
        )
        assertEquals(
            DashboardState.CONNECTING,
            DashboardStateMapper.map(InverterUiState.Loading, false, com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState.RECONNECTING)
        )
        assertEquals(
            DashboardState.OFFLINE,
            DashboardStateMapper.map(InverterUiState.Success(data(), offlineStatus, false), false, com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState.CLOSED)
        )
    }

    @Test
    fun `null measurements never become zero and are not shown as data`() {
        val empty = InverterData()
        assertFalse(empty.hasMeasurements)
        assertNull(empty.batterySoc)
        assertNull(empty.pGrid)
        assertNull(empty.pSun)
        assertNull(empty.pv1Power)
        assertNull(empty.dcTemperature)
        val emptyFlow = data(soc = null, grid = null)
        assertFalse(emptyFlow.hasMeasurements || emptyFlow.batterySoc == 0f)
    }

    @Test
    fun `real confirmed values are preserved unchanged`() {
        val d = data(soc = 90f, grid = -140f)
        assertEquals(90f, d.batterySoc)
        assertEquals(-140f, d.pGrid)
        assertTrue(d.hasMeasurements)
    }

    @Test
    fun `energy flow never guesses sign or animates unknown values`() {
        val unknown = EnergyFlowConnection("grid", 100f, directionConfirmed = false, color = sunLinkDarkColors().grid)
        assertFalse("unknown direction must not animate", unknown.isActive)
        val confirmed = EnergyFlowConnection("grid", 100f, directionConfirmed = true, color = sunLinkDarkColors().grid)
        assertTrue(confirmed.isActive)
        val zero = EnergyFlowConnection("grid", 0f, directionConfirmed = true, color = sunLinkDarkColors().grid)
        assertFalse("zero power must not animate", zero.isActive)
        val absent = EnergyFlowConnection("grid", null, directionConfirmed = true, color = sunLinkDarkColors().grid)
        assertFalse("absent power must not animate", absent.isActive)
    }

    @Test
    fun `online requires real exchange not just app start`() {
        val state = DashboardStateMapper.map(
            InverterUiState.Success(data(), onlineStatus, false), false,
            com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState.CLOSED
        )
        // Success з реальним статусом залишається ONLINE навіть якщо WS тимчасово закритий,
        // але Success створюється тільки після успішного REST/WS обміну (Stage 30).
        assertEquals(DashboardState.ONLINE, state)
        assertTrue(DashboardState.ONLINE.isActiveFlowAllowed)
        assertFalse(DashboardState.STALE.isActiveFlowAllowed)
        assertFalse(DashboardState.OFFLINE.isActiveFlowAllowed)
    }

    @Test
    fun `theme modes are exactly system light dark`() {
        assertEquals(3, ThemeMode.values().size)
        val controller = ThemeController(ThemeMode.SYSTEM) {}
        assertEquals(ThemeMode.SYSTEM, controller.mode)
    }

    @Test
    fun `ukrainian and english string resources have identical keys`() {
        val uk = File("src/main/res/values/strings.xml")
        val en = File("src/main/res/values-en/strings.xml")
        assertTrue("uk strings exist", uk.exists())
        assertTrue("en strings exist", en.exists())
        fun keys(f: File) = Regex("""<string name="([^"]+)"""").findAll(f.readText()).map { it.groupValues[1] }.toSet()
        val ukKeys = keys(uk)
        val enKeys = keys(en)
        assertTrue("uk keys", ukKeys.size >= 40)
        assertEquals("localization parity", ukKeys, enKeys)
    }

    @Test
    fun `production sources contain no random or fake telemetry`() {
        val root = File("src/main/java/com/sunlink/svitgrid")
        val forbidden = listOf("kotlin.random", "Random.", "random(", "uniform(")
        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "DashboardPreviews.kt" }
            .flatMap { file ->
                forbidden.filter { token -> file.readText().contains(token) }.map { token -> "${file.name}:$token" }
            }
            .toList()
        assertTrue("no fake data in production: $violations", violations.isEmpty())
    }

    @Test
    fun `preview values are explicitly isolated from production`() {
        val preview = File("src/main/java/com/sunlink/svitgrid/ui/dashboard/DashboardPreviews.kt")
        assertTrue(preview.exists())
        assertTrue(preview.readText().contains("PREVIEW_ONLY"))
        val dashboard = File("src/main/java/com/sunlink/svitgrid/ui/dashboard/DashboardScreen.kt").readText()
        assertFalse("production dashboard must not reference preview data", dashboard.contains("PreviewTelemetry"))
    }
}
