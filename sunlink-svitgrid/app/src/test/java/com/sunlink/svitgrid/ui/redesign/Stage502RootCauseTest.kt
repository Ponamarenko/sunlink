package com.sunlink.svitgrid.ui.redesign

import androidx.compose.ui.graphics.Color
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.ui.theme.ThemeMode
import com.sunlink.svitgrid.ui.theme.resolveRootDarkTheme
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage502RootCauseTest {

    @Test
    fun `root theme is dark for every stored preference`() {
        ThemeMode.values().forEach { mode ->
            assertTrue("root must stay dark for $mode", resolveRootDarkTheme(mode))
        }
        val home = File("src/main/java/com/sunlink/svitgrid/ui/redesign/HomeScreen.kt").readText()
        assertFalse(home.contains("themeMode = ThemeMode.LIGHT"))
        assertFalse(home.contains("sunLinkLightColors"))
    }

    @Test
    fun `migration converts any non dark value and startup ignores preference`() {
        val settings = File("src/main/java/com/sunlink/svitgrid/data/SettingsRepository.kt").readText()
        assertTrue(settings.contains("valid != com.sunlink.svitgrid.ui.theme.ThemeMode.DARK"))
        val activity = File("src/main/java/com/sunlink/svitgrid/MainActivity.kt").readText()
        assertTrue(activity.contains("settings.migrateLegacyThemeMode()"))
        assertTrue(activity.contains("mutableStateOf(ThemeMode.DARK)"))
        assertTrue(activity.contains("isAppearanceLightStatusBars = false"))
        assertTrue(activity.contains("isAppearanceLightNavigationBars = false"))
    }

    @Test
    fun `websocket CONNECTING is never presented as OPEN`() {
        assertEquals("WS OPEN", transportLabel(TelemetryWebSocketClient.WsTransportState.OPEN))
        assertEquals("WS CONNECTING", transportLabel(TelemetryWebSocketClient.WsTransportState.CONNECTING))
        assertEquals("WS RECONNECTING", transportLabel(TelemetryWebSocketClient.WsTransportState.RECONNECTING))
        assertEquals("WS CLOSED", transportLabel(TelemetryWebSocketClient.WsTransportState.CLOSED))
        val home = File("src/main/java/com/sunlink/svitgrid/ui/redesign/HomeScreen.kt").readText()
        assertTrue(home.contains("WebSocket:"))
        assertFalse(home.contains("WS OPEN"))
    }

    @Test
    fun `load power sign is preserved from signed register 175`() {
        assertEquals(-93f, displayLoadPower(-93f))
        assertEquals(0f, displayLoadPower(0f))
        assertEquals(52f, displayLoadPower(52f))
        assertNull(displayLoadPower(null))
        val source = File("src/main/java/com/sunlink/svitgrid/ui/redesign/TelemetryHelpers.kt").readText()
        assertTrue(source.contains("is_signed=true"))
        assertFalse(source.contains("return value?.let { kotlin.math.abs(it) }"))
        val load = File("src/main/java/com/sunlink/svitgrid/ui/redesign/LoadScreen.kt").readText()
        assertTrue(load.contains("displayLoadPower("))
    }

    @Test
    fun `hybrid premium tokens match approved values`() {
        val c = sunLinkDarkColors()
        assertEquals(Color(0xFF070B14), c.background)
        assertEquals(Color(0xFF0F1726), c.surface)
        assertEquals(Color(0xFF24324D), c.border)
        assertEquals(Color(0xFFF8FAFC), c.textPrimary)
        assertEquals(Color(0xFF94A3B8), c.textSecondary)
    }

    @Test
    fun `version is at least stage 50 dot 2`() {
        val gradle = File("build.gradle.kts").readText()
        val code = Regex("versionCode = (\\d+)").find(gradle)?.groupValues?.get(1)?.toIntOrNull()
        assertTrue("versionCode must be >= 73, got $code", code != null && code >= 73)
        val name = Regex("versionName = \"([^\"]+)\"").find(gradle)?.groupValues?.get(1) ?: ""
        assertTrue("versionName must be >= 2.50.2, got $name", name >= "2.50.2")
    }
}
