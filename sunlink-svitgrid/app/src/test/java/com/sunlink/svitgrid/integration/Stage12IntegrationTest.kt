package com.sunlink.svitgrid.integration

import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.network.ApiService
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
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Этап 12 — воспроизводимые end-to-end сценарии интеграции.
 *
 * Использует MockWebServer с РЕАЛЬНЫМИ JSON-ответами изолированного DEMO backend
 * (app/src/test/resources/demo/). Никаких запросов к production URL.
 *
 * Цепочка: HTTP -> Retrofit -> DataRepository -> MainViewModel -> UI state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Stage12IntegrationTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var repository: DataRepository
    private lateinit var vm: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server = MockWebServer().also { it.start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/").toString())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        repository = DataRepository(api)
        vm = MainViewModel(repository)
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

    private fun jsonResponse(name: String, code: Int = 200) =
        MockResponse().setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(fixture(name))

    /** Repository уходит в Dispatchers.IO — ждём завершения корутины. */
    private suspend fun waitFor(timeoutMs: Long = 3000, predicate: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!predicate() && System.currentTimeMillis() - start < timeoutMs) {
            delay(20)
        }
    }

    // --- Сценарий 5-8: получение telemetry и попадание на Dashboard ---

    @Test
    fun `demo telemetry reaches ViewModel success state`() {
        runBlocking {
            server.enqueue(jsonResponse("live_raw.json"))
            vm.loadData()
            waitFor { vm.uiState.value !is InverterUiState.Loading }

            val state = vm.uiState.value
            assertTrue(state is InverterUiState.Success)
            state as InverterUiState.Success
            assertEquals("demo_simulator", state.data.source)
            assertEquals(85.0f, state.data.batterySoc!!, 0.001f)
            assertEquals(211.0f, state.data.pv1Power!!, 0.001f)
            assertEquals(449.0f, state.data.pSun!!, 0.001f)
            assertEquals("deye_sg02lp1_am3", state.status.inverterId)
            assertEquals(9, state.status.availableRegisters)
            // Отсутствующие параметры остаются null — UI покажет «—»
            assertNull(state.data.pGrid)
            assertNull(state.data.pLoad)
        }
    }

    @Test
    fun `repository does not fabricate empty success on http error`() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500)
                .setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))
            vm.refresh()
            waitFor { !vm.refreshing.value }

            val state = vm.uiState.value
            assertTrue("state was $state", state is InverterUiState.Error)
            state as InverterUiState.Error
            assertNull(state.data) // нет последних данных — не подменяем нулями
        }
    }

    // --- Сценарий 9: история из реальных измерений ---

    @Test
    fun `history from real backend fixture reaches UI state`() {
        runBlocking {
            server.enqueue(jsonResponse("history_battery_soc.json"))
            vm.loadHistory("battery_soc", "2026-11-18T00:00:00Z", "2026-11-19T00:00:00Z")
            waitFor { vm.historyState.value !is HistoryUiState.Loading }

            val state = vm.historyState.value
            assertTrue(state is HistoryUiState.Success)
            state as HistoryUiState.Success
            assertEquals("battery_soc", state.parameter)
            assertEquals("%", state.unit)
            assertTrue(state.points.size > 1)
            assertTrue(state.points.all { it.value != null })
        }
    }

    @Test
    fun `empty history from backend yields Empty state`() {
        runBlocking {
            server.enqueue(jsonResponse("history_empty.json"))
            vm.loadHistory("does_not_exist", "2026-11-18T00:00:00Z", "2026-11-19T00:00:00Z")
            waitFor { vm.historyState.value !is HistoryUiState.Loading }
            assertTrue(vm.historyState.value is HistoryUiState.Empty)
        }
    }

    // --- Сценарий 10-11: offline и восстановление соединения ---

    @Test
    fun `backend offline preserves last good data then recovers`() {
        runBlocking {
            // 1. backend доступен
            server.enqueue(jsonResponse("live_raw.json"))
            vm.loadData()
            waitFor { vm.uiState.value !is InverterUiState.Loading }
            val online = vm.uiState.value as InverterUiState.Success
            assertEquals(85.0f, online.data.batterySoc!!, 0.001f)

            // 2. backend недоступен -> ошибка, но последние данные сохранены
            server.enqueue(MockResponse().setResponseCode(500).setBody("offline"))
            vm.refresh()
            waitFor { !vm.refreshing.value }
            val offline = vm.uiState.value
            assertTrue(offline is InverterUiState.Error)
            offline as InverterUiState.Error
            assertNotNull(offline.data) // НЕ нули, а последние реальные значения
            assertEquals(85.0f, offline.data!!.batterySoc!!, 0.001f)
            assertTrue(offline.stale)

            // 3. backend снова доступен -> восстановление
            server.enqueue(jsonResponse("live_raw.json"))
            vm.refresh()
            waitFor { !vm.refreshing.value }
            val recovered = vm.uiState.value
            assertTrue(recovered is InverterUiState.Success)
            recovered as InverterUiState.Success
            assertEquals("demo_simulator", recovered.data.source)
        }
    }

    // --- Получение списка инверторов ---

    @Test
    fun `inverter list loads from backend`() {
        runBlocking {
            server.enqueue(jsonResponse("inverters.json"))
            vm.loadInverters()
            waitFor(timeoutMs = 1000) { vm.inverters.value != null }
            val dto = vm.inverters.value
            assertNotNull(dto)
            assertEquals(1, dto!!.count)
            assertEquals("deye_sg02lp1_am3", dto.inverters[0].inverter_id)
        }
    }

    @Test
    fun `history request uses correct backend parameter names`() {
        runBlocking {
            server.enqueue(jsonResponse("history_battery_soc.json"))
            vm.loadHistory("battery_soc", "2026-11-18T00:00:00Z", "2026-11-19T00:00:00Z")
            waitFor { vm.historyState.value !is HistoryUiState.Loading }
            val recorded = server.takeRequest()
            val path = recorded.path.orEmpty()
            assertTrue(path.contains("parameter=battery_soc"))
            assertTrue(path.startsWith("/api/measurements/history"))
        }
    }
}
