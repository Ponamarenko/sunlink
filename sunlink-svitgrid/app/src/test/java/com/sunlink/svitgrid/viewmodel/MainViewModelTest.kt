package com.sunlink.svitgrid.viewmodel

import com.sunlink.svitgrid.data.model.*
import com.sunlink.svitgrid.data.repository.DataRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

import java.util.concurrent.atomic.AtomicReference

/**
 * Тести MainViewModel з фейковим репозиторієм.
 */
class MainViewModelTest {

    private class FakeRepository : DataRepository(null) {
        // Перевизначаємо поведінку через mutable стан
        var loadDataResult: androidx.lifecycle.Result<Pair<InverterData, InverterStatus>> =
            androidx.lifecycle.Result.success(
                Pair(
                    InverterData(
                        pSun = 1000f,
                        battSoc = 85f,
                        pGrid = 200f,
                        pLoad = 800f,
                        connectionStatus = true,
                        source = "modbus_register",
                        quality = "valid",
                        confidence = "corroborated"
                    ),
                    InverterStatus(online = true, availableRegisters = 9)
                )
            )
        var loadStatusResult: androidx.lifecycle.Result<InverterStatus> =
            androidx.lifecycle.Result.success(InverterStatus(online = true, availableRegisters = 9))
        var loadHistoryResult: androidx.lifecycle.Result<com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto> =
            androidx.lifecycle.Result.success(
                com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto(
                    measurements = listOf(
                        com.sunlink.svitgrid.data.dto.MeasurementDto(
                            parameter_name = "batt_soc",
                            normalized_value = 85.0,
                            unit = "%",
                            timestamp = "2026-11-18T10:00:00Z"
                        )
                    )
                )
            )

        override suspend fun loadInverterData(): androidx.lifecycle.Result<Pair<InverterData, InverterStatus>> = loadDataResult
        override suspend fun loadStatus(): androidx.lifecycle.Result<InverterStatus> = loadStatusResult
        override suspend fun loadHistory(parameter: String, start: String, end: String):
            androidx.lifecycle.Result<com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto> = loadHistoryResult
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(FakeRepository())
    }

    @Test
    fun `loadData sets Success state`() {
        runBlocking {
            val vm = createViewModel()
            vm.loadData()

            // Даємо корутині час на виконання
            Thread.sleep(200)

            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Success)
            val success = state as InverterUiState.Success
            assertEquals(1000f, success.data.pSun, 0.001f)
            assertEquals(85f, success.data.batterySoc, 0.001f)
            assertTrue(success.status.online)
        }
    }

    @Test
    fun `loadData with no measurements sets Error`() {
        runBlocking {
            val repo = FakeRepository()
            repo.loadDataResult = androidx.lifecycle.Result.success(
                Pair(
                    InverterData(connectionStatus = false),
                    InverterStatus(online = false)
                )
            )
            val vm = MainViewModel(repo)
            vm.loadData()

            Thread.sleep(200)

            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Error)
        }
    }

    @Test
    fun `refresh updates state`() {
        runBlocking {
            val vm = createViewModel()
            vm.refresh()

            Thread.sleep(200)

            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Success)
        }
    }

    @Test
    fun `checkConnection sets Success when online`() {
        runBlocking {
            val vm = createViewModel()
            vm.checkConnection()

            Thread.sleep(200)

            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Success)
            assertTrue((state as InverterUiState.Success).status.online)
        }
    }

    @Test
    fun `loadHistory sets Success state`() {
        runBlocking {
            val vm = createViewModel()
            vm.loadHistory("batt_soc", "2026-11-01T00:00:00Z", "2026-11-18T23:59:59Z")

            Thread.sleep(200)

            val state = vm.historyState.value
            assertTrue(state is HistoryUiState.Success)
            val success = state as HistoryUiState.Success
            assertEquals(1, success.points.size)
            assertEquals(85.0f, success.points[0].value, 0.001f)
        }
    }

    @Test
    fun `pollIntervalSeconds default is 5`() {
        val vm = createViewModel()
        assertEquals(5, vm.pollIntervalSeconds)
    }

    @Test
    fun `setPollInterval clamps to valid range`() {
        val vm = createViewModel()
        vm.setPollInterval(100)
        assertEquals(60, vm.pollIntervalSeconds)

        vm.setPollInterval(0)
        assertEquals(1, vm.pollIntervalSeconds)
    }
}
