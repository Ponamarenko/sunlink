package com.sunlink.svitgrid.ui.redesign

import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.model.success
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class Stage49RedesignTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun dto(vararg pairs: Pair<String, MeasurementValueDto>) = TelemetryFullDto(
        timestamp = "2026-11-23T10:00:00Z",
        categories = mapOf("pv" to pairs.toMap())
    )

    @Test
    fun `telemetryNumber returns null for missing category field and full snapshot`() {
        assertNull(telemetryNumber(null, "pv", "total_power"))
        assertNull(telemetryNumber(dto(), "pv", "total_power"))
        assertNull(telemetryNumber(dto("total_power" to MeasurementValueDto()), "pv", "total_power"))
    }

    @Test
    fun `telemetryNumber preserves zero and does not substitute fake values`() {
        val d = dto("total_power" to MeasurementValueDto(normalized_value = 0.0, confidence = "verified"))
        assertEquals(0f, telemetryNumber(d, "pv", "total_power")!!, 0.0001f)
    }

    @Test
    fun `telemetryLabel keeps quality semantics distinct`() {
        assertEquals("Немає даних", telemetryLabel(null))
        assertEquals("Підтверджено", telemetryLabel(MeasurementValueDto(confidence = "corroborated")))
        assertEquals("Попередньо", telemetryLabel(MeasurementValueDto(confidence = "provisional")))
        assertEquals("Розраховано", telemetryLabel(MeasurementValueDto(confidence = "calculated")))
    }

    @Test
    fun `isConfirmed rejects provisional stale and unavailable readings`() {
        assertTrue(isConfirmed(MeasurementValueDto(confidence = "verified"), false))
        assertTrue(isConfirmed(MeasurementValueDto(confidence = "corroborated"), false))
        assertTrue(isConfirmed(MeasurementValueDto(confidence = "calculated"), false))
        assertFalse(isConfirmed(MeasurementValueDto(confidence = "provisional"), false))
        assertFalse(isConfirmed(MeasurementValueDto(confidence = "unverified"), false))
        assertFalse(isConfirmed(MeasurementValueDto(confidence = "verified"), true))
        assertFalse(isConfirmed(MeasurementValueDto(confidence = "verified", quality = "stale"), false))
        assertFalse(isConfirmed(MeasurementValueDto(confidence = "verified", quality = "unavailable"), false))
        assertFalse(isConfirmed(null, false))
    }

    @Test
    fun `seriesSegments keeps original indices and breaks on gaps`() {
        val values = listOf(1f, 2f, null, 4f, 5f, Float.NaN, 7f, 8f, 9f)
        val segments = seriesSegments(values)
        assertEquals(3, segments.size)
        assertEquals(listOf(0 to 1f, 1 to 2f), segments[0])
        assertEquals(listOf(3 to 4f, 4 to 5f), segments[1])
        assertEquals(listOf(6 to 7f, 7 to 8f, 8 to 9f), segments[2])
    }

    @Test
    fun `seriesSegments returns no segments for a fully unavailable series`() {
        assertTrue(seriesSegments(listOf(null, null, null)).isEmpty())
        assertTrue(seriesSegments(emptyList()).isEmpty())
    }

    @Test
    fun `loadSeriesFor stores real series in shared seriesByParam state`() {
        class FakeRepo : DataRepository(null) {
            var result: Result<HistorySeriesDto> = success(
                HistorySeriesDto(parameter = "pv_total_power", period = "24h", count = 2)
            )
            override suspend fun loadHistorySeries(parameter: String, period: String): Result<HistorySeriesDto> = result
        }
        val repo = FakeRepo()
        val vm = MainViewModel(repo)
        runBlocking {
            vm.loadSeriesFor("pv_total_power", "24h")
            val stored = vm.seriesByParam.value["pv_total_power"]
            assertNotNull(stored)
            assertEquals("24h", stored!!.period)
            assertEquals(2, stored.count)
        }
    }

    @Test
    fun `redesign history parameters exist in production catalog`() {
        // Підмножина production_allowed + historical з /api/telemetry/catalog,
        // а також похідні pv_total_power/load_power, що повертають реальні точки.
        val supported = setOf(
            "battery_soc", "battery_voltage", "battery_current", "battery_power",
            "pv1_power", "pv2_power", "pv3_power", "pv_total_power",
            "grid_voltage_l1", "grid_frequency", "load_power",
            "daily_pv_energy", "daily_grid_import", "daily_grid_export",
            "daily_battery_charge", "daily_battery_discharge"
        )
        val root = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
        root.listFiles { f -> f.extension == "kt" }!!.forEach { file ->
            Regex("loadSeriesFor\\(\"([a-z0-9_]+)\"").findAll(file.readText()).forEach { match ->
                val param = match.groupValues[1]
                assertTrue("${file.name}: unsupported history parameter $param", param in supported)
            }
        }
    }

    @Test
    fun `redesign uses real telemetry and no fabricated values`() {
        val root = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
        val sources = root.listFiles { f -> f.extension == "kt" }!!.map { it.readText() }.joinToString("\n")
        assertTrue(sources.contains("telemetryNumber("))
        assertTrue(sources.contains("isConfirmed("))
        assertFalse(sources.contains("Random"))
        assertFalse(sources.contains("Math.random"))
        assertFalse(sources.contains("demo"))
    }

    @Test
    fun `navigation exposes five tabs and More section destinations`() {
        val nav = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        assertTrue(nav.contains("NavTarget(\"home\""))
        assertTrue(nav.contains("NavTarget(\"energy\""))
        assertTrue(nav.contains("NavTarget(\"battery\""))
        assertTrue(nav.contains("NavTarget(\"solar\""))
        assertTrue(nav.contains("NavTarget(\"more\""))
        assertTrue(nav.contains("startDestination = \"home\""))
        val more = File("src/main/java/com/sunlink/svitgrid/ui/redesign/MoreScreen.kt").readText()
        listOf("grid", "load", "analytics", "diagnostics", "settings").forEach {
            assertTrue("More must expose $it", more.contains("\"$it\""))
        }
    }

    @Test
    fun `version is bumped to stage 49`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 70"))
        assertTrue(gradle.contains("versionName = \"2.49.0\""))
    }
}
