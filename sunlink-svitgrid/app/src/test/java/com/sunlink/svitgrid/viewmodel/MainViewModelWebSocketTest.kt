package com.sunlink.svitgrid.viewmodel

import com.sunlink.svitgrid.data.dto.LiveDataMessageDto
import com.sunlink.svitgrid.data.dto.WebSocketLiveParser
import com.sunlink.svitgrid.data.dto.WsMeasurementDto
import com.sunlink.svitgrid.data.dto.WsStatusDto
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.model.success
import com.sunlink.svitgrid.data.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Stage 29/30 — обработка WebSocket-кадров и REST-резерва в ViewModel.
 * Тесты не обращаются к оборудованию: кадры повторяют формат реального backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelWebSocketTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FakeRepository : DataRepository(null) {
        var restResult: Result<Pair<com.sunlink.svitgrid.data.model.InverterData,
                com.sunlink.svitgrid.data.model.InverterStatus>>? = null
        override suspend fun loadInverterData() = restResult
            ?: throw IllegalStateException("REST not configured")
    }

    private fun frame(
        soc: Double? = 94.0,
        online: Boolean = true,
        source: String = "deye_modbus",
        timestamp: String? = Instant.now().toString(),
        withData: Boolean = true,
        quality: String = "valid"
    ): LiveDataMessageDto {
        val data = if (withData) mapOf(
            "battery_soc" to WsMeasurementDto(
                raw_value = 94,
                normalized_value = soc,
                unit = "%",
                confidence = "corroborated",
                quality = quality,
                address = 184,
                timestamp = timestamp
            )
        ) else emptyMap()
        return LiveDataMessageDto(
            type = "live_data",
            data = data,
            status = WsStatusDto(
                connection_status = online,
                last_update = timestamp,
                inverter_id = "deye_sg02lp1_am3",
                available_registers = 9,
                source = source
            ),
            timestamp = timestamp
        )
    }

    @Test
    fun `real frame sets online with physical soc and null for missing parameters`() {
        val vm = MainViewModel(FakeRepository())
        vm.onLiveDataMessage(frame())

        val state = vm.uiState.value as InverterUiState.Success
        assertTrue(state.status.online)
        assertEquals(94.0f, state.data.batterySoc)
        assertEquals("deye_modbus", state.data.source)
        assertFalse(state.stale)
        assertNull(state.data.pGrid)
        assertNull(state.data.pv1Power)
    }

    @Test
    fun `connection status false never sets online`() {
        val vm = MainViewModel(FakeRepository())
        vm.onLiveDataMessage(frame(online = false))

        assertTrue(vm.uiState.value is InverterUiState.Error)
        val error = vm.uiState.value as InverterUiState.Error
        assertNull(error.data?.batterySoc)
        assertFalse(error.status?.online ?: false)
    }

    @Test
    fun `frame without measurements is not treated as success`() {
        val vm = MainViewModel(FakeRepository())
        vm.onLiveDataMessage(frame(withData = false))
        assertTrue(vm.uiState.value is InverterUiState.Error)
    }

    @Test
    fun `frame without timestamp is stale-unknown and rejected`() {
        val vm = MainViewModel(FakeRepository())
        vm.onLiveDataMessage(frame(timestamp = null))
        assertTrue(vm.uiState.value is InverterUiState.Error)
    }

    @Test
    fun `old timestamp is marked stale, not presented as fresh`() {
        val vm = MainViewModel(FakeRepository())
        val old = Instant.now().minusSeconds(3600).toString()
        vm.onLiveDataMessage(frame(timestamp = old))

        val state = vm.uiState.value as InverterUiState.Success
        assertTrue(state.stale)
        assertEquals(94.0f, state.data.batterySoc)
    }

    @Test
    fun `demo source is preserved for UI warning`() {
        val vm = MainViewModel(FakeRepository())
        vm.onLiveDataMessage(frame(source = "demo_simulator"))
        val state = vm.uiState.value as InverterUiState.Success
        assertEquals("demo_simulator", state.data.source)
    }

    @Test
    fun `invalid frame type is ignored`() {
        val vm = MainViewModel(FakeRepository())
        vm.onLiveDataMessage(LiveDataMessageDto(type = "ping", status = WsStatusDto(connection_status = true)))
        assertTrue(vm.uiState.value is InverterUiState.Loading)
    }
}
