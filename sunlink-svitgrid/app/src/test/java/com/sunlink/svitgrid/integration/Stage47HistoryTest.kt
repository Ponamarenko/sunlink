package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
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
class Stage47HistoryTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var server: MockWebServer

    @Before fun setUp() { Dispatchers.setMain(dispatcher); server = MockWebServer(); server.start() }
    @After fun tearDown() { server.shutdown(); Dispatchers.resetMain() }

    @Test
    fun `history series parses all real parameters timestamps and gaps`() {
        val json = "{\"parameter\":\"pv_total_power\",\"period\":\"7d\",\"unit\":\"W\",\"count\":2," +
            "\"available_points\":1,\"missing_points\":1," +
            "\"points\":[{\"timestamp\":\"2026-11-20T10:00:00Z\",\"value\":7380.0,\"gap\":false}," +
            "{\"timestamp\":\"2026-11-20T11:00:00Z\",\"value\":null,\"gap\":true}]}"
        val dto = Gson().fromJson(json, HistorySeriesDto::class.java)
        assertEquals("pv_total_power", dto.parameter)
        assertEquals("7d", dto.period)
        assertEquals(7380.0, dto.points[0].value!!, 0.0)
        assertNull(dto.points[1].value)
        assertTrue(dto.points[1].gap)
        assertTrue(dto.points[0].timestamp.contains("T"))
    }

    @Test
    fun `history screen includes all real parameters including pv total`() {
        val src = File("src/main/java/com/sunlink/svitgrid/ui/history/HistoryScreen.kt").readText()
        listOf("pv_total_power", "battery_voltage", "grid_frequency", "load_power",
            "daily_grid_import", "daily_battery_charge").forEach { assertTrue(it, src.contains(it)) }
        assertTrue(src.contains("vm.loadSeries("))
        assertFalse(src.contains("random"))
    }

    @Test
    fun `repository loads series from mock backend`() {
        val body = "{\"inverter_id\":\"deye_sg02lp1_am3\",\"parameter\":\"battery_soc\",\"period\":\"24h\"," +
            "\"count\":1,\"available_points\":1,\"missing_points\":0," +
            "\"points\":[{\"timestamp\":\"t\",\"value\":94.0,\"gap\":false}]}"
        server.enqueue(MockResponse().setBody(body).setHeader("Content-Type", "application/json"))
        com.sunlink.svitgrid.network.RetrofitClient.setBaseUrl(server.url("/").toString())
        val repo = DataRepository(com.sunlink.svitgrid.network.RetrofitClient.getApiService())
        val result = kotlinx.coroutines.runBlocking { repo.loadHistorySeries("battery_soc", "24h") }
        assertTrue("result=$result", result is Result.Success)
        assertEquals(94.0, (result as Result.Success).data.points[0].value!!, 0.0)
    }

    @Test
    fun `localization parity and no russian leftovers`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun keys(s: String) = Regex("""<string name="([^"]+)"""").findAll(s).map { it.groupValues[1] }.toSet()
        assertEquals(keys(uk), keys(en))
        assertTrue(keys(uk).contains("hist_pv_total"))
        val sources = File("src/main/java/com/sunlink/svitgrid")
        val bad = sources.walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            listOf("ОТСУТСТВУЄ", "Батерея").filter { f.readText().contains(it) }.map { "${f.name}:$it" }
        }.toList()
        assertTrue(bad.toString(), bad.isEmpty())
    }

    @Test
    fun `viewmodel series error state starts clean`() {
        val vm = MainViewModel(DataRepository(null))
        assertNull(vm.series.value)
        assertNull(vm.seriesError.value)
        assertFalse(vm.seriesLoading.value)
    }
}
