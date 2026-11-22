package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class Stage46HistoryTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val seriesJson = """
        {"inverter_id":"deye_sg02lp1_am3","parameter":"battery_soc","period":"24h",
         "interval_seconds":300,"unit":"%","count":3,"available_points":2,"missing_points":1,
         "points":[{"timestamp":"t1","value":94.0,"gap":false,"samples":2},
                   {"timestamp":"t2","value":null,"gap":true},
                   {"timestamp":"t3","value":95.0,"gap":false,"samples":1}]}
    """.trimIndent()

    @Test
    fun `series dto keeps gaps as null`() {
        val dto = Gson().fromJson(seriesJson, HistorySeriesDto::class.java)
        assertEquals("24h", dto.period)
        assertEquals(3, dto.count)
        assertNull(dto.points[1].value)
        assertTrue(dto.points[1].gap)
        assertEquals(94.0, dto.points[0].value!!, 0.0)
    }

    @Test
    fun `viewmodel series starts empty`() {
        val vm = MainViewModel(com.sunlink.svitgrid.data.repository.DataRepository(null))
        assertNull(vm.series.value)
        assertFalse(vm.seriesLoading.value)
    }

    @Test
    fun `history screen uses real series endpoint and three periods`() {
        val src = File("src/main/java/com/sunlink/svitgrid/ui/history/HistoryScreen.kt").readText()
        assertTrue(src.contains("vm.loadSeries("))
        listOf("\"24h\"", "\"7d\"", "\"30d\"").forEach { assertTrue(src.contains(it)) }
        assertTrue(src.contains("TelemetryChart("))
        assertFalse(src.contains("vm.loadHistory("))
    }

    @Test
    fun `localization parity with history strings`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun keys(s: String) = Regex("""<string name="([^"]+)"""").findAll(s).map { it.groupValues[1] }.toSet()
        assertEquals(keys(uk), keys(en))
        assertTrue(keys(uk).contains("history_points"))
    }
}
