package com.sunlink.svitgrid.ui.redesign

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage55VisualRedesignTest {

    private val root = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
    private fun s(name: String) = File(root, name).readText()
    private val allScreens = listOf(
        "HomeScreen.kt", "EnergyScreen.kt", "BatteryScreen.kt", "SolarScreen.kt",
        "GridScreen.kt", "LoadScreen.kt", "AnalyticsScreen.kt", "DiagnosticsScreen.kt",
        "MoreScreen.kt", "InverterDetailsScreen.kt", "AllParametersScreen.kt"
    ).map { it to s(it) }.toMap()

    @Test
    fun `layered surface roles replace uniform card stacks`() {
        listOf("HomeScreen.kt", "BatteryScreen.kt", "SolarScreen.kt", "GridScreen.kt", "LoadScreen.kt", "AnalyticsScreen.kt")
            .forEach { name ->
                listOf("HeroPanel(", "StatTile(", "DataPanel(", "InlineMetric(").forEach { token ->
                    assertFalse("$name still uses generic $token", allScreens[name]!!.contains(token))
                }
            }
        val ds = s("DesignSystem.kt")
        assertTrue(ds.contains("fun PremiumEnergyFlow("))
    }

    @Test
    fun `home flow has atmospheric depth and no primitive placeholder`() {
        val ds = s("DesignSystem.kt")
        assertTrue(ds.contains("Brush.verticalGradient(listOf(colors.surface, colors.background))"))
        assertFalse(ds.contains("drawCircle(Color(0xFFF5B942).copy(alpha = 0.07f)"))
        assertFalse(ds.contains("drawCircle(Color(0xFF10B981).copy(alpha = 0.05f)"))
        assertFalse(ds.contains("drawCircle(Color(0xFFF59E0B).copy(alpha = 0.08f)"))
        assertTrue(ds.contains("horizon glow"))
        assertTrue(ds.contains("DEYE"))
        assertTrue(ds.contains("drawLine("))
    }

    @Test
    fun `professional charts remain intact`() {
        val charts = s("Charts.kt")
        assertTrue(charts.contains("fun AreaChart("))
        assertTrue(charts.contains("fun AnalyticsBarChart("))
        assertTrue(charts.contains("alignBarBuckets("))
        assertTrue(allScreens["BatteryScreen.kt"]!!.contains("AreaChart("))
        assertTrue(allScreens["SolarScreen.kt"]!!.contains("AreaChart("))
        assertTrue(allScreens["LoadScreen.kt"]!!.contains("AreaChart("))
        assertTrue(allScreens["AnalyticsScreen.kt"]!!.contains("AnalyticsBarChart("))
    }

    @Test
    fun `dark theme and real telemetry are preserved`() {
        val sources = allScreens.values.joinToString("\n") + s("DesignSystem.kt") + s("Charts.kt")
        assertFalse(sources.contains("PremiumAnalyticsTheme"))
        assertFalse(sources.contains("lightColorScheme"))
        assertFalse(sources.contains("Random"))
        assertFalse(sources.contains("demo"))
        assertTrue(sources.contains("isConfirmed(") || sources.contains("freshnessOf("))
        assertTrue(allScreens["LoadScreen.kt"]!!.contains("displayLoadPower("))
        assertFalse(allScreens["LoadScreen.kt"]!!.contains("kotlin.math.abs"))
    }

    @Test
    fun `original stitch fonts are bundled`() {
        val res = File("src/main/res/font")
        listOf("mona_sans_400.ttf", "mona_sans_600.ttf", "mona_sans_700.ttf",
            "commissioner_400.ttf", "commissioner_500.ttf", "commissioner_600.ttf",
            "red_hat_mono_400.ttf", "red_hat_mono_500.ttf").forEach {
            assertTrue("missing $it", File(res, it).exists())
        }
    }

    @Test
    fun `version is bumped to stage 55`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 82"))
        assertTrue(gradle.contains("versionName = \"2.57.0\""))
    }
}
