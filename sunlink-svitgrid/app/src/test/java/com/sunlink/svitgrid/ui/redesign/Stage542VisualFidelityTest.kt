package com.sunlink.svitgrid.ui.redesign

import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.HistorySeriesPointDto
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage542VisualFidelityTest {

    private val root = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
    private fun s(name: String) = File(root, name).readText()

    private fun point(ts: String, value: Double?, gap: Boolean = false) =
        HistorySeriesPointDto(ts, value, gap)

    @Test
    fun `area charts are used by battery solar and load`() {
        listOf("BatteryScreen.kt", "SolarScreen.kt", "LoadScreen.kt").forEach { name ->
            assertTrue("$name must use AreaChart", s(name).contains("AreaChart("))
            assertFalse("$name still uses AnalyticsChart", s(name).contains("AnalyticsChart("))
        }
        val charts = s("Charts.kt")
        assertTrue(charts.contains("fun AreaChart("))
        assertTrue(charts.contains("AREA_TOP_ALPHA = 0.28f"))
        assertTrue(charts.contains("PathEffect.dashPathEffect"))
        assertTrue(charts.contains("drawPath("))
        assertTrue(charts.contains("seriesSegments(values)"))
    }

    @Test
    fun `analytics uses paired bar chart aligned by timestamp`() {
        val analytics = s("AnalyticsScreen.kt")
        assertTrue(analytics.contains("AnalyticsBarChart("))
        assertTrue(analytics.contains("alignBarBuckets("))
        assertFalse(analytics.contains("AnalyticsChart(pvValues"))
        assertTrue(s("Charts.kt").contains("fun AnalyticsBarChart("))

        val pv = HistorySeriesDto(points = listOf(
            point("2026-11-24T06:10:00Z", 1.0),
            point("2026-11-24T07:10:00Z", 2.0)
        ))
        val load = HistorySeriesDto(points = listOf(
            point("2026-11-24T07:05:00Z", 3.0),
            point("2026-11-24T06:05:00Z", 4.0)
        ))
        val buckets = alignBarBuckets(pv, load, BarBucketSize.HOUR)
        assertEquals(2, buckets.size)
        assertEquals(1f, buckets[0].pv!!, 0.001f)
        assertEquals(4f, buckets[0].load!!, 0.001f)
        assertEquals(2f, buckets[1].pv!!, 0.001f)
        assertEquals(3f, buckets[1].load!!, 0.001f)
    }

    @Test
    fun `gaps and missing buckets never become bars`() {
        val pv = HistorySeriesDto(points = listOf(
            point("2026-11-24T06:10:00Z", 5.0),
            point("2026-11-24T06:20:00Z", null, gap = true)
        ))
        val buckets = alignBarBuckets(pv, null, BarBucketSize.HOUR)
        assertEquals(1, buckets.size)
        assertEquals(5f, buckets[0].pv!!, 0.001f)
        assertNull(buckets[0].load)
        assertTrue(s("Charts.kt").contains("if (value == null)"))
        assertTrue(s("Charts.kt").contains("Stroke(width = 1.dp.toPx(), pathEffect = dash)"))
    }

    @Test
    fun `home flow matches stitch straight cross and DEYE hub`() {
        val ds = s("DesignSystem.kt")
        assertTrue(ds.contains("Сонце (PV)"))
        assertTrue(ds.contains("Батарея"))
        assertTrue(ds.contains("Мережа"))
        assertTrue(ds.contains("Будинок"))
        assertTrue(ds.contains("DEYE"))
        assertTrue(ds.contains("quadraticBezierTo") == false)
        assertTrue(ds.contains("drawLine("))
        assertTrue(ds.contains("PathEffect.dashPathEffect"))
    }

    @Test
    fun `premium card uses border and no top accent bar`() {
        val ds = s("DesignSystem.kt")
        assertTrue(ds.contains("BorderStroke(1.dp, borderColor)"))
        assertTrue(ds.contains("colors.surface.copy(alpha = 0.88f)"))
        val cardBlock = ds.substringAfter("fun PremiumCard(").substringBefore("fun SunLinkCard(")
        assertFalse(cardBlock.contains("height(3.dp)"))
    }

    @Test
    fun `grid diagnostics link is wired in navigation`() {
        val grid = s("GridScreen.kt")
        assertTrue(grid.contains("onNavigate(\"diagnostics\")"))
        val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        assertTrue(app.contains("GridScreen(vm = viewModel, onNavigate = { go(it) })"))
    }

    @Test
    fun `original stitch fonts are bundled`() {
        val res = File("src/main/res/font")
        listOf("mona_sans_400.ttf", "mona_sans_600.ttf", "mona_sans_700.ttf",
            "commissioner_400.ttf", "commissioner_500.ttf", "commissioner_600.ttf",
            "red_hat_mono_400.ttf", "red_hat_mono_500.ttf").forEach {
            assertTrue("missing font $it", File(res, it).exists())
        }
        val typo = File("src/main/java/com/sunlink/svitgrid/ui/designsystem/SunLinkTypography.kt").readText()
        assertTrue(typo.contains("R.font.mona_sans_700"))
        assertTrue(typo.contains("R.font.commissioner_400"))
        assertTrue(typo.contains("R.font.red_hat_mono_500"))
    }

    @Test
    fun `real telemetry rules preserved`() {
        val sources = root.listFiles { f -> f.extension == "kt" }!!.map { it.readText() }.joinToString("\n")
        assertTrue(sources.contains("isConfirmed("))
        assertTrue(sources.contains("point.gap") || sources.contains("seriesSegments("))
        assertTrue(sources.contains("displayLoadPower("))
        assertFalse(sources.contains("Random"))
        assertFalse(sources.contains("demo"))
    }

    @Test
    fun `version is bumped to stage 54 dot 2`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 83"))
        assertTrue(gradle.contains("versionName = \"2.58.0\""))
    }
}
