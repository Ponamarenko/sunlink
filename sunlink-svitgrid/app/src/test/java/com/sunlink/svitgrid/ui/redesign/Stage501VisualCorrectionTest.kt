package com.sunlink.svitgrid.ui.redesign

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.util.Locale

class Stage501VisualCorrectionTest {

    @Test
    fun `timestamp is human readable and exact ISO stays in the model`() {
        val iso = "2026-11-23T13:12:06Z"
        val formatted = formatTelemetryTimestamp(iso, ZoneId.of("UTC"), Locale.US)
        assertEquals("23 Nov, 13:12:06", formatted)
        assertFalse(formatted.contains("T"))
        assertFalse(formatted.contains("Z"))
        assertEquals("—", formatTelemetryTimestamp(null))
        assertEquals("—", formatTelemetryTimestamp("  "))
    }

    @Test
    fun `malformed timestamp degrades to stable text`() {
        assertEquals("2026-11-23 13:12:06", formatTelemetryTimestamp("2026-11-23T13:12:06+00:00/bad"))
    }

    @Test
    fun `startup never falls back to SYSTEM and bars stay dark`() {
        val activity = File("src/main/java/com/sunlink/svitgrid/MainActivity.kt").readText()
        assertTrue(activity.contains("settings.migrateLegacyThemeMode()"))
        assertTrue(activity.contains("mutableStateOf(ThemeMode.DARK)"))
        assertTrue(activity.contains("isAppearanceLightStatusBars = false"))
        assertTrue(activity.contains("isAppearanceLightNavigationBars = false"))

        val theme = File("src/main/java/com/sunlink/svitgrid/ui/theme/SvitGridTheme.kt").readText()
        assertTrue(theme.contains("fun resolveRootDarkTheme"))
        assertTrue(theme.contains("ThemeMode.SYSTEM -> true") == false)
        assertFalse(theme.contains("PremiumAnalyticsTheme"))
        assertFalse(theme.contains("PremiumDiagnosticsTheme"))

        val v27 = File("src/main/res/values-v27/themes.xml").readText()
        assertTrue(v27.contains("android:windowLightNavigationBar\">false"))
    }

    @Test
    fun `home time and bottom overlap stay fixed after navigation cleanup`() {
        val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        assertFalse(app.contains("TopAppBar("))
        assertFalse(app.contains("ModalNavigationDrawer("))
        assertTrue(app.contains("WindowInsets.systemBars.only(WindowInsetsSides.Top)"))

        val home = File("src/main/java/com/sunlink/svitgrid/ui/redesign/HomeScreen.kt").readText()
        assertTrue(home.contains("formatTelemetryTimestamp(full?.timestamp)"))
        assertTrue(home.contains("freshnessLabel("))

        val ds = File("src/main/java/com/sunlink/svitgrid/ui/redesign/DesignSystem.kt").readText()
        assertTrue(ds.contains("bottom = PremiumTokens.spaceXl"))
        assertTrue(ds.contains("navigationBarsPadding()"))
        assertTrue(ds.contains("compact: Boolean = false"))
    }

    @Test
    fun `analytics and diagnostics use the shared dark theme`() {
        val analytics = File("src/main/java/com/sunlink/svitgrid/ui/redesign/AnalyticsScreen.kt").readText()
        assertFalse(analytics.contains("PremiumAnalyticsTheme"))
        val diagnostics = File("src/main/java/com/sunlink/svitgrid/ui/redesign/DiagnosticsScreen.kt").readText()
        assertFalse(diagnostics.contains("PremiumDiagnosticsTheme"))
        val sources = File("src/main/java/com/sunlink/svitgrid/ui/redesign").listFiles { f -> f.extension == "kt" }!!
            .map { it.readText() }.joinToString("\n")
        assertFalse(sources.contains("lightColorScheme"))
        assertFalse(sources.contains("sunLinkLightColors"))
    }

    @Test
    fun `version is bumped to stage 50 dot 1`() {
        val gradle = File("build.gradle.kts").readText()
        val code = Regex("versionCode = (\\d+)").find(gradle)?.groupValues?.get(1)?.toIntOrNull()
        assertTrue("versionCode must be >= 72, got $code", code != null && code >= 72)
        val name = Regex("versionName = \"([^\"]+)\"").find(gradle)?.groupValues?.get(1) ?: ""
        assertTrue("versionName must be >= 2.50.0, got $name", name == "2.50.0" || name == "2.50.1" ||
            name == "2.50.2" || name == "2.51.0" || name == "2.52.0" || name == "2.53.0" || name == "2.53.1" || name == "2.54.0" || name == "2.54.1" || name == "2.55.0" || name == "2.56.0" || name == "2.57.0" || name == "2.58.0")
    }
}
