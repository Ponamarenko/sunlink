package com.sunlink.svitgrid.ui.redesign

import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage531DarkThemeConsistencyTest {

    private val root = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
    private val sources = root.listFiles { f -> f.extension == "kt" }!!.map { it.readText() }.joinToString("\n")
    private val theme = File("src/main/java/com/sunlink/svitgrid/ui/theme/SvitGridTheme.kt").readText()

    @Test
    fun `no local light theme remains in the app`() {
        assertFalse(theme.contains("lightColorScheme"))
        assertFalse(theme.contains("sunLinkLightColors"))
        assertFalse(theme.contains("PremiumAnalyticsTheme"))
        assertFalse(theme.contains("PremiumDiagnosticsTheme"))
        assertFalse(File("src/main/java/com/sunlink/svitgrid/ui/designsystem/SunLinkColors.kt").readText()
            .contains("sunLinkAnalyticsColors"))
        assertFalse(sources.contains("PremiumAnalyticsTheme"))
        assertFalse(sources.contains("PremiumDiagnosticsTheme"))
        assertFalse(sources.contains("lightColorScheme"))
        assertFalse(sources.contains("sunLinkLightColors"))
    }

    @Test
    fun `all nine screens rely on shared dark tokens`() {
        listOf("HomeScreen.kt", "EnergyScreen.kt", "BatteryScreen.kt", "SolarScreen.kt",
            "GridScreen.kt", "LoadScreen.kt", "AnalyticsScreen.kt", "DiagnosticsScreen.kt", "MoreScreen.kt")
            .forEach { name ->
                val src = File(root, name).readText()
                assertTrue("$name must use LocalSunLinkColors", src.contains("LocalSunLinkColors.current"))
            }
        val c = sunLinkDarkColors()
        assertEquals(androidx.compose.ui.graphics.Color(0xFF070B14), c.background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0F1726), c.surface)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF24324D), c.border)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF8FAFC), c.textPrimary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF94A3B8), c.textSecondary)
    }

    @Test
    fun `Analytics and Diagnostics have no white surfaces`() {
        val analytics = File(root, "AnalyticsScreen.kt").readText()
        val diagnostics = File(root, "DiagnosticsScreen.kt").readText()
        listOf(analytics, diagnostics).forEach {
            assertFalse(it.contains("Color(0xFFFFFFFF)"))
            assertFalse(it.contains("surfaceVariant = Color(0xFFF"))
            assertFalse(it.contains("background = Color(0xFFF"))
        }
        assertTrue(analytics.contains("AnalyticsBarChart("))
        assertTrue(diagnostics.contains("freshnessLabel(") || diagnostics.contains("DataFreshnessIndicator("))
    }

    @Test
    fun `telemetry honesty is preserved`() {
        assertTrue(sources.contains("isConfirmed("))
        assertTrue(sources.contains("seriesSegments(") || sources.contains("point.gap"))
        assertTrue(sources.contains("freshnessOf("))
        assertFalse(sources.contains("Random"))
        assertFalse(sources.contains("demo"))
        assertTrue(File(root, "LoadScreen.kt").readText().contains("displayLoadPower("))
    }

    @Test
    fun `version is bumped to stage 53 dot 1`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 82"))
        assertTrue(gradle.contains("versionName = \"2.57.0\""))
    }
}
