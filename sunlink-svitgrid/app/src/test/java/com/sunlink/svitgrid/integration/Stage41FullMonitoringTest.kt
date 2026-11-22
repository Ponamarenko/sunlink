package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.TelemetryQualityDto
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class Stage41FullMonitoringTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `quality dto parses counts and active registers`() {
        val dto = Gson().fromJson(
            """{"source":"deye_modbus","known_parameters":80,"available_parameters":1,
                "unavailable_parameters":79,"active_registers":[184],
                "categories":{"battery":{"total":10,"available":1}}}""",
            TelemetryQualityDto::class.java
        )
        assertEquals(80, dto.known_parameters)
        assertEquals(1, dto.available_parameters)
        assertEquals(listOf(184), dto.active_registers)
        assertEquals(10, dto.categories["battery"]?.total)
    }

    @Test
    fun `all parameter groups present in full telemetry dto`() {
        val json = """
            {"source":"deye_modbus","timestamp":"t","categories":{
              "battery":{},"pv":{},"grid":{},"load":{},"inverter":{},"energy":{},
              "status":{},"warnings":{},"errors":{},"system":{}}}
        """.trimIndent()
        val dto = Gson().fromJson(json, TelemetryFullDto::class.java)
        assertEquals(10, dto.categories!!.size)
    }

    @Test
    fun `all parameters and inverter routes are unique`() {
        val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        val routes = Regex("""composable\("([^"]+)"\)""").findAll(app).map { it.groupValues[1] }.toList()
        assertEquals(routes.size, routes.toSet().size)
        listOf("all-parameters", "inverter-status", "telemetry", "diagnostics").forEach {
            assertTrue("missing $it", routes.contains(it))
        }
    }

    @Test
    fun `localization keeps parity after stage 41 keys`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun keys(s: String) = Regex("""<string name="([^"]+)"""").findAll(s).map { it.groupValues[1] }.toSet()
        assertEquals(keys(uk), keys(en))
        listOf("all_parameters", "quality_title", "filter_verified").forEach {
            assertTrue("missing $it", keys(uk).contains(it))
        }
    }

    @Test
    fun `viewmodel quality starts empty without fabricated values`() {
        val vm = MainViewModel(com.sunlink.svitgrid.data.repository.DataRepository(null))
        assertNull(vm.telemetryQuality.value)
        assertNull(vm.fullTelemetryError.value)
    }
}
