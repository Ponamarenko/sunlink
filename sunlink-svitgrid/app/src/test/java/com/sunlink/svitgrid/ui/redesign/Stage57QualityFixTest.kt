package com.sunlink.svitgrid.ui.redesign

import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.HistorySeriesPointDto
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage57QualityFixTest {

    @Test
    fun `physically impossible SOC samples become gaps not max values`() {
        assertTrue(isValidSoc(93f))
        assertTrue(isValidSoc(0f))
        assertTrue(isValidSoc(100f))
        assertFalse(isValidSoc(6634.8f))
        assertFalse(isValidSoc(6511.2f))
        assertFalse(isValidSoc(-1f))
        assertFalse(isValidSoc(Float.NaN))
        assertFalse(isValidSoc(null))

        val series = HistorySeriesDto(points = listOf(
            HistorySeriesPointDto("2026-11-25T06:50:00Z", 93.0),
            HistorySeriesPointDto("2026-11-25T06:55:00Z", 6634.8),
            HistorySeriesPointDto("2026-11-25T07:00:00Z", 94.0, gap = true)
        ))
        val values = sanitizedSocSeries(series)
        assertEquals(listOf(93f, null, null), values)
        assertEquals(93f, values.filterNotNull().maxOrNull()!!, 0.001f)
    }

    @Test
    fun `solar pv statuses are derived from telemetry not hardcoded`() {
        val solar = File("src/main/java/com/sunlink/svitgrid/ui/redesign/SolarScreen.kt").readText()
        assertFalse(solar.contains("PvInputCard(\"Вхід PV1\", \"Підтверджено\""))
        assertFalse(solar.contains("PvInputCard(\"Вхід PV2\", \"Очікування\""))
        assertFalse(solar.contains("PvInputCard(\"Вхід PV3\", \"Недоступно\""))
        assertTrue(solar.contains("pvVariant(full, stale, \"pv1\")"))
        assertTrue(solar.contains("pvVariant(full, stale, \"pv2\")"))
        assertTrue(solar.contains("pvVariant(full, stale, \"pv3\")"))
    }

    @Test
    fun `home has no huge decorative circles`() {
        val ds = File("src/main/java/com/sunlink/svitgrid/ui/redesign/DesignSystem.kt").readText()
        assertFalse(ds.contains("drawCircle(Color(0xFFF5B942).copy(alpha = 0.07f)"))
        assertFalse(ds.contains("drawCircle(Color(0xFF10B981).copy(alpha = 0.05f)"))
        assertFalse(ds.contains("drawCircle(Color(0xFFF59E0B).copy(alpha = 0.08f)"))
        assertTrue(ds.contains("horizon glow"))
    }

    @Test
    fun `version is bumped to stage 57`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 82"))
        assertTrue(gradle.contains("versionName = \"2.57.0\""))
    }
}
