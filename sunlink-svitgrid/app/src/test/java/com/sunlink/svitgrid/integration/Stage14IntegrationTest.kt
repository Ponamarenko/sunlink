package com.sunlink.svitgrid.integration

import com.sunlink.svitgrid.data.BackendDefaults
import com.sunlink.svitgrid.data.SettingsRepository
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.network.ApiService
import com.sunlink.svitgrid.network.DelegatingApiService
import com.sunlink.svitgrid.network.RetrofitClient
import com.sunlink.svitgrid.viewmodel.HistoryUiState
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
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Stage 14 — сценарии 1-8 на реальных ответах изолированного DEMO backend.
 *
 * Нет сети, нет production URL, нет физического устройства.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Stage14IntegrationTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var serverA: MockWebServer
    private lateinit var serverB: MockWebServer

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        serverA = MockWebServer().also { it.start() }
        serverB = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() {
        serverA.shutdown()
        serverB.shutdown()
        RetrofitClient.setBaseUrl(BackendDefaults.BASE_URL)
        Dispatchers.resetMain()
    }

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("demo/$name")
            ?.bufferedReader()?.use { reader -> reader.readText() }
            ?: error("Fixture demo/$name not found")

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)

    private fun apiFor(server: MockWebServer): ApiService =
        Retrofit.Builder()
            .baseUrl(server.url("/").toString())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

    private suspend fun waitFor(timeoutMs: Long = 4000, predicate: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!predicate() && System.currentTimeMillis() - start < timeoutMs) {
            delay(20)
        }
    }

    // --- Сценарий 1: первый запуск ---

    @Test
    fun `first launch has valid default settings`() {
        assertNull(SettingsRepository.validate(BackendDefaults.HOST, BackendDefaults.PORT.toString()))
        assertEquals("http://ha4a.pp.ua:8800/", BackendDefaults.BASE_URL)
    }

    // --- Сценарий 2: смена адреса фактически меняет endpoint ---

    @Test
    fun `changing backend address changes the endpoint used by repository`() {
        runBlocking {
            var current = apiFor(serverA)
            val bus = DelegatingApiService { current }
            val vm = MainViewModel(DataRepository(bus))

            serverA.enqueue(json(fixture("live_raw.json")))
            vm.loadData()
            waitFor { vm.uiState.value !is InverterUiState.Loading }
            val first = vm.uiState.value as InverterUiState.Success
            assertEquals(85.0f, first.data.batterySoc!!, 0.001f)
            assertEquals("/api/live/raw", serverA.takeRequest().path)

            // Пользователь сменил адрес: шина теперь указывает на другой сервер
            current = apiFor(serverB)
            val changedLive = fixture("live_raw.json").replace("\"batt_soc\":85.0", "\"batt_soc\":42.0")
            serverB.enqueue(json(changedLive))
            vm.loadData()
            waitFor { vm.uiState.value !is InverterUiState.Loading }

            val second = vm.uiState.value as InverterUiState.Success
            assertEquals(42.0f, second.data.batterySoc!!, 0.001f)
            assertEquals("/api/live/raw", serverB.takeRequest().path)
        }
    }

    // --- Сценарий 3: успешное подключение ---

    @Test
    fun `successful connection loads status inverters and telemetry`() {
        runBlocking {
            val vm = MainViewModel(DataRepository(apiFor(serverA)))
            serverA.enqueue(json(fixture("live_raw.json")))
            serverA.enqueue(json(fixture("inverters.json")))
            serverA.enqueue(json(fixture("status.json")))

            vm.loadData()
            waitFor { vm.uiState.value !is InverterUiState.Loading }
            vm.loadInverters()
            waitFor { vm.inverters.value != null }
            vm.checkConnection()
            waitFor { !vm.refreshing.value }

            val state = vm.uiState.value as InverterUiState.Success
            assertEquals("demo_simulator", state.data.source)
            assertEquals(211.0f, state.data.pv1Power!!, 0.001f)
            assertEquals("deye_sg02lp1_am3", dtoInverterId(vm))
            assertEquals(1, vm.inverters.value!!.count)
        }
    }

    private fun dtoInverterId(vm: MainViewModel): String =
        (vm.uiState.value as InverterUiState.Success).status.inverterId

    // --- Сценарий 4: ошибка сервера ---

    @Test
    fun `http 500 keeps last good data as stale`() {
        runBlocking {
            val vm = MainViewModel(DataRepository(apiFor(serverA)))
            serverA.enqueue(json(fixture("live_raw.json")))
            vm.loadData()
            waitFor { vm.uiState.value !is InverterUiState.Loading }

            serverA.enqueue(json("{\"error\":\"boom\"}", 500))
            vm.refresh()
            waitFor { !vm.refreshing.value }

            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Error)
            state as InverterUiState.Error
            assertEquals(85.0f, state.data!!.batterySoc!!, 0.001f) // не нули
            assertTrue(state.stale)
        }
    }

    // --- Сценарий 5: отсутствие сети ---

    @Test
    fun `network failure shows error without fake telemetry`() {
        runBlocking {
            val vm = MainViewModel(DataRepository(apiFor(serverA)))
            serverA.enqueue(
                MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
            )
            vm.refresh()
            waitFor { !vm.refreshing.value }

            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Error)
            state as InverterUiState.Error
            assertNull(state.data) // нет последних данных — не показываем выдуманные
            assertTrue(state.message.isNotBlank())
        }
    }

    // --- Сценарий 6: восстановление связи ---

    @Test
    fun `connection recovery clears error state`() {
        runBlocking {
            val vm = MainViewModel(DataRepository(apiFor(serverA)))
            serverA.enqueue(json("{\"error\":\"boom\"}", 500))
            vm.refresh()
            waitFor { !vm.refreshing.value }
            assertTrue(vm.uiState.value is InverterUiState.Error)

            serverA.enqueue(json(fixture("live_raw.json")))
            vm.refresh()
            waitFor { !vm.refreshing.value }
            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Success)
            state as InverterUiState.Success
            assertEquals("demo_simulator", state.data.source)
        }
    }

    // --- Сценарий 7: история ---

    @Test
    fun `history uses correct parameter and handles empty result`() {
        runBlocking {
            val vm = MainViewModel(DataRepository(apiFor(serverA)))
            serverA.enqueue(json(fixture("history_battery_soc.json")))
            vm.loadHistory("battery_soc", "2026-11-18T00:00:00Z", "2026-11-19T00:00:00Z")
            waitFor { vm.historyState.value !is HistoryUiState.Loading }

            val path = serverA.takeRequest().path.orEmpty()
            assertTrue(path.startsWith("/api/measurements/history"))
            assertTrue(path.contains("parameter=battery_soc"))
            val success = vm.historyState.value as HistoryUiState.Success
            val timestamps = success.points.map { it.timestamp }
            assertEquals(timestamps.sorted(), timestamps) // backend сортирует по времени

            serverA.enqueue(json(fixture("history_empty.json")))
            vm.loadHistory("dc_temperature", "2026-11-18T00:00:00Z", "2026-11-19T00:00:00Z")
            waitFor { vm.historyState.value !is HistoryUiState.Loading }
            assertTrue(vm.historyState.value is HistoryUiState.Empty)
        }
    }

    // --- Сценарий 8: некорректный URL ---

    @Test
    fun `invalid backend url is rejected without crashing`() {
        assertNotNull(SettingsRepository.validate("bad host", "8800"))
        assertNotNull(SettingsRepository.validate("127.0.0.1", "0"))

        val before = RetrofitClient.getBaseUrl()
        assertFalse(RetrofitClient.setBaseUrl("not a valid url"))
        assertEquals(before, RetrofitClient.getBaseUrl())

        assertTrue(RetrofitClient.setBaseUrl("http://127.0.0.1:8801"))
        assertEquals("http://127.0.0.1:8801/", RetrofitClient.getBaseUrl())
    }
}
