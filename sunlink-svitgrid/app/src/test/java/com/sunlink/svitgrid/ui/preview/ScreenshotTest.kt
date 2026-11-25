package com.sunlink.svitgrid.ui.preview

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.sunlink.svitgrid.ui.SettingsScreen
import com.sunlink.svitgrid.ui.redesign.AllParametersScreen
import com.sunlink.svitgrid.ui.redesign.AnalyticsScreen
import com.sunlink.svitgrid.ui.redesign.BatteryScreen
import com.sunlink.svitgrid.ui.redesign.DiagnosticsScreen
import com.sunlink.svitgrid.ui.redesign.EnergyScreen
import com.sunlink.svitgrid.ui.redesign.GridScreen
import com.sunlink.svitgrid.ui.redesign.HomeScreen
import com.sunlink.svitgrid.ui.redesign.InverterDetailsScreen
import com.sunlink.svitgrid.ui.redesign.LoadScreen
import com.sunlink.svitgrid.ui.redesign.MoreScreen
import com.sunlink.svitgrid.ui.redesign.SolarScreen
import com.sunlink.svitgrid.ui.theme.SvitGridTheme
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar"
    )

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun previewVm(): MainViewModel {
        val vm = MainViewModel(PreviewFixtures.Repository())
        runBlocking {
            vm.loadFullTelemetry()
            vm.loadData()
            vm.loadTelemetryQuality()
            listOf("battery_soc", "pv_total_power", "load_power").forEach { vm.loadSeriesFor(it) }
        }
        return vm
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        paparazzi.snapshot(name = name) { SvitGridTheme { content() } }
    }

    @Test fun home() = shot("01_home") { HomeScreen(vm = previewVm(), onNavigate = {}) }
    @Test fun energy() = shot("02_energy") { EnergyScreen(vm = previewVm(), onNavigate = {}) }
    @Test fun battery() = shot("03_battery") { BatteryScreen(vm = previewVm()) }
    @Test fun solar() = shot("04_solar") { SolarScreen(vm = previewVm()) }
    @Test fun grid() = shot("05_grid") { GridScreen(vm = previewVm(), onNavigate = {}) }
    @Test fun load() = shot("06_load") { LoadScreen(vm = previewVm()) }
    @Test fun analytics() = shot("07_analytics") { AnalyticsScreen(vm = previewVm()) }
    @Test fun diagnostics() = shot("08_diagnostics") { DiagnosticsScreen(vm = previewVm()) }
    @Test fun more() = shot("09_more") { MoreScreen(currentRoute = "more", onNavigate = {}) }
    @Test fun settings() = shot("10_settings") { SettingsScreen(vm = previewVm(), previewRepo = previewSettingsRepository()) }
    @Test fun inverter() = shot("11_inverter") { InverterDetailsScreen(vm = previewVm(), onNavigate = {}) }
    @Test fun allParameters() = shot("12_all_parameters") { AllParametersScreen(vm = previewVm()) }
}
