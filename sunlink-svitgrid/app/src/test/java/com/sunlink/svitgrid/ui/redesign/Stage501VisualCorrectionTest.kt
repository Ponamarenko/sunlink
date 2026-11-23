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
        assertTrue(activity.contains("getOrDefault(ThemeMode.DARK)"))
        assertTrue(activity.contains("isAppearanceLightStatusBars = false"))
        assertTrue(activity.contains("isAppearanceLightNavigationBars = false"))

        val theme = File("src/main/java/com/sunlink/svitgrid/ui/theme/SvitGridTheme.kt").readText()
        assertTrue(theme.contains("ThemeMode.SYSTEM -> true"))
        assertTrue(theme.contains("fun PremiumAnalyticsTheme"))
        assertTrue(theme.contains("fun PremiumDiagnosticsTheme"))

        val v27 = File("src/main/res/values-v27/themes.xml").readText()
        assertTrue(v27.contains("android:windowLightNavigationBar\">false"))
    }

    @Test
    fun `home fixes title clipping time and bottom overlap`() {
        val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        assertTrue(app.contains("Один рядок"))
        assertFalse(app.contains("tabs.find { it.route == currentRoute }?.let"))

        val home = File("src/main/java/com/sunlink/svitgrid/ui/redesign/HomeScreen.kt").readText()
        assertTrue(home.contains("formatTelemetryTimestamp(full?.timestamp)"))
        assertTrue(home.contains("compact = true"))

        val ds = File("src/main/java/com/sunlink/svitgrid/ui/redesign/DesignSystem.kt").readText()
        assertTrue(ds.contains("Spacer(Modifier.height(32.dp))"))
        assertTrue(ds.contains("navigationBarsPadding()"))
        assertTrue(ds.contains("compact: Boolean = false"))
    }

    @Test
    fun `analytics and diagnostics keep light themes while main screens stay dark`() {
        val analytics = File("src/main/java/com/sunlink/svitgrid/ui/redesign/AnalyticsScreen.kt").readText()
        assertTrue(analytics.contains("PremiumAnalyticsTheme"))
        val diagnostics = File("src/main/java/com/sunlink/svitgrid/ui/redesign/DiagnosticsScreen.kt").readText()
        assertTrue(diagnostics.contains("PremiumDiagnosticsTheme"))
        val home = File("src/main/java/com/sunlink/svitgrid/ui/redesign/HomeScreen.kt").readText()
        assertFalse(home.contains("PremiumAnalyticsTheme"))
    }

    @Test
    fun `version is bumped to stage 50 dot 1`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 72"))
        assertTrue(gradle.contains("versionName = \"2.50.1\""))
    }
}
