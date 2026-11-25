package com.sunlink.svitgrid.ui.redesign

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage52InformationArchitectureTest {

    private val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
    private val more = File("src/main/java/com/sunlink/svitgrid/ui/redesign/MoreScreen.kt").readText()
    private val redesign = File("src/main/java/com/sunlink/svitgrid/ui/redesign")

    @Test
    fun `five tabs and no drawer remain`() {
        val block = app.substringAfter("val tabs = listOf(").substringBefore("    )")
        val routes = Regex("NavTarget\\(\"([a-z-]+)\"").findAll(block).map { it.groupValues[1] }.toList()
        assertEquals(listOf("home", "energy", "battery", "solar", "more"), routes)
        assertFalse(app.contains("ModalNavigationDrawer("))
        assertFalse(app.contains("TopAppBar("))
    }

    @Test
    fun `More is a short technical menu without duplicates`() {
        val routes = listOf("grid", "load", "analytics", "diagnostics", "settings", "inverter-details", "all-parameters")
        routes.forEach { assertTrue("More must expose $it", more.contains("\"$it\"")) }
        assertEquals(routes.size, routes.toSet().size)
        assertTrue(more.contains("Вторинні функції"))
        assertFalse(more.contains("nav_about"))
    }

    @Test
    fun `fake forecast and duplicate about screen are removed from navigation`() {
        assertFalse(File("src/main/java/com/sunlink/svitgrid/ui/forecast/ForecastScreen.kt").exists())
        assertTrue(app.contains("composable(\"forecast\") { LegacyRedirect(navController, \"forecast\", \"home\") }"))
        assertTrue(app.contains("composable(\"about\") { LegacyRedirect(navController, \"about\", \"settings\") }"))
        assertFalse(app.contains("fun AboutScreen("))
        val sources = redesign.listFiles { f -> f.extension == "kt" }!!.map { it.readText() }.joinToString("\n")
        listOf("☀️", "💧", "💨", "⚡", "☁️", "🔋", "🔌", "15-22°C").forEach {
            assertFalse("emoji/fake weather token $it still present", sources.contains(it))
        }
    }

    @Test
    fun `debug is developer only`() {
        assertTrue(app.contains("if (BuildConfig.DEBUG) DebugScreen()"))
        assertTrue(app.contains("LegacyRedirect(navController, \"debug\", \"settings\")"))
        assertFalse(more.contains("\"debug\""))
    }

    @Test
    fun `all legacy routes still redirect safely`() {
        mapOf(
            "dashboard" to "home", "pv" to "solar", "energy-flow" to "energy",
            "telemetry" to "all-parameters", "statistics" to "analytics",
            "history" to "analytics", "inverter-status" to "inverter-details",
            "live" to "home", "forecast" to "home", "about" to "settings"
        ).forEach { (from, to) ->
            assertTrue(app.contains("composable(\"$from\") { LegacyRedirect(navController, \"$from\", \"$to\") }"))
        }
    }

    @Test
    fun `Energy no longer duplicates Home metric cards and charts`() {
        val energy = File("src/main/java/com/sunlink/svitgrid/ui/redesign/EnergyScreen.kt").readText()
        assertFalse(energy.contains("MetricCard("))
        assertFalse(energy.contains("AnalyticsChart("))
        assertFalse(energy.contains("loadSeriesFor("))
        assertTrue(energy.contains("onNavigate(\"analytics\")"))
        assertTrue(energy.contains("Потоки потужності"))
    }

    @Test
    fun `telemetry semantics units and signed load are preserved`() {
        val helpers = File("src/main/java/com/sunlink/svitgrid/ui/redesign/TelemetryHelpers.kt").readText()
        assertTrue(helpers.contains("fun isConfirmed("))
        assertTrue(helpers.contains("fun seriesSegments("))
        assertTrue(helpers.contains("is_signed=true"))
        val load = File("src/main/java/com/sunlink/svitgrid/ui/redesign/LoadScreen.kt").readText()
        assertTrue(load.contains("displayLoadPower("))
        assertFalse(load.contains("kotlin.math.abs"))
        val sources = redesign.listFiles { f -> f.extension == "kt" }!!.map { it.readText() }.joinToString("\n")
        assertTrue(sources.contains("\"W\""))
        assertTrue(sources.contains("kWh"))
        assertTrue(sources.contains("\"V\""))
        assertTrue(sources.contains("\"A\""))
        assertTrue(sources.contains("\"%\""))
        assertTrue(sources.contains("Гц") || sources.contains("Hz"))
        assertFalse(sources.contains("Random"))
        assertFalse(sources.contains("demo"))
    }

    @Test
    fun `version is bumped to stage 52`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 82"))
        assertTrue(gradle.contains("versionName = \"2.57.0\""))
    }
}
