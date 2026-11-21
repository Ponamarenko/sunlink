package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.HealthDto
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.StatsSummaryDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.VersionDto
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.network.ApiService
import com.sunlink.svitgrid.network.RetrofitClient
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

@OptIn(ExperimentalCoroutinesApi::class)
class ComprehensiveAndroidTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var api: ApiService

    private val fullJson = """
        {"source":"deye_modbus","timestamp":"2026-11-21T10:00:00+00:00",
         "connection_status":true,"stale":false,"inverter_id":"deye_sg02lp1_am3",
         "categories":{"battery":{"soc":{"raw_value":82,"normalized_value":82.0,
         "unit":"%","confidence":"corroborated","quality":"valid","address":184,
         "timestamp":"2026-11-21T10:00:00+00:00"},"voltage":null},
         "pv":{"total_power":{"normalized_value":null,"confidence":"calculated"}}}}
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server = MockWebServer()
        server.start()
        RetrofitClient.setBaseUrl(server.url("/").toString())
        api = RetrofitClient.getApiService()
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun `full telemetry dto keeps nulls and provenance`() {
        val dto = Gson().fromJson(fullJson, TelemetryFullDto::class.java)
        assertEquals("deye_modbus", dto.source)
        assertFalse(dto.stale!!)
        val battery = dto.categories!!["battery"]!!
        assertEquals(82.0, battery["soc"]!!.normalized_value)
        assertNull(battery["voltage"])
        assertEquals("calculated", dto.categories!!["pv"]!!["total_power"]!!.confidence)
        assertNull(dto.categories!!["pv"]!!["total_power"]!!.normalized_value)
    }

    @Test
    fun `measurement value dto parses raw separately from normalized`() {
        val value = Gson().fromJson(
            """{"raw_value":82,"normalized_value":82.0,"unit":"%","address":184}""",
            MeasurementValueDto::class.java
        )
        assertEquals(82, value.raw_value)
        assertEquals(82.0, value.normalized_value!!, 0.0)
        assertEquals(184, value.address)
    }

    @Test
    fun `stats health and version dtos parse`() {
        val stats = Gson().fromJson(
            """{"parameter":"battery_soc","count":0,"min":null,"max":null}""",
            StatsSummaryDto::class.java
        )
        assertEquals(0, stats.count)
        assertNull(stats.min)
        val health = Gson().fromJson("""{"status":"ok","read_only":true}""", HealthDto::class.java)
        assertEquals("ok", health.status)
        assertTrue(health.read_only!!)
        val version = Gson().fromJson("""{"api":"2.1.0","protocol":"read-only FC03"}""", VersionDto::class.java)
        assertEquals("2.1.0", version.api)
    }

    @Test
    fun `repository loads full telemetry and statistics from isolated server`() {
        server.enqueue(MockResponse().setBody(fullJson).setHeader("Content-Type", "application/json"))
        val repo = DataRepository(api)
        val result = kotlinx.coroutines.runBlocking { repo.loadFullTelemetry() }
        assertTrue(result is Result.Success)
        assertEquals(82.0, (result as Result.Success).data.categories!!["battery"]!!["soc"]!!.normalized_value)

        server.enqueue(MockResponse().setBody("""{"parameter":"battery_soc","count":2,"min":80.0,"max":82.0,"avg":81.0,"sum":162.0}""").setHeader("Content-Type", "application/json"))
        val stats = kotlinx.coroutines.runBlocking { repo.loadStats("battery_soc", "2026-01-01T00:00:00Z", "2027-01-01T00:00:00Z") }
        assertTrue(stats is Result.Success)
        assertEquals(2, (stats as Result.Success).data.count)
    }

    @Test
    fun `viewmodel exposes full telemetry and stats`() {
        val vm = MainViewModel(DataRepository(api))
        vm.onFullTelemetryLoaded(
            Gson().fromJson(fullJson, TelemetryFullDto::class.java)
        )
        assertEquals(82.0, vm.fullTelemetry.value!!.categories!!["battery"]!!["soc"]!!.normalized_value)
    }
}
