package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.LiveDataFullMapper
import com.sunlink.svitgrid.data.dto.LiveDataMessageDto
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto
import com.sunlink.svitgrid.data.dto.MeasurementDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.WebSocketLiveParser
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.viewmodel.HistoryUiState
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

@OptIn(ExperimentalCoroutinesApi::class)
class Stage39IntegrationTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val wsFrame = """
        {"type":"live_data","data":{"battery_soc":{"raw_value":90,"normalized_value":90.0,
        "unit":"%","confidence":"corroborated","quality":"valid","address":184,
        "timestamp":"2026-11-22T10:00:00Z"}},
        "status":{"connection_status":true,"last_update":"2026-11-22T10:00:00Z",
        "inverter_id":"deye_sg02lp1_am3","source":"deye_modbus"},
        "timestamp":"2026-11-22T10:00:00Z"}
    """.trimIndent()

    @Test
    fun `websocket frame updates full telemetry model without fabricating fields`() {
        val message = WebSocketLiveParser.parse(wsFrame)!!
        val current = TelemetryFullDto(
            source = "deye_modbus", timestamp = "2026-11-22T09:59:00Z",
            categories = mapOf("battery" to mapOf("voltage" to null))
        )
        val merged = LiveDataFullMapper.merge(current, message)
        assertEquals("deye_modbus", merged.source)
        assertEquals(90.0, merged.categories!!["battery"]!!["soc"]!!.normalized_value)
        assertNull(merged.categories!!["battery"]!!["voltage"])
        assertNull(merged.categories!!["pv"]?.get("pv1_power"))
    }

    @Test
    fun `viewmodel updates both ui state and full telemetry from websocket`() {
        val vm = MainViewModel(DataRepository(null))
        vm.onLiveDataMessage(WebSocketLiveParser.parse(wsFrame)!!)
        assertEquals(90.0f, (vm.uiState.value as com.sunlink.svitgrid.data.model.InverterUiState.Success).data.batterySoc)
        assertEquals(90.0, vm.fullTelemetry.value!!.categories!!["battery"]!!["soc"]!!.normalized_value)
        assertNull(vm.fullTelemetryError.value)
    }

    @Test
    fun `history sorts by timestamp and removes duplicates`() {
        class Repo : DataRepository(null) {
            override suspend fun loadHistory(
                parameter: String, start: String, end: String
            ): Result<MeasurementsHistoryDto> = Result.Success(
                MeasurementsHistoryDto(parameter = parameter, measurements = listOf(
                    MeasurementDto(timestamp = "2026-11-22T10:00:00Z", normalized_value = 90.0, unit = "%"),
                    MeasurementDto(timestamp = "2026-11-22T09:00:00Z", normalized_value = 88.0, unit = "%"),
                    MeasurementDto(timestamp = "2026-11-22T10:00:00Z", normalized_value = 91.0, unit = "%")
                ))
            )
        }
        val vm = MainViewModel(Repo())
        vm.loadHistory("battery_soc", "2026-11-22T00:00:00Z", "2026-11-23T00:00:00Z")
        val state = vm.historyState.value as HistoryUiState.Success
        assertEquals(listOf("2026-11-22T09:00:00Z", "2026-11-22T10:00:00Z"), state.points.map { it.timestamp })
        assertEquals(listOf(88.0f, 90.0f), state.points.map { it.value })
    }

    @Test
    fun `statistics dto does not infer values from empty measurements`() {
        val stats = Gson().fromJson(
            """{"parameter":"battery_soc","count":0,"min":null,"max":null,"avg":null,"sum":null}""",
            com.sunlink.svitgrid.data.dto.StatsSummaryDto::class.java
        )
        assertEquals(0, stats.count)
        assertNull(stats.sum)
        assertNull(stats.avg)
    }

    @Test
    fun `measurement value null is distinct from numeric zero`() {
        val nullValue = Gson().fromJson("{}", MeasurementValueDto::class.java)
        val zero = Gson().fromJson("""{"normalized_value":0.0,"raw_value":0}""", MeasurementValueDto::class.java)
        assertNull(nullValue.normalized_value)
        assertEquals(0.0, zero.normalized_value!!, 0.0)
        assertNotEquals(nullValue.normalized_value, zero.normalized_value)
    }
}
