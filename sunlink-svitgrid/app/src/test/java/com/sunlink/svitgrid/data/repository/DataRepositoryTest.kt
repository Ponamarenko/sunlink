package com.sunlink.svitgrid.data.repository

import com.sunlink.svitgrid.data.dto.*
import com.sunlink.svitgrid.network.ApiService
import com.sunlink.svitgrid.data.model.Result
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson

/**
 * Тести DataRepository з використанням MockWebServer.
 * Не робить реальних мережевих запитів.
 */
class DataRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService
    private lateinit var repository: DataRepository
    private val gson = Gson()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val baseUrl = mockWebServer.url("/").toString()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)
        repository = DataRepository(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `loadInverterData success`() {
        runBlocking {
            val json = """{
                "p_sun": 1500.0,
                "p_load": 1000.0,
                "p_batt": -100.0,
                "p_grid": 400.0,
                "batt_soc": 86.0,
                "battery_voltage": 52.5,
                "battery_current": -2.0,
                "dc_temperature": 45.0,
                "ac_temperature": 40.0,
                "pv1_power": 800.0,
                "pv2_power": 700.0,
                "pv3_power": 0.0,
                "grid_voltage_l1": 230.0,
                "timestamp": "2026-11-18T10:00:00Z",
                "connection_status": true,
                "error": null
            }"""
            mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"))

            val result = repository.loadInverterData()
            assertTrue(result is Result.Success)
            val (data, status) = (result as Result.Success).data
            assertEquals(1500.0f, data.pSun!!, 0.001f)
            assertEquals(86.0f, data.batterySoc!!, 0.001f)
            assertTrue(status.online)
            assertTrue(data.hasMeasurements)
        }
    }

    @Test
    fun `loadInverterData http error`() {
        runBlocking {
            mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"))

            val result = repository.loadInverterData()
            assertTrue(result is Result.Error)
        }
    }

    @Test
    fun `loadStatus success`() {
        runBlocking {
            val json = """{
                "connection_status": true,
                "last_update": "2026-11-18T10:00:00Z",
                "inverter_id": "deye_sg02lp1_am3",
                "available_registers": 9,
                "registers": [90, 91, 183, 184, 186, 187, 188, 190, 191]
            }"""
            mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"))

            val result = repository.loadStatus()
            assertTrue(result is Result.Success)
            val status = (result as Result.Success).data
            assertTrue(status.online)
            assertEquals("deye_sg02lp1_am3", status.inverterId)
            assertEquals(9, status.availableRegisters)
        }
    }

    @Test
    fun `loadLatestMeasurements success`() {
        runBlocking {
            val json = """{
                "inverter_id": "deye_sg02lp1_am3",
                "count": 1,
                "measurements": [
                    {
                        "parameter_name": "battery_soc",
                        "raw_value": 86,
                        "normalized_value": 86.0,
                        "unit": "%",
                        "timestamp": "2026-11-18T10:00:00Z",
                        "quality": "valid",
                        "confidence": "corroborated",
                        "source": "modbus_register"
                    }
                ]
            }"""
            mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"))

            val result = repository.loadLatestMeasurements()
            assertTrue(result is Result.Success)
            val dto = (result as Result.Success).data
            assertEquals(1, dto.count)
            assertEquals("battery_soc", dto.measurements[0].parameter_name)
            assertEquals(86.0, dto.measurements[0].normalized_value!!, 0.001)
        }
    }

    @Test
    fun `loadHistory success`() {
        runBlocking {
            val json = """{
                "inverter_id": "deye_sg02lp1_am3",
                "parameter": "batt_soc",
                "start": "2026-11-01T00:00:00Z",
                "end": "2026-11-18T23:59:59Z",
                "count": 2,
                "measurements": [
                    {"parameter_name": "batt_soc", "normalized_value": 80.0, "unit": "%", "timestamp": "2026-11-18T08:00:00Z"},
                    {"parameter_name": "batt_soc", "normalized_value": 86.0, "unit": "%", "timestamp": "2026-11-18T10:00:00Z"}
                ]
            }"""
            mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"))

            val result = repository.loadHistory("batt_soc", "2026-11-01T00:00:00Z", "2026-11-18T23:59:59Z")
            assertTrue(result is Result.Success)
            val dto = (result as Result.Success).data
            assertEquals(2, dto.count)
            assertEquals(80.0, dto.measurements[0].normalized_value!!, 0.001)
            assertEquals(86.0, dto.measurements[1].normalized_value!!, 0.001)
        }
    }

    @Test
    fun `loadRegisters success`() {
        runBlocking {
            val json = """{
                "registers": [
                    {"address": 183, "name": "battery_voltage", "data_type": "U_WORD", "scale": 0.01, "unit": "V", "confidence": "corroborated", "provisional_reason": "", "description": "Battery Voltage"}
                ],
                "count": 1,
                "source": "domain.register_metadata (stage 7)"
            }"""
            mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"))

            val result = repository.loadRegisters()
            assertTrue(result is Result.Success)
            val dto = (result as Result.Success).data
            assertEquals(1, dto.count)
            assertEquals(183, dto.registers[0].address)
            assertEquals("corroborated", dto.registers[0].confidence)
        }
    }
}
