package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class Stage37ScreensTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var vm: MainViewModel

    private val fullJson = """
        {"source":"deye_modbus","timestamp":"2026-11-21T10:00:00Z",
         "connection_status":true,"stale":false,
         "categories":{
           "battery":{"soc":{"raw_value":82,"normalized_value":82.0,"unit":"%",
             "confidence":"corroborated","quality":"valid","address":184,"timestamp":"2026-11-21T10:00:00Z"},
             "voltage":null,"current":null,"power":null,"temperature":null},
           "pv":{"total_power":{"normalized_value":null,"confidence":"calculated"},
             "pv1_voltage":null,"pv1_current":null,"pv1_power":null},
           "grid":{"power":null,"voltage_l1":null,"frequency":null},
           "load":{"power":null},
           "inverter":{"dc_temperature":null},
           "energy":{"daily_generation":null}
         }}
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server = MockWebServer()
        server.start()
        com.sunlink.svitgrid.network.RetrofitClient.setBaseUrl(server.url("/").toString())
        vm = MainViewModel(DataRepository(com.sunlink.svitgrid.network.RetrofitClient.getApiService()))
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun `all energy category routes exist and are unique`() {
        val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        val routes = Regex("""composable\("([^"]+)"\)""").findAll(app).map { it.groupValues[1] }.toList()
        assertEquals(routes.size, routes.toSet().size)
        listOf("dashboard", "battery", "pv", "grid", "load", "energy-flow",
            "telemetry", "statistics", "history", "diagnostics", "settings").forEach {
            assertTrue("missing route $it", routes.contains(it))
        }
    }

    @Test
    fun `category screens use full telemetry without fabrication`() {
        val dto = Gson().fromJson(fullJson, TelemetryFullDto::class.java)
        vm.onFullTelemetryLoaded(dto)
        val full = vm.fullTelemetry.value!!
        assertEquals(82.0, full.categories!!["battery"]!!["soc"]!!.normalized_value)
        assertNull(full.categories!!["battery"]!!["voltage"])
        assertNull(full.categories!!["pv"]!!["total_power"]!!.normalized_value)
        assertEquals("calculated", full.categories!!["pv"]!!["total_power"]!!.confidence)
        assertFalse(full.stale!!)
    }

    @Test
    fun `measurement value handles null and raw separately`() {
        val v = Gson().fromJson(
            """{"raw_value":82,"normalized_value":82.0,"unit":"%"}""",
            MeasurementValueDto::class.java
        )
        assertEquals(82, v.raw_value)
        assertEquals(82.0, v.normalized_value!!, 0.0)
        val empty = Gson().fromJson("{}", MeasurementValueDto::class.java)
        assertNull(empty.normalized_value)
        assertNull(empty.raw_value)
    }

    @Test
    fun `full telemetry loads from isolated mock backend`() {
        server.enqueue(MockResponse().setBody(fullJson).setHeader("Content-Type", "application/json"))
        val result = kotlinx.coroutines.runBlocking {
            DataRepository(com.sunlink.svitgrid.network.RetrofitClient.getApiService()).loadFullTelemetry()
        }
        assertTrue(result is Result.Success)
        val dto = (result as Result.Success).data
        assertEquals("deye_modbus", dto.source)
        assertEquals(82.0, dto.categories!!["battery"]!!["soc"]!!.normalized_value)
    }

    @Test
    fun `localization parity for new screen keys`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun keys(s: String) = Regex("""<string name="([^"]+)"""").findAll(s).map { it.groupValues[1] }.toSet()
        assertEquals(keys(uk), keys(en))
        listOf("screen_battery", "screen_pv", "screen_grid", "screen_load",
            "screen_energy_flow", "quick_access", "value_calculated").forEach {
            assertTrue("missing $it", keys(uk).contains(it))
        }
    }

    @Test
    fun `no hardcoded fake production values in new screens`() {
        val dir = File("src/main/java/com/sunlink/svitgrid/ui/telemetry")
        val violations = dir.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            listOf("Random", "random(", "94.0f", "85.0f").filter { token -> file.readText().contains(token) }
                .map { "${file.name}:$it" }
        }.toList()
        assertTrue("fake values: $violations", violations.isEmpty())
    }
}
