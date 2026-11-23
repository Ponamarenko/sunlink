package com.sunlink.svitgrid.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.TelemetryQualityDto
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterStatus
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.model.success
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.ui.redesign.AnalyticsScreen
import com.sunlink.svitgrid.ui.redesign.BatteryScreen
import com.sunlink.svitgrid.ui.redesign.DiagnosticsScreen
import com.sunlink.svitgrid.ui.redesign.EnergyScreen
import com.sunlink.svitgrid.ui.redesign.GridScreen
import com.sunlink.svitgrid.ui.redesign.HomeScreen
import com.sunlink.svitgrid.ui.redesign.LoadScreen
import com.sunlink.svitgrid.ui.redesign.MoreScreen
import com.sunlink.svitgrid.ui.redesign.SolarScreen
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

@OptIn(ExperimentalCoroutinesApi::class)
class Stage50ComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private class FakeRepository : DataRepository(null) {
        private val ts = "2026-11-23T10:00:00Z"

        override suspend fun loadInverterData(): Result<Pair<InverterData, InverterStatus>> = success(
            Pair(
                InverterData(batterySoc = 82f, timestamp = ts, connectionStatus = true),
                InverterStatus(online = true, lastUpdate = ts, availableRegisters = 12)
            )
        )

        override suspend fun loadStatus(): Result<InverterStatus> =
            success(InverterStatus(online = true, lastUpdate = ts))

        override suspend fun loadFullTelemetry(): Result<TelemetryFullDto> = success(
            TelemetryFullDto(
                source = "deye_modbus", timestamp = ts, connection_status = true, stale = false,
                categories = mapOf(
                    "battery" to mapOf(
                        "soc" to MeasurementValueDto(82, 82.0, "%", "corroborated", "valid", 184, ts),
                        "voltage" to MeasurementValueDto(normalized_value = 51.2, confidence = "verified", quality = "valid"),
                        "current" to null,
                        "power" to MeasurementValueDto(normalized_value = 640.0, confidence = "verified", quality = "valid")
                    ),
                    "pv" to mapOf(
                        "total_power" to MeasurementValueDto(normalized_value = 2400.0, confidence = "calculated", quality = "valid"),
                        "pv1_power" to MeasurementValueDto(normalized_value = 900.0, confidence = "corroborated", quality = "valid")
                    ),
                    "grid" to mapOf(
                        "power" to MeasurementValueDto(normalized_value = 800.0, confidence = "verified", quality = "valid"),
                        "voltage_l1" to MeasurementValueDto(normalized_value = 231.0, confidence = "verified", quality = "valid")
                    ),
                    "load" to mapOf(
                        "power" to MeasurementValueDto(normalized_value = 1600.0, confidence = "verified", quality = "valid")
                    )
                )
            )
        )

        override suspend fun loadTelemetryQuality(): Result<TelemetryQualityDto> = success(
            TelemetryQualityDto(
                source = "deye_modbus", stale = false, known_parameters = 80,
                available_parameters = 23, active_registers = listOf(184),
                polling_running = true, cycles = 42
            )
        )

        override suspend fun loadHistorySeries(parameter: String, period: String): Result<HistorySeriesDto> = success(
            HistorySeriesDto(
                parameter = parameter,
                period = period,
                unit = "W",
                count = 4,
                available_points = 2,
                missing_points = 2,
                points = listOf(
                    com.sunlink.svitgrid.data.dto.HistorySeriesPointDto("2026-11-23T09:00:00Z", 100.0),
                    com.sunlink.svitgrid.data.dto.HistorySeriesPointDto("2026-11-23T09:30:00Z", null, gap = true),
                    com.sunlink.svitgrid.data.dto.HistorySeriesPointDto("2026-11-23T10:00:00Z", 200.0)
                )
            )
        )
    }

    @Before
    fun setUp() { Dispatchers.setMain(Dispatchers.Default) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = MainViewModel(FakeRepository())

    private fun render(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { SvitGridTheme { content() } }
    }

    @Test
    fun home_shows_premium_flow_and_no_fake_zero() {
        render { HomeScreen(vm = vm()) }
        compose.onNodeWithText("Fresh").assertIsDisplayed()
        compose.onNodeWithText("PV").assertIsDisplayed()
        compose.onNodeWithText("HOME").assertIsDisplayed()
        compose.onNodeWithText("0 W").assertDoesNotExist()
    }

    @Test
    fun daily_energy_card_is_reachable_by_scrolling() {
        render { HomeScreen(vm = vm()) }
        compose.onNodeWithText("Енергія сьогодні").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun battery_and_solar_screens_render_real_values() {
        render { BatteryScreen(vm = vm()) }
        compose.onNodeWithText("82%").assertIsDisplayed()
        render { SolarScreen(vm = vm()) }
        compose.onNodeWithText("PV1").assertIsDisplayed()
        compose.onNodeWithText("PV2").assertIsDisplayed()
        compose.onNodeWithText("PV3").assertIsDisplayed()
    }

    @Test
    fun grid_load_energy_screens_render() {
        render { GridScreen(vm = vm()) }
        compose.onNodeWithText("Імпорт", substring = true).assertIsDisplayed()
        render { LoadScreen(vm = vm()) }
        compose.onNodeWithText("Навантаження").assertIsDisplayed()
        render { EnergyScreen(vm = vm()) }
        compose.onNodeWithText("Fresh").assertIsDisplayed()
    }

    @Test
    fun analytics_uses_light_theme_periods() {
        render { AnalyticsScreen(vm = vm()) }
        compose.onNodeWithText("Daily").assertIsDisplayed()
        compose.onNodeWithText("Weekly").assertIsDisplayed()
        compose.onNodeWithText("Monthly").assertIsDisplayed()
        compose.onNodeWithText("Yearly").assertIsDisplayed()
    }

    @Test
    fun diagnostics_and_more_render_all_destinations() {
        render { DiagnosticsScreen(vm = vm()) }
        compose.onNodeWithText("Діагностика").assertIsDisplayed()
        render { MoreScreen(currentRoute = "home", onNavigate = {}) }
        compose.onNodeWithText("МЕРЕЖА").assertIsDisplayed()
        compose.onNodeWithText("Діагностика").assertIsDisplayed()
    }
}
