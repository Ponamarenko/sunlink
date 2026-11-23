package com.sunlink.svitgrid.ui.redesign

import androidx.compose.ui.graphics.Color
import com.sunlink.svitgrid.ui.designsystem.sunLinkAnalyticsColors
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.ui.theme.PremiumAnalyticsTheme
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage50PremiumTest {

    @Test
    fun `dark palette follows hybrid premium color semantics`() {
        val c = sunLinkDarkColors()
        assertEquals(Color(0xFF070B14), c.background)
        assertEquals(Color(0xFF0F1726), c.surface)
        assertEquals(Color(0xFF10B981), c.battery)
        assertEquals(Color(0xFFF5B942), c.solar)
        assertEquals(Color(0xFF3B82F6), c.grid)
        assertEquals(Color(0xFFF59E0B), c.load)
    }

    @Test
    fun `analytics palette stays light for the analytics screen`() {
        val light = sunLinkAnalyticsColors()
        assertNotEquals(sunLinkDarkColors().background, light.background)
        assertEquals(Color(0xFFFFFFFF), light.surface)
        assertEquals(Color(0xFF16243A), light.textPrimary)
        val theme = File("src/main/java/com/sunlink/svitgrid/ui/theme/SvitGridTheme.kt").readText()
        assertTrue(theme.contains("fun PremiumAnalyticsTheme"))
    }

    @Test
    fun `freshness mapper separates fresh stale provisional unavailable offline`() {
        assertEquals(Freshness.FRESH, freshnessOf(false, "valid", "verified", true))
        assertEquals(Freshness.STALE, freshnessOf(true, "valid", "verified", true))
        assertEquals(Freshness.PROVISIONAL, freshnessOf(false, "valid", "provisional", true))
        assertEquals(Freshness.UNAVAILABLE, freshnessOf(false, "unavailable", null, true))
        assertEquals(Freshness.OFFLINE, freshnessOf(false, null, null, false))
    }

    @Test
    fun `flow directions are only derived from confirmed signed power`() {
        assertEquals(FlowDirection.TO_HUB, pvFlowDirection(1200f, true))
        assertEquals(FlowDirection.UNKNOWN, pvFlowDirection(1200f, false))
        assertEquals(FlowDirection.UNKNOWN, pvFlowDirection(null, true))

        assertEquals(FlowDirection.TO_HUB, gridFlowDirection(800f, true))
        assertEquals(FlowDirection.FROM_HUB, gridFlowDirection(-800f, true))
        assertEquals(FlowDirection.UNKNOWN, gridFlowDirection(-800f, false))
        assertEquals(FlowDirection.UNKNOWN, gridFlowDirection(0f, true))

        assertEquals(FlowDirection.FROM_HUB, loadFlowDirection(600f, true))
        assertEquals(FlowDirection.UNKNOWN, loadFlowDirection(600f, false))
        assertEquals(FlowDirection.UNKNOWN, batteryFlowDirection())
    }

    @Test
    fun `dark theme is the default and platform window is dark`() {
        val settings = File("src/main/java/com/sunlink/svitgrid/data/SettingsRepository.kt").readText()
        assertTrue(settings.contains("KEY_THEME_MODE, \"DARK\""))
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("Theme.Material.Light"))
        assertTrue(manifest.contains("@style/SunLinkPlatformTheme"))
        val theme = File("src/main/res/values/themes.xml").readText()
        assertTrue(theme.contains("android:windowLightStatusBar\">false"))
        assertTrue(theme.contains("premium_background"))
        val activity = File("src/main/java/com/sunlink/svitgrid/MainActivity.kt").readText()
        assertTrue(activity.contains("enableEdgeToEdge()"))
    }

    @Test
    fun `all nine premium screens are routed`() {
        val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        listOf("home", "energy", "battery", "solar", "grid", "load", "analytics", "diagnostics", "more")
            .forEach { assertTrue("missing route $it", app.contains("composable(\"$it\")")) }
        assertTrue(app.contains("PremiumBottomNavigation("))
    }

    @Test
    fun `premium components are shared and fake data is absent`() {
        val dir = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
        val sources = dir.listFiles { f -> f.extension == "kt" }!!.map { it.readText() }.joinToString("\n")
        assertTrue(sources.contains("fun PremiumCard("))
        assertTrue(sources.contains("fun PremiumEnergyFlow("))
        assertTrue(sources.contains("fun DataFreshnessIndicator("))
        assertTrue(sources.contains("fun AnalyticsChart("))
        assertTrue(sources.contains("fun PremiumBottomNavigation("))
        assertFalse(sources.contains("Random"))
        assertFalse(sources.contains("demo"))
        assertFalse(sources.contains("Math.random"))
    }

    @Test
    fun `version is at least stage 50`() {
        val gradle = File("build.gradle.kts").readText()
        val code = Regex("versionCode = (\\d+)").find(gradle)?.groupValues?.get(1)?.toIntOrNull()
        assertTrue("versionCode must be >= 71, got $code", code != null && code >= 71)
        assertTrue(
            "versionName must be >= 2.50.0",
            gradle.contains("versionName = \"2.50.0\"") ||
                gradle.contains("versionName = \"2.50.1\"") ||
                gradle.contains("versionName = \"2.50.2\"")
        )
    }
}
