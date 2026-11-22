package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.LiveRawDto
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.repository.DataRepository
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class Stage44GlobalAuditTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var server: MockWebServer

    @Before fun setUp() { Dispatchers.setMain(dispatcher); server = MockWebServer(); server.start() }
    @After fun tearDown() { server.shutdown(); Dispatchers.resetMain() }

    private val realJson = """
        {"source":"deye_modbus","timestamp":"2026-11-22T10:00:00Z","connection_status":true,
         "batt_soc":97.0,"battery_voltage":53.27,"battery_current":1.65,
         "pv1_voltage":307.1,"pv1_current":18.6,"pv1_power":5619.0,
         "grid_frequency":49.96,"grid_voltage_l1":233.6,"load_power":6885.0,
         "daily_pv_energy":9.0,"daily_grid_import":10.1,"daily_grid_export":0.0,
         "daily_battery_charge":4.0,"daily_battery_discharge":3.0,
         "total_battery_charge_low":560.8,"total_battery_discharge_low":525.2,
         "total_pv_energy_low":696.3}
    """.trimIndent()

    @Test
    fun `all real flat fields parse into domain model`() {
        val dto = Gson().fromJson(realJson, LiveRawDto::class.java)
        val vm = MainViewModel(object : DataRepository(null) {
            override suspend fun loadInverterData(): Result<Pair<com.sunlink.svitgrid.data.model.InverterData, com.sunlink.svitgrid.data.model.InverterStatus>> =
                Result.Success(Pair(
                    com.sunlink.svitgrid.data.model.InverterData(
                        gridFrequency = dto.grid_frequency?.toFloat(),
                        dailyPvEnergy = dto.daily_pv_energy?.toFloat(),
                        dailyGridImport = dto.daily_grid_import?.toFloat(),
                        dailyGridExport = dto.daily_grid_export?.toFloat(),
                        dailyBatteryCharge = dto.daily_battery_charge?.toFloat(),
                        dailyBatteryDischarge = dto.daily_battery_discharge?.toFloat(),
                        totalBatteryCharge = dto.total_battery_charge_low?.toFloat(),
                        totalBatteryDischarge = dto.total_battery_discharge_low?.toFloat(),
                        totalPvEnergy = dto.total_pv_energy_low?.toFloat()
                    ),
                    com.sunlink.svitgrid.data.model.InverterStatus(online = true)
                ))
        })
        vm.loadData()
        val data = (vm.uiState.value as com.sunlink.svitgrid.data.model.InverterUiState.Success).data
        assertEquals(49.96f, data.gridFrequency)
        assertEquals(9.0f, data.dailyPvEnergy)
        assertEquals(10.1f, data.dailyGridImport)
        assertEquals(560.8f, data.totalBatteryCharge)
        assertEquals(696.3f, data.totalPvEnergy)
    }

    @Test
    fun `repository maps real backend json through retrofit`() {
        server.enqueue(MockResponse().setBody(realJson).setHeader("Content-Type", "application/json"))
        com.sunlink.svitgrid.network.RetrofitClient.setBaseUrl(server.url("/").toString())
        val repo = DataRepository(com.sunlink.svitgrid.network.RetrofitClient.getApiService())
        val result = kotlinx.coroutines.runBlocking { repo.loadInverterData() }
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data.first
        assertEquals(97.0f, data.batterySoc)
        assertEquals(53.27f, data.batteryVoltage)
        assertEquals(49.96f, data.gridFrequency)
        assertEquals(9.0f, data.dailyPvEnergy)
        assertEquals(4.0f, data.dailyBatteryCharge)
        assertEquals(560.8f, data.totalBatteryCharge)
    }

    @Test
    fun `localization parity and no russian leftovers`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun keys(s: String) = Regex("""<string name="([^"]+)"""").findAll(s).map { it.groupValues[1] }.toSet()
        assertEquals(keys(uk), keys(en))
        assertTrue(keys(uk).contains("energy_summary"))
        val sources = File("src/main/java/com/sunlink/svitgrid")
        val found = sources.walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            listOf("ОТСУТСТВУЄ", "Батерея").filter { f.readText().contains(it) }.map { "${f.name}:$it" }
        }.toList()
        assertTrue(found.toString(), found.isEmpty())
    }
}
