package com.sunlink.svitgrid.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sunlink.svitgrid.data.dto.HealthDto
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto
import com.sunlink.svitgrid.data.dto.StatsSummaryDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.VersionDto
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterStatus
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.model.success
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.ui.dashboard.DashboardScreen
import com.sunlink.svitgrid.ui.telemetry.BatteryDetailScreen
import com.sunlink.svitgrid.ui.telemetry.EnergyFlowScreen
import com.sunlink.svitgrid.ui.telemetry.GridDetailScreen
import com.sunlink.svitgrid.ui.telemetry.LoadDetailScreen
import com.sunlink.svitgrid.ui.telemetry.PvDetailScreen
import com.sunlink.svitgrid.ui.theme.SvitGridTheme
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Stage 38 — instrumented Compose UI tests. Все данные изолированные TEST fixtures;
 *production-сеть и инвертор не используются.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Stage38ComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private class FakeRepository : DataRepository(null) {
        private val ts = "2026-11-21T10:00:00Z"

        override suspend fun loadInverterData(): Result<Pair<InverterData, InverterStatus>> = success(
            Pair(
                InverterData(
                    batterySoc = 82f, timestamp = ts, connectionStatus = true,
                    source = "deye_modbus", quality = "valid"
                ),
                InverterStatus(online = true, lastUpdate = ts, availableRegisters = 9)
            )
        )

        override suspend fun loadStatus(): Result<InverterStatus> = success(
            InverterStatus(online = true, lastUpdate = ts, availableRegisters = 9)
        )

        override suspend fun loadFullTelemetry(): Result<TelemetryFullDto> = success(
            TelemetryFullDto(
                source = "deye_modbus", timestamp = ts, connection_status = true,
                stale = false, inverter_id = "deye_sg02lp1_am3",
                categories = mapOf(
                    "battery" to mapOf(
                        "soc" to MeasurementValueDto(82, 82.0, "%", "corroborated", "valid", 184, ts),
                        "voltage" to null,
                        "current" to null,
                        "power" to null,
                        "temperature" to null
                    ),
                    "pv" to mapOf(
                        "total_power" to MeasurementValueDto(
                            confidence = "calculated", quality = "valid", timestamp = ts
                        ),
                        "pv1_voltage" to null
                    ),
                    "grid" to mapOf("power" to null),
                    "load" to mapOf("power" to null)
                )
            )
        )

        override suspend fun loadStats(
            parameter: String, start: String, end: String
        ): Result<StatsSummaryDto> = success(StatsSummaryDto(parameter = parameter, count = 0))

        override suspend fun loadHistory(
            parameter: String, start: String, end: String
        ): Result<MeasurementsHistoryDto> = success(MeasurementsHistoryDto(measurements = emptyList()))

        override suspend fun loadHealth(): Result<HealthDto> = success(
            HealthDto(status = "ok", read_only = true, source = "deye_modbus")
        )

        override suspend fun loadBackendVersion(): Result<VersionDto> = success(
            VersionDto(api = "2.1.0", gateway = "2.0.0", protocol = "read-only FC03")
        )
    }

    @Before
    fun setUp() { Dispatchers.setMain(Dispatchers.Default) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = MainViewModel(FakeRepository())

    @Test
    fun dashboard_shows_real_soc_and_no_fake_zero() {
        compose.setContent { SvitGridTheme { DashboardScreen(vm = vm()) } }
        compose.onNodeWithText("82 %").assertExists()
        compose.onNodeWithText("0 %").assertDoesNotExist()
        compose.onNodeWithText("ШВИДКИЙ ДОСТУП").assertExists()
    }

    @Test
    fun battery_screen_shows_soc_and_missing_values() {
        compose.setContent { SvitGridTheme { BatteryDetailScreen(vm = vm()) } }
        compose.onNodeWithText("82 %").assertExists()
        compose.onNodeWithText("Немає даних").assertExists()
    }

    @Test
    fun pv_grid_load_and_flow_screens_render_without_crash() {
        compose.setContent { SvitGridTheme { PvDetailScreen(vm = vm()) } }
        compose.onNodeWithText("СОНЯЧНІ ПАНЕЛІ").assertExists()
        compose.setContent { SvitGridTheme { GridDetailScreen(vm = vm()) } }
        compose.onNodeWithText("МЕРЕЖА").assertExists()
        compose.setContent { SvitGridTheme { LoadDetailScreen(vm = vm()) } }
        compose.onNodeWithText("НАВАНТАЖЕННЯ").assertExists()
        compose.setContent { SvitGridTheme { EnergyFlowScreen(vm = vm()) } }
        compose.onNodeWithText("ПОТОКИ ЕНЕРГІЇ").assertExists()
    }

    @Test
    fun navigation_drawer_opens_and_switches_to_battery() {
        compose.setContent { SvitGridTheme { SvitGridApp(repository = FakeRepository()) } }
        compose.onNodeWithContentDescription("Меню").performClick()
        compose.onNodeWithText("АКУМУЛЯТОР").performClick()
        compose.onNodeWithText("АКУМУЛЯТОР").assertExists()
    }

    @Test
    fun bottom_navigation_switches_to_statistics() {
        compose.setContent { SvitGridTheme { SvitGridApp(repository = FakeRepository()) } }
        compose.onNodeWithText("Статистика").performClick()
        compose.onNodeWithText("СТАТИСТИКА").assertExists()
    }
}
