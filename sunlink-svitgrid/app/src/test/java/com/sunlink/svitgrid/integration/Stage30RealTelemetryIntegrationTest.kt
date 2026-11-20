package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.LiveRawDto
import com.sunlink.svitgrid.data.dto.WebSocketLiveParser
import com.sunlink.svitgrid.data.dto.WebSocketTelemetryMapper
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.data.repository.DataRepository
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

/**
 * Stage 30 — сквозная проверка на КАДРАХ, реально снятых с backend
 * (изолированный deye_gateway + физический FC03 reg 184, 2026-11-20).
 *
 * Значение SOC в этих фикстурах — 90% (свежий ответ), а не 94% из Stage 28:
 * 94% больше не является ожидаемым без нового подтверждения.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Stage30RealTelemetryIntegrationTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val server = MockWebServer()

    /** Фактический ответ /api/live/raw (сокращён только пробелами). */
    private val realRest = """
        {"p_sun":null,"p_load":null,"p_batt":null,"p_grid":null,"batt_soc":90.0,
        "battery_voltage":null,"battery_current":null,"dc_temperature":null,
        "ac_temperature":null,"pv1_power":null,"pv2_power":null,"pv3_power":null,
        "p_string1":null,"p_string2":null,"p_string3":null,"provisional_parameters":[],
        "timestamp":"2026-11-20T09:47:40.318294+00:00","connection_status":true,
        "inverter_id":"deye_sg02lp1_am3","available_registers":9,
        "source":"deye_modbus","battery_soc":90.0}
    """.trimIndent()

    /** Фактический кадр /ws. */
    private val realWs = """
        {"type":"live_data","data":{"battery_soc":{"raw_value":90,"normalized_value":90.0,
        "unit":"%","confidence":"corroborated","quality":"valid","address":184,
        "timestamp":"2026-11-20T09:47:40.282179+00:00"}},
        "status":{"connection_status":true,"last_update":"2026-11-20T09:47:40.282179+00:00",
        "inverter_id":"deye_sg02lp1_am3","available_registers":9,
        "registers":[90,91,183,184,186,187,188,190,191],"source":"deye_modbus"},
        "timestamp":"2026-11-20T09:47:59.725264+00:00"}
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server.start()
        RetrofitClient.setBaseUrl(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun `real REST response reaches domain model without fabricated values`() {
        server.enqueue(MockResponse().setBody(realRest).setHeader("Content-Type", "application/json"))
        val repository = DataRepository(RetrofitClient.getApiService())
        val result = kotlinx.coroutines.runBlocking { repository.loadInverterData() }

        assertTrue(result is com.sunlink.svitgrid.data.model.Result.Success)
        val (data, status) = (result as com.sunlink.svitgrid.data.model.Result.Success).data
        assertEquals(90.0f, data.batterySoc)
        assertEquals("deye_modbus", data.source)
        assertTrue(status.online)
        // Неприсланые параметры остаются null — в UI будет «Немає даних».
        assertNull(data.pv1Power)
        assertNull(data.pGrid)
        assertNull(data.batteryVoltage)
        assertNull(data.dcTemperature)
    }

    @Test
    fun `real websocket frame reaches viewmodel state`() {
        val vm = MainViewModel(DataRepository(RetrofitClient.getApiService()))
        val message = WebSocketLiveParser.parse(realWs)!!
        vm.onLiveDataMessage(message)

        val state = vm.uiState.value as InverterUiState.Success
        assertEquals(90.0f, state.data.batterySoc)
        assertEquals("deye_modbus", state.data.source)
        assertEquals(184, message.data!!["battery_soc"]!!.address)
        assertTrue(state.status.online)
        assertTrue(WebSocketTelemetryMapper.toInverterData(message).provisionalParameters.isEmpty())
    }

    @Test
    fun `gson parses raw REST field names of real backend`() {
        val dto = Gson().fromJson(realRest, LiveRawDto::class.java)
        assertEquals(90.0, dto.batt_soc)
        assertEquals("deye_modbus", dto.source)
        assertNull(dto.p_grid)
        assertTrue(dto.provisional_parameters.isEmpty())
    }
}
