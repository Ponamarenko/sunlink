package com.sunlink.svitgrid.ui.redesign

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage51ProductCleanupTest {

    private val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
    private val more = File("src/main/java/com/sunlink/svitgrid/ui/redesign/MoreScreen.kt").readText()
    private val home = File("src/main/java/com/sunlink/svitgrid/ui/redesign/HomeScreen.kt").readText()

    @Test
    fun `bottom navigation has exactly five main tabs`() {
        val block = app.substringAfter("val tabs = listOf(").substringBefore("    )")
        val routes = Regex("NavTarget\\(\"([a-z-]+)\"").findAll(block).map { it.groupValues[1] }.toList()
        assertEquals(listOf("home", "energy", "battery", "solar", "more"), routes)
    }

    @Test
    fun `drawer and duplicate top bar are gone`() {
        assertFalse(app.contains("ModalNavigationDrawer("))
        assertFalse(app.contains("TopAppBar("))
        assertFalse(app.contains("NavigationDrawerItem("))
        assertTrue(app.contains("PremiumBottomNavigation("))
        assertTrue(app.contains("WindowInsets.systemBars.only(WindowInsetsSides.Top)"))
    }

    @Test
    fun `more has no duplicate routes and no main tabs`() {
        val routes = Regex("MoreItem\\(\"([a-z-]+)\"").findAll(more).map { it.groupValues[1] }.toList()
        assertEquals(routes.size, routes.toSet().size)
        listOf("home", "energy", "battery", "solar", "more").forEach {
            assertFalse("More must not duplicate main tab $it", routes.contains(it))
        }
        listOf("grid", "load", "analytics", "diagnostics", "settings",
            "inverter-details", "all-parameters").forEach {
            assertTrue("More must expose $it", routes.contains(it))
        }
        listOf("energy-flow", "history", "statistics", "telemetry",
            "inverter-status", "live", "forecast", "about", "debug").forEach {
            assertFalse("Obsolete item $it still visible", routes.contains(it))
        }
    }

    @Test
    fun `legacy routes are preserved as safe redirects`() {
        val aliases = mapOf(
            "dashboard" to "home",
            "pv" to "solar",
            "energy-flow" to "energy",
            "telemetry" to "all-parameters",
            "statistics" to "analytics",
            "history" to "analytics",
            "inverter-status" to "inverter-details",
            "live" to "home"
        )
        aliases.forEach { (from, to) ->
            assertTrue("missing redirect $from", app.contains("composable(\"$from\") { LegacyRedirect(navController, \"$from\", \"$to\") }"))
        }
        assertTrue(app.contains("popUpTo(from) { inclusive = true }"))
    }

    @Test
    fun `home cards open detailed screens`() {
        mapOf(
            "energy" to "onNavigate(\"energy\")",
            "solar" to "onNavigate(\"solar\")",
            "battery" to "onNavigate(\"battery\")",
            "grid" to "onNavigate(\"grid\")",
            "load" to "onNavigate(\"load\")"
        ).forEach { (route, call) ->
            assertTrue("Home card must open $route", home.contains(call))
        }
        assertTrue(home.contains("onNavigate: (String) -> Unit = {}"))
    }

    @Test
    fun `real telemetry rules and signed load are preserved`() {
        val helpers = File("src/main/java/com/sunlink/svitgrid/ui/redesign/TelemetryHelpers.kt").readText()
        assertTrue(helpers.contains("fun telemetryNumber("))
        assertTrue(helpers.contains("fun isConfirmed("))
        assertTrue(helpers.contains("fun seriesSegments("))
        assertTrue(helpers.contains("is_signed=true"))
        val load = File("src/main/java/com/sunlink/svitgrid/ui/redesign/LoadScreen.kt").readText()
        assertTrue(load.contains("displayLoadPower("))
        assertFalse(load.contains("kotlin.math.abs"))
        val sources = File("src/main/java/com/sunlink/svitgrid/ui/redesign").listFiles { f -> f.extension == "kt" }!!
            .map { it.readText() }.joinToString("\n")
        assertFalse(sources.contains("Random"))
        assertFalse(sources.contains("demo"))
    }

    @Test
    fun `version is bumped to stage 51`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 75"))
        assertTrue(gradle.contains("versionName = \"2.52.0\""))
    }
}
