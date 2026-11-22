package com.sunlink.svitgrid.integration

import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
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
class Stage38QualityTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `uk and en resources have identical keys and no empty values`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun entries(s: String) = Regex("""<string name="([^"]+)">(.*?)</string>""")
            .findAll(s).associate { it.groupValues[1] to it.groupValues[2] }
        val ukMap = entries(uk)
        val enMap = entries(en)
        assertEquals(ukMap.keys, enMap.keys)
        assertTrue(ukMap.size > 100)
        assertTrue(ukMap.values.none { it.isBlank() })
        assertTrue(enMap.values.none { it.isBlank() })
    }

    @Test
    fun `no Russian leftovers in user facing sources`() {
        val root = File("src/main/java/com/sunlink/svitgrid")
        val forbidden = listOf("ОТСУТСТВУЄ", "Батерея", "Бутова навантаження")
        val found = root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            forbidden.filter { file.readText().contains(it) }.map { "${file.name}:$it" }
        }.toList()
        assertTrue("leftovers: $found", found.isEmpty())
    }

    @Test
    fun `full telemetry error is exposed without fabricating data`() {
        class FailingRepo : DataRepository(null) {
            override suspend fun loadFullTelemetry(): Result<TelemetryFullDto> =
                Result.Error(IllegalStateException("backend offline"))
        }
        val vm = MainViewModel(FailingRepo())
        vm.loadFullTelemetry()
        assertNull(vm.fullTelemetry.value)
        assertEquals("backend offline", vm.fullTelemetryError.value)
    }

    @Test
    fun `compose ui test sources are present and reference all screens`() {
        val test = File("src/androidTest/java/com/sunlink/svitgrid/ui/Stage38ComposeTest.kt")
        assertTrue(test.exists())
        val text = test.readText()
        listOf("DashboardScreen", "BatteryDetailScreen", "PvDetailScreen",
            "GridDetailScreen", "LoadDetailScreen", "EnergyFlowScreen", "SvitGridApp")
            .forEach { assertTrue("missing UI test for $it", text.contains(it)) }
    }
}
