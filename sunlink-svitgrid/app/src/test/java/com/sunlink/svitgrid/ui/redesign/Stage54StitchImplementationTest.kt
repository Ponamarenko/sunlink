package com.sunlink.svitgrid.ui.redesign

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage54StitchImplementationTest {

    private val root = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
    private val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
    private val sources = root.listFiles { f -> f.extension == "kt" }!!.map { it.readText() }.joinToString("\n")

    @Test
    fun `all twelve stitch screens have compose implementations`() {
        val files = listOf(
            "HomeScreen.kt", "EnergyScreen.kt", "BatteryScreen.kt", "SolarScreen.kt",
            "GridScreen.kt", "LoadScreen.kt", "AnalyticsScreen.kt", "DiagnosticsScreen.kt",
            "MoreScreen.kt", "InverterDetailsScreen.kt", "AllParametersScreen.kt"
        )
        files.forEach { assertTrue("missing $it", File(root, it).exists()) }
        assertTrue("SettingsScreen lives in SvitGridApp", app.contains("fun SettingsScreen("))
        assertEquals(11, files.size)
    }

    @Test
    fun `twelve stitch destinations are routed`() {
        listOf("home", "energy", "battery", "solar", "more", "grid", "load",
            "analytics", "diagnostics", "settings", "inverter-details", "all-parameters")
            .forEach { assertTrue("missing route $it", app.contains("composable(\"$it\")")) }
        assertTrue(app.contains("redesign.InverterDetailsScreen"))
        assertTrue(app.contains("redesign.AllParametersScreen"))
    }

    @Test
    fun `all screens stay in the single dark theme`() {
        assertFalse(sources.contains("PremiumAnalyticsTheme"))
        assertFalse(sources.contains("PremiumDiagnosticsTheme"))
        assertFalse(sources.contains("lightColorScheme"))
        assertFalse(sources.contains("sunLinkLightColors"))
        assertTrue(File("src/main/java/com/sunlink/svitgrid/ui/theme/SvitGridTheme.kt").readText()
            .contains("colorScheme = DarkColorScheme"))
    }

    @Test
    fun `stitch content details are present`() {
        assertTrue(File(root, "HomeScreen.kt").readText().contains("За сьогодні"))
        assertTrue(File(root, "EnergyScreen.kt").readText().contains("Баланс і потоки") || File(root, "EnergyScreen.kt").readText().contains("Покриття навантаження"))
        assertTrue(File(root, "BatteryScreen.kt").readText().contains("Стан накопичувача"))
        assertTrue(File(root, "SolarScreen.kt").readText().contains("Вхід PV1"))
        assertTrue(File(root, "GridScreen.kt").readText().contains("Дані мережі недоступні"))
        assertTrue(File(root, "LoadScreen.kt").readText().contains("Поточне споживання будинку"))
        val analytics = File(root, "AnalyticsScreen.kt").readText()
        assertTrue(analytics.contains("День") && analytics.contains("Тиждень") &&
            analytics.contains("Місяць") && analytics.contains("Рік"))
        assertTrue(analytics.contains("rememberSaveable"))
        assertTrue(File(root, "DiagnosticsScreen.kt").readText().contains("Якість телеметрії"))
        assertTrue(File(root, "MoreScreen.kt").readText().contains("Детальний моніторинг"))
        assertTrue(File(root, "InverterDetailsScreen.kt").readText().contains("SUN-12K-SG02LP1-EU-AM3"))
        val all = File(root, "AllParametersScreen.kt").readText()
        assertTrue(all.contains("search") || all.contains("Пошук"))
        assertTrue(all.contains("Батарея") && all.contains("Сонце (PV)") && all.contains("Система"))
    }

    @Test
    fun `settings are read only for server and persist user preferences`() {
        val settings = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        assertTrue(settings.contains("Лише перегляд"))
        assertFalse(settings.contains("settings_invalid_address"))
        assertFalse(settings.contains("onValueChange = { serverAddress"))
        assertTrue(settings.contains("repo.pollIntervalSeconds = seconds"))
        assertTrue(settings.contains("repo.alertsEnabled = it"))
        assertTrue(settings.contains("repo.lowSocWarningEnabled = it"))
        assertTrue(settings.contains("repo.lowSocThreshold = threshold"))
        val repo = File("src/main/java/com/sunlink/svitgrid/data/SettingsRepository.kt").readText()
        assertTrue(repo.contains("KEY_POLL_INTERVAL"))
        assertTrue(repo.contains("KEY_ALERTS_ENABLED"))
    }

    @Test
    fun `real telemetry and honest states survive the redesign`() {
        assertTrue(sources.contains("telemetryNumber("))
        assertTrue(sources.contains("isConfirmed("))
        assertTrue(sources.contains("point.gap") || sources.contains("seriesSegments("))
        assertTrue(sources.contains("Freshness") || sources.contains("freshnessOf("))
        assertFalse(sources.contains("Random"))
        assertFalse(sources.contains("demo"))
        assertTrue(File(root, "LoadScreen.kt").readText().contains("displayLoadPower("))
        assertFalse(File(root, "LoadScreen.kt").readText().contains("kotlin.math.abs"))
    }

    @Test
    fun `version is bumped to stage 54`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 80"))
        assertTrue(gradle.contains("versionName = \"2.55.0\""))
    }
}
