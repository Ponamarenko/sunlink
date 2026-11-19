package com.sunlink.svitgrid.data.dto

import com.google.gson.Gson
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.network.ApiService
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Stage 24 — контракт новых полей телеметрии и состояния ViewModel.
 * Только локальные фикстуры, полученные от DEMO backend.
 */
class Stage24TelemetryContractTest {

    private val gson = Gson()

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("demo/$name")
            ?.bufferedReader()?.use { reader -> reader.readText() }
            ?: error("Fixture demo/$name not found")

    @Test
    fun `live raw contains all new telemetry fields and provisional markers`() {
        val dto = gson.fromJson(fixture("live_raw_discharge.json"), LiveRawDto::class.java)
        assertEquals(1519.0, dto.pv1_power!!, 0.001)
        assertEquals(796.0, dto.pv2_power!!, 0.001)
        assertEquals(123.0, dto.pv3_power!!, 0.001)
        assertEquals(53.53, dto.battery_voltage!!, 0.001)
        assertEquals(-3.51, dto.battery_current!!, 0.001)
        assertEquals(41.0, dto.dc_temperature!!, 0.001)
        assertTrue(dto.provisional_parameters.contains("battery_power"))
        assertTrue(dto.provisional_parameters.contains("dc_temperature"))
        assertTrue(dto.provisional_parameters.contains("ac_temperature"))
    }

    @Test
    fun `discharge fixture keeps negative current and power`() {
        val dto = gson.fromJson(fixture("live_raw_discharge.json"), LiveRawDto::class.java)
        assertTrue(dto.battery_current!! < 0)
        assertTrue(dto.p_batt!! < 0)
        assertEquals(-3.51, dto.battery_current!!, 0.001)
    }

    @Test
    fun `partial fixture uses null instead of zero`() {
        val dto = gson.fromJson(fixture("live_raw_partial.json"), LiveRawDto::class.java)
        assertNull(dto.pv1_power)
        assertNull(dto.battery_current)
        assertNull(dto.p_sun)
        assertEquals(70.0, dto.batt_soc!!, 0.001)
    }

}

@OptIn(ExperimentalCoroutinesApi::class)
class Stage24ViewModelFixtureTest {
        private val dispatcher = UnconfinedTestDispatcher()
        private lateinit var server: MockWebServer

        @Before
        fun setUp() {
            Dispatchers.setMain(dispatcher)
            server = MockWebServer().also { it.start() }
        }

        @After
        fun tearDown() {
            server.shutdown()
            Dispatchers.resetMain()
        }

        private fun fixture(name: String): String =
            javaClass.classLoader.getResourceAsStream("demo/$name")
                ?.bufferedReader()?.use { reader -> reader.readText() }
                ?: error("Fixture demo/$name not found")

        private suspend fun waitFor(predicate: () -> Boolean) {
            val start = System.currentTimeMillis()
            while (!predicate() && System.currentTimeMillis() - start < 4000) delay(20)
        }

        @Test
        fun `repository maps provisional parameters into domain model`() = runBlocking {
            val api = Retrofit.Builder()
                .baseUrl(server.url("/").toString())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService::class.java)
            val repo = DataRepository(api)
            server.enqueue(MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(fixture("live_raw_discharge.json")))

            val result = repo.loadInverterData()
            assertTrue(result is com.sunlink.svitgrid.data.model.Result.Success)
            val (data, _) = (result as com.sunlink.svitgrid.data.model.Result.Success).data
            assertTrue(data.provisionalParameters.contains("battery_power"))
            assertEquals(1519.0f, data.pv1Power!!, 0.001f)
            assertTrue(data.pBatt!! < 0)
        }

        @Test
        fun `view model exposes negative telemetry from discharge fixture`() = runBlocking {
            val api = Retrofit.Builder()
                .baseUrl(server.url("/").toString())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService::class.java)
            val vm = MainViewModel(DataRepository(api))
            server.enqueue(MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(fixture("live_raw_discharge.json")))

            vm.refresh()
            waitFor { !vm.refreshing.value }
            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Success)
            state as InverterUiState.Success
            assertTrue(state.data.pBatt!! < 0)
            assertTrue(state.data.batteryCurrent!! < 0)
            assertTrue(state.data.provisionalParameters.contains("battery_power"))
        }
}
