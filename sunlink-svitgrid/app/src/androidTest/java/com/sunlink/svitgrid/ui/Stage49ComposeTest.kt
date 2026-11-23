package com.sunlink.svitgrid.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.TelemetryQualityDto
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterStatus
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.model.success
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.ui.redesign.HomeScreen
import com.sunlink.svitgrid.ui.redesign.MoreScreen
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
class Stage49ComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private class FakeRepository : DataRepository(null) {
        private val ts = "2026-11-23T10:00:00Z"

        override suspend fun loadInverterData(): Result<Pair<InverterData, InverterStatus>> = success(
            Pair(
                InverterData(batterySoc = 82f, timestamp = ts, connectionStatus = true),
                InverterStatus(online = true, lastUpdate = ts)
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
                        "voltage" to null
                    ),
                    "pv" to mapOf("total_power" to null)
                )
            )
        )

        override suspend fun loadTelemetryQuality(): Result<TelemetryQualityDto> = success(
            TelemetryQualityDto(source = "deye_modbus", stale = false, known_parameters = 80, available_parameters = 1)
        )
    }

    @Before
    fun setUp() { Dispatchers.setMain(Dispatchers.Default) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun home_shows_real_soc_and_missing_pv_without_fake_zero() {
        compose.setContent { SvitGridTheme { HomeScreen(vm = MainViewModel(FakeRepository())) } }
        compose.onNodeWithText("82.0").assertIsDisplayed()
        compose.onNodeWithText("0.0 W").assertDoesNotExist()
        compose.onNodeWithText("0 W").assertDoesNotExist()
    }

    @Test
    fun more_screen_lists_grid_load_analytics_diagnostics() {
        compose.setContent { SvitGridTheme { MoreScreen(currentRoute = "home", onNavigate = {}) } }
        compose.onNodeWithText("МЕРЕЖА").assertIsDisplayed()
        compose.onNodeWithText("НАВАНТАЖЕННЯ").assertIsDisplayed()
        compose.onNodeWithText("Діагностика").assertIsDisplayed()
    }
}
