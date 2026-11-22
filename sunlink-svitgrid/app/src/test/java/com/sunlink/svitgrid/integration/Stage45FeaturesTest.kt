package com.sunlink.svitgrid.integration

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage45FeaturesTest {
    @Test
    fun `history includes all real telemetry parameters`() {
        val src = File("src/main/java/com/sunlink/svitgrid/ui/history/HistoryScreen.kt").readText()
        listOf("grid_voltage_l1", "grid_frequency", "load_power", "daily_pv_energy",
            "daily_grid_import", "daily_grid_export", "daily_battery_charge", "daily_battery_discharge")
            .forEach { assertTrue("missing history parameter $it", src.contains(it)) }
    }

    @Test
    fun `statistics includes real parameter set`() {
        val src = File("src/main/java/com/sunlink/svitgrid/ui/statistics/StatisticsScreen.kt").readText()
        listOf("battery_voltage", "grid_frequency", "daily_pv_energy", "daily_grid_import")
            .forEach { assertTrue("missing stats parameter $it", src.contains(it)) }
    }

    @Test
    fun `energy flow screen uses diagram component`() {
        val src = File("src/main/java/com/sunlink/svitgrid/ui/telemetry/CategoryScreens.kt").readText()
        assertTrue(src.contains("EnergyFlowDiagram("))
        assertTrue(src.contains("flow_hint"))
    }

    @Test
    fun `dashboard shows grid voltage frequency and load power`() {
        val src = File("src/main/java/com/sunlink/svitgrid/ui/dashboard/DashboardScreen.kt").readText()
        listOf("data.gridVoltageL1", "data.gridFrequency", "data.pLoad", "EnergySummaryCard")
            .forEach { assertTrue("missing $it", src.contains(it)) }
    }

    @Test
    fun `localization uk en parity after stage45`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun keys(s: String) = Regex("""<string name="([^"]+)"""").findAll(s).map { it.groupValues[1] }.toSet()
        assertEquals(keys(uk), keys(en))
        listOf("hist_grid_freq", "hist_load_power", "unit_hz", "unit_kwh", "energy_summary")
            .forEach { assertTrue("missing $it", keys(uk).contains(it)) }
    }

    @Test
    fun `no fake values in production ui`() {
        val root = File("src/main/java/com/sunlink/svitgrid")
        val bad = root.walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            listOf("Random", "random(", "94.0f", "85.0f").filter { f.readText().contains(it) }.map { "${f.name}:$it" }
        }.toList()
        assertTrue(bad.toString(), bad.isEmpty())
    }
}
