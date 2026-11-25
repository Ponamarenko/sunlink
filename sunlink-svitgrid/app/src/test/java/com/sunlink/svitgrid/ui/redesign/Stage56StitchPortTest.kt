package com.sunlink.svitgrid.ui.redesign

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage56StitchPortTest {

    private val root = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
    private val screenFiles = mapOf(
        "Home" to "HomeScreen.kt", "Energy" to "EnergyScreen.kt", "Battery" to "BatteryScreen.kt",
        "Solar" to "SolarScreen.kt", "Grid" to "GridScreen.kt", "Load" to "LoadScreen.kt",
        "Analytics" to "AnalyticsScreen.kt", "Diagnostics" to "DiagnosticsScreen.kt",
        "More" to "MoreScreen.kt", "Inverter" to "InverterDetailsScreen.kt",
        "AllParameters" to "AllParametersScreen.kt"
    )
    private val src = screenFiles.mapValues { File(root, it.value).readText() }

    @Test
    fun `generic stage55 components are gone from all screens`() {
        src.forEach { (name, code) ->
            listOf("HeroPanel(", "StatTile(", "DataPanel(", "InlineMetric(").forEach { token ->
                assertFalse("$name still uses $token", code.contains(token))
            }
        }
    }

    @Test
    fun `each stitch screen keeps its own composition signature`() {
        assertTrue(src["Home"]!!.contains("PremiumEnergyFlow("))
        assertTrue(File(root, "DesignSystem.kt").readText().contains("DEYE"))
        assertTrue(src["Home"]!!.contains("За сьогодні"))
        assertTrue(src["Home"]!!.contains("WebSocket"))
        assertTrue(src["Energy"]!!.contains("Покриття навантаження"))
        assertTrue(src["Energy"]!!.contains("Потоки потужності"))
        assertTrue(src["Battery"]!!.contains("Історія SOC (24 год)"))
        assertTrue(src["Battery"]!!.contains("Підтверджено"))
        assertTrue(src["Solar"]!!.contains("Вхід PV1") && src["Solar"]!!.contains("Вхід PV2") && src["Solar"]!!.contains("Вхід PV3"))
        assertTrue(src["Grid"]!!.contains("Дані мережі недоступні"))
        assertTrue(src["Load"]!!.contains("Поточне споживання будинку"))
        assertTrue(src["Analytics"]!!.contains("Порівняння за день") && src["Analytics"]!!.contains("День"))
        assertTrue(src["Diagnostics"]!!.contains("Якість телеметрії"))
        assertTrue(src["More"]!!.contains("Детальний моніторинг"))
        assertTrue(src["Inverter"]!!.contains("SUN-12K-SG02LP1-EU-AM3"))
        assertTrue(src["AllParameters"]!!.contains("Сонце (PV)"))
        val settings = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        assertTrue(settings.contains("ReadOnlyRow("))
    }

    @Test
    fun `real telemetry field mapping is preserved`() {
        val all = src.values.joinToString("\n")
        assertTrue(all.contains("telemetryNumber(") || all.contains("telemetryValue("))
        assertTrue(all.contains("full.categories") || all.contains("categories"))
        assertTrue(all.contains("seriesByParam") || all.contains("AreaChart("))
        assertFalse(all.contains("Random"))
        assertFalse(all.contains("demo"))
        assertTrue(src["Load"]!!.contains("displayLoadPower(") || all.contains("displayLoadPower("))
        assertFalse(src["Load"]!!.contains("kotlin.math.abs"))
    }

    @Test
    fun `version is bumped to stage 56`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 81"))
        assertTrue(gradle.contains("versionName = \"2.56.0\""))
    }
}
